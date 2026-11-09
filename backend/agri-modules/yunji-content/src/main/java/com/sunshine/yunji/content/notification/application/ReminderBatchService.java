package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.article.application.ArticleAccess;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Durable planning/claim/settlement. External work is performed outside these transactions. */
@Service
public class ReminderBatchService {
    private final Db db; private final ReminderTrackingService tracking; private final Actors actors; private final boolean local;
    public ReminderBatchService(Db db,ReminderTrackingService tracking,Actors actors,@Value("${yunji.local-enabled:false}") boolean local) {this.db=db;this.tracking=tracking;this.actors=actors;this.local=local;}
    private boolean yes(Object value){return Db.number(value)==1;}
    private boolean visible(Map<String,Object> c){return db.count("SELECT COUNT(*) FROM article a WHERE a.id=? AND a.current_version_id=? AND "+ArticleAccess.VISIBLE,c.get("articleId"),c.get("versionId"))==1;}
    private boolean future(Map<String,Object> c){return db.count("SELECT COUNT(*) FROM article a WHERE a.id=? AND a.current_version_id=? AND "+ArticleAccess.CURRENT+" AND a.effective_from>UTC_TIMESTAMP()",c.get("articleId"),c.get("versionId"))==1;}
    private boolean read(Map<String,Object> c){return db.count("SELECT COUNT(*) FROM user_exposure_total WHERE user_id=? AND article_id=? AND visible_count>=9",c.get("userId"),c.get("articleId"))>0||db.count("SELECT COUNT(*) FROM user_read_state r JOIN article_version v ON v.id=r.read_version_id JOIN article_version cv ON cv.id=? WHERE r.user_id=? AND r.article_id=? AND v.version_no>=cv.version_no",c.get("versionId"),c.get("userId"),c.get("articleId"))>0;}
    private List<Map<String,Object>> matches(Map<String,Object> c){return db.rows("SELECT t.* FROM reminder_tracking t JOIN reminder_match m ON m.tracking_id=t.id WHERE m.change_id=? AND t.enabled=1 AND t.lock_version=m.tracking_version",c.get("id"));}
    @Transactional public int plan() {
        var control=tracking.lock();int planned=0;
        // A crashed sender's outcome is unknown. Never reclaim it as a fresh send.
        db.update("UPDATE reminder_batch SET state='UNKNOWN',error_code='LEASE_EXPIRED' WHERE state='SENDING' AND lease_until<UTC_TIMESTAMP()");
        String usersSql="SELECT DISTINCT c.user_id FROM reminder_change c JOIN app_user u ON u.id=c.user_id WHERE c.state='PENDING' AND u.status='ACTIVE' AND u.deleted_at IS NULL AND c.user_id>? ORDER BY c.user_id LIMIT 100";
        var users=db.rows(usersSql,control.get("planAfterUser"));
        if(users.isEmpty())users=db.rows(usersSql,0);
        if(!users.isEmpty())db.update("UPDATE reminder_control SET plan_after_user=? WHERE id=1",users.get(users.size()-1).get("userId"));
        for(var user:users) {
            long uid=Db.number(user.get("userId"));var settings=tracking.settings(uid);if(!Boolean.TRUE.equals(settings.get("enabled")))continue;
            ZonedDateTime now=ZonedDateTime.now(ZoneId.of(control.get("timezone").toString()));
            var pref=db.one("SELECT * FROM reminder_preference WHERE user_id=?",uid);
            ZonedDateTime taskTime=now.toLocalDate().atTime(LocalTime.parse(pref.get("slot").toString())).atZone(now.getZone()).minusMinutes(5);
            boolean due=yes(control.get("dailyEnabled"))&&!now.isBefore(taskTime)&&!now.toLocalDate().toString().equals(Objects.toString(pref.get("lastBatchDate"),""));
            var daily=new ArrayList<Map<String,Object>>();
            for(var c:db.rows("SELECT c.* FROM reminder_change c WHERE c.user_id=? AND c.state='PENDING' AND NOT EXISTS(SELECT 1 FROM article a WHERE a.id=c.article_id AND a.current_version_id=c.version_id AND "+ArticleAccess.CURRENT+" AND a.effective_from>UTC_TIMESTAMP()) ORDER BY c.created_at,c.id LIMIT 200",uid)) {
                if(future(c))continue;
                String suppress=!visible(c)?"SUPERSEDED":read(c)?"READ":matches(c).isEmpty()?"CANCELLED":null;
                if(suppress!=null){db.update("UPDATE reminder_change SET state=? WHERE id=?",suppress,c.get("id"));continue;}
                boolean instant=matches(c).stream().anyMatch(m->m.get("mode").equals("INSTANT"));
                if(instant){if(yes(control.get("instantEnabled"))&&db.count("SELECT COUNT(*) FROM reminder_match current_match JOIN reminder_match other_match ON other_match.tracking_id=current_match.tracking_id JOIN reminder_batch_item i ON i.change_id=other_match.change_id JOIN reminder_batch b ON b.id=i.batch_id WHERE current_match.change_id=? AND b.channel='WECHAT_SUBSCRIBE' AND b.state IN('PENDING','WAITING_AUTH','SENDING','UNKNOWN')",c.get("id"))==0){batch(uid,"WECHAT_SUBSCRIBE",List.of(c));planned++;}}
                else if(due)daily.add(c);
            }
            if(!daily.isEmpty()) {batch(uid,"WECom",daily);planned++;db.update("UPDATE reminder_preference SET last_batch_date=? WHERE user_id=?",now.toLocalDate(),uid);}
        }
        return planned;
    }
    private void batch(long user,String channel,List<Map<String,Object>> changes) {
        long id=db.id();db.update("INSERT INTO reminder_batch(id,user_id,channel,snapshot) VALUES(?,?,?,?)",id,user,channel,snapshot(changes));
        for(var c:changes){db.update("INSERT INTO reminder_batch_item(batch_id,change_id) VALUES(?,?)",id,c.get("id"));db.update("UPDATE reminder_change SET state='BATCHED' WHERE id=?",c.get("id"));}
    }
    private String snapshot(List<Map<String,Object>> changes) {
        var items=new ArrayList<Map<String,Object>>();
        for(var c:changes)items.add(db.one("SELECT id version_id,article_id,title,summary FROM article_version WHERE id=?",c.get("versionId")));
        return db.json(Map.of("items",items));
    }
    @Transactional public Map<String,Object> claim() {
        var control=tracking.lock();if(!local)return Map.of();
        var rows=db.rows("SELECT * FROM reminder_batch WHERE state IN('PENDING','WAITING_AUTH') AND next_attempt_at<=UTC_TIMESTAMP() ORDER BY next_attempt_at,created_at,id LIMIT 100");
        for(var b:rows) {
            long id=Db.number(b.get("id")),user=Db.number(b.get("userId"));boolean wecom=b.get("channel").equals("WECom");
            if(!yes(control.get(wecom?"dailyEnabled":"instantEnabled"))){defer(id);continue;}
            boolean active=db.count("SELECT COUNT(*) FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user)==1;
            boolean enabled=db.count("SELECT COUNT(*) FROM reminder_preference WHERE user_id=? AND enabled=1",user)==1;
            if(active&&!enabled){defer(id);continue;}
            var eligible=new ArrayList<Map<String,Object>>();
            for(var c:db.rows("SELECT c.* FROM reminder_change c JOIN reminder_batch_item i ON i.change_id=c.id WHERE i.batch_id=? AND c.state='BATCHED'",id)) {
                if(active&&visible(c)&&!read(c)&&!matches(c).isEmpty())eligible.add(c);
                else db.update("UPDATE reminder_change SET state=? WHERE id=?",read(c)?"READ":"CANCELLED",c.get("id"));
            }
            if(eligible.isEmpty()){settleQuota(b,false);db.update("UPDATE reminder_batch SET state='CANCELLED',completed_at=UTC_TIMESTAMP() WHERE id=?",id);continue;}
            var perms=db.rows("SELECT * FROM reminder_permission WHERE user_id=?",user);
            if(perms.isEmpty()||(wecom?!yes(perms.get(0).get("wecomBound")):Db.number(perms.get(0).get("available"))==0&&!yes(b.get("quotaReserved")))) {
                db.update("UPDATE reminder_batch SET state='WAITING_AUTH',next_attempt_at=UTC_TIMESTAMP()+INTERVAL 30 SECOND WHERE id=?",id);continue;
            }
            if(!wecom&&!yes(b.get("quotaReserved"))) {db.update("UPDATE reminder_permission SET available=available-1,reserved=reserved+1 WHERE user_id=?",user);db.update("UPDATE reminder_batch SET quota_reserved=1 WHERE id=?",id);}
            String lease=db.token();db.update("UPDATE reminder_batch SET state='SENDING',snapshot=?,attempts=attempts+1,lease_token=?,lease_until=UTC_TIMESTAMP()+INTERVAL 2 MINUTE WHERE id=?",snapshot(eligible),lease,id);
            var result=db.one("SELECT * FROM reminder_batch WHERE id=?",id);result.put("simulationOutcome",perms.get(0).get("simulationOutcome"));return result;
        }
        return Map.of();
    }
    private void defer(long id){db.update("UPDATE reminder_batch SET next_attempt_at=UTC_TIMESTAMP()+INTERVAL 30 SECOND WHERE id=?",id);}
    private void settleQuota(Map<String,Object> b,boolean success) {
        if(!yes(b.get("quotaReserved")))return;
        db.update("UPDATE reminder_permission SET reserved=reserved-1,"+(success?"consumed=consumed+1":"available=available+1")+" WHERE user_id=? AND reserved>0",b.get("userId"));
        db.update("UPDATE reminder_batch SET quota_reserved=0 WHERE id=?",b.get("id"));
    }
    @Transactional public void finish(long id,String lease,String outcome,String providerId,String code) {
        tracking.lock();var b=db.one("SELECT * FROM reminder_batch WHERE id=?",id);
        if(!b.get("state").equals("SENDING")||!Objects.equals(b.get("leaseToken"),lease))return;
        String state=switch(outcome){case "SUCCESS"->b.get("channel").equals("WECom")?"AWAITING_STAFF":"SENT";case "FAILURE"->"FAILED";case "RETRY"->Db.number(b.get("attempts"))>=5?"FAILED":"PENDING";default->"UNKNOWN";};
        db.update("INSERT INTO reminder_attempt(id,batch_id,attempt_no,outcome,evidence) VALUES(?,?,?,?,?)",db.id(),id,b.get("attempts"),state,code);
        db.update("UPDATE reminder_batch SET state=?,provider_task_id=?,error_code=?,lease_until=NULL,next_attempt_at=UTC_TIMESTAMP()+INTERVAL 1 MINUTE WHERE id=?",state,providerId,code,id);
        terminal(b,state);
    }
    private void terminal(Map<String,Object> b,String state) {
        if(!Set.of("SENT","ACCEPTED","FAILED").contains(state))return;
        boolean success=!state.equals("FAILED");settleQuota(b,success);
        db.update("UPDATE reminder_batch SET completed_at=UTC_TIMESTAMP() WHERE id=?",b.get("id"));
        db.update("UPDATE reminder_change c JOIN reminder_batch_item i ON i.change_id=c.id SET c.state=? WHERE i.batch_id=? AND c.state='BATCHED'",success?"SENT":"CANCELLED",b.get("id"));
        if(success&&b.get("channel").equals("WECHAT_SUBSCRIBE")) {
            db.update("UPDATE reminder_tracking t JOIN reminder_match m ON m.tracking_id=t.id JOIN reminder_batch_item i ON i.change_id=m.change_id SET t.mode='DAILY',t.lock_version=t.lock_version+1 WHERE i.batch_id=? AND t.mode='INSTANT' AND t.lock_version=m.tracking_version",b.get("id"));
            db.update("UPDATE reminder_match m JOIN reminder_change c ON c.id=m.change_id JOIN reminder_tracking t ON t.id=m.tracking_id SET m.tracking_version=t.lock_version WHERE c.user_id=? AND c.state='PENDING' AND t.mode='DAILY' AND t.enabled=1",b.get("userId"));
        }
    }
    @Transactional public Map<String,Object> reconcile(long id,String state,String evidence) {
        actors.staff().require("yunji:notification:manage");tracking.lock();
        BusinessException.require(Set.of("SENT","FAILED","PARTIAL").contains(state),422,"无效的核实结果");
        String proof=Db.text(evidence,1000);BusinessException.require(proof.length()>=10,422,"请提供至少十个字符的核实依据");
        var b=db.one("SELECT * FROM reminder_batch WHERE id=?",id);
        BusinessException.require(Set.of("UNKNOWN","AWAITING_STAFF","PARTIAL").contains(b.get("state")),409,"此任务不需要核实或已结束");
        BusinessException.require(!state.equals("PARTIAL")||b.get("channel").equals("WECom"),422,"仅企微任务支持部分执行");
        db.update("UPDATE reminder_batch SET state=? WHERE id=?",state,id);terminal(b,state);
        db.audit(actors.staff().id(),"STAFF","REMINDER_RECONCILE","BATCH",id,Map.of("state",state,"evidence",proof,"localSimulation",local));
        return db.one("SELECT id,state,provider_task_id,completed_at FROM reminder_batch WHERE id=?",id);
    }
    @Transactional public void retry(long id,String evidence) {
        actors.staff().require("yunji:notification:manage");tracking.lock();String proof=Db.text(evidence,1000);
        BusinessException.require(proof.length()>=10,422,"补提需要至少十个字符的依据");
        db.one("SELECT id FROM reminder_batch WHERE id=? AND state='FAILED'",id);
        db.update("UPDATE reminder_batch SET state='PENDING',provider_task_id=NULL,error_code=NULL,completed_at=NULL,next_attempt_at=UTC_TIMESTAMP() WHERE id=?",id);
        db.update("UPDATE reminder_change c JOIN reminder_batch_item i ON i.change_id=c.id SET c.state='BATCHED' WHERE i.batch_id=? AND c.state='CANCELLED'",id);
        db.audit(actors.staff().id(),"STAFF","REMINDER_RETRY","BATCH",id,Map.of("evidence",proof));
    }
    public Map<String,Object> batches(Long user,int page,int size,String state,String channel) {
        if(user==null)actors.staff().require("yunji:notification:manage");
        var args=new ArrayList<Object>();String where=" WHERE 1=1";
        if(user!=null){where+=" AND user_id=?";args.add(user);}
        if(state!=null&&!state.isBlank()){where+=" AND state=?";args.add(state);}
        if(channel!=null&&!channel.isBlank()){BusinessException.require(Set.of("WECom","WECHAT_SUBSCRIBE").contains(channel),422,"提醒通道无效");where+=" AND channel=?";args.add(channel);}
        long total=db.count("SELECT COUNT(*) FROM reminder_batch"+where,args.toArray());args.add(size);args.add((page-1)*size);
        return Map.of("items",db.rows("SELECT id,user_id,channel,state,attempts,provider_task_id,error_code,created_at,completed_at,(SELECT COUNT(*) FROM reminder_batch_item i WHERE i.batch_id=b.id) item_count FROM reminder_batch b"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",args.toArray()),"total",total,"page",page,"size",size,"localSimulation",local);
    }
    public Map<String,Object> statistics() {
        actors.staff().require("yunji:notification:manage");
        var result=new LinkedHashMap<String,Object>();
        result.put("states",db.rows("SELECT channel,state,COUNT(*) total FROM reminder_batch GROUP BY channel,state"));
        result.put("pendingChanges",db.count("SELECT COUNT(*) FROM reminder_change WHERE state='PENDING'"));
        result.put("activeRules",db.count("SELECT COUNT(*) FROM reminder_tracking WHERE enabled=1"));
        result.put("trackedUsers",db.count("SELECT COUNT(DISTINCT user_id) FROM reminder_tracking"));
        String active=" FROM reminder_tracking t JOIN app_user u ON u.id=t.user_id JOIN reminder_preference p ON p.user_id=t.user_id WHERE t.enabled=1 AND p.enabled=1 AND u.status='ACTIVE' AND u.deleted_at IS NULL";
        result.put("enabledUsers",db.count("SELECT COUNT(DISTINCT t.user_id)"+active));
        result.put("dailyUsers",db.count("SELECT COUNT(DISTINCT t.user_id)"+active+" AND t.mode='DAILY'"));
        result.put("instantUsers",db.count("SELECT COUNT(DISTINCT t.user_id)"+active+" AND t.mode='INSTANT'"));
        result.put("taskCreated",db.count("SELECT COUNT(*) FROM reminder_batch WHERE provider_task_id IS NOT NULL"));
        result.put("confirmed",db.count("SELECT COUNT(*) FROM reminder_batch WHERE state='SENT'"));
        result.put("trend",db.rows("SELECT DATE_FORMAT(created_at+INTERVAL 8 HOUR,'%Y-%m-%d') day,channel,COUNT(*) created,SUM(state='SENT') confirmed FROM reminder_batch WHERE created_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY GROUP BY day,channel ORDER BY day,channel"));
        result.put("failures",db.rows("SELECT channel,COALESCE(error_code,'UNSPECIFIED') error_code,COUNT(*) total FROM reminder_batch WHERE state IN('FAILED','UNKNOWN') GROUP BY channel,error_code ORDER BY total DESC,channel LIMIT 10"));
        result.put("sources",db.rows("SELECT t.target_type,COUNT(DISTINCT m.change_id) total FROM reminder_match m JOIN reminder_tracking t ON t.id=m.tracking_id GROUP BY t.target_type ORDER BY t.target_type"));
        result.put("localSimulation",local);return result;
    }
    public Map<String,Object> rules(int page,int size) {actors.staff().require("yunji:notification:manage");return Map.of("items",db.rows("SELECT t.*,CASE WHEN target_type='ARTICLE' THEN (SELECT title FROM article WHERE id=t.target_id) ELSE (SELECT display_name FROM keyword WHERE id=t.target_id) END title FROM reminder_tracking t ORDER BY id DESC LIMIT ? OFFSET ?",size,(page-1)*size),"total",db.count("SELECT COUNT(*) FROM reminder_tracking"));}
}
