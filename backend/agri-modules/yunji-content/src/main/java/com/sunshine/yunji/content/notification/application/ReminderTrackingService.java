package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.article.application.ArticleAccess;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Change-driven v2 domain. The control row is the lock-order root for reminder mutations. */
@Service
public class ReminderTrackingService {
    private final Db db; private final Actors actors; private final ArticleAccess articles;
    private final boolean local; private final String secret;
    public ReminderTrackingService(Db db, Actors actors, ArticleAccess articles,
            @Value("${yunji.local-enabled:false}") boolean local, @Value("${yunji.local-secret:}") String secret) {
        this.db=db; this.actors=actors; this.articles=articles; this.local=local; this.secret=secret;
    }
    public record Tracking(@NotBlank String targetType,@Positive long targetId,@NotBlank String mode,boolean enabled,@Min(0) int expectedVersion) {}
    public record Preference(boolean enabled,@NotBlank String slot,@Min(0) int expectedVersion) {}
    public record Control(boolean dailyEnabled,boolean instantEnabled,@NotBlank String timezone,
                          @Size(min=3,max=3) List<String> slots,@Size(max=128) String dailyTemplate,
                          @Size(max=128) String instantTemplate,@Min(0) int expectedVersion) {}
    public record Authorization(@NotBlank @Size(max=100) String receiptKey,boolean wecomBound,
                                @Min(0) @Max(10) int amount,@NotBlank String outcome) {}
    public Map<String,Object> lock() {return db.one("SELECT * FROM reminder_control WHERE id=1 FOR UPDATE");}
    public void active(long user) {db.one("SELECT id FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user);}
    private LocalTime time(String value) {try {return LocalTime.parse(value);}catch(Exception e){throw new BusinessException(422,"时间格式应为 HH:mm");}}
    public List<String> slots(Map<String,Object> control) {
        var values=db.object("{\"slots\":"+control.get("slots")+"}").get("slots");
        return ((List<?>)values).stream().map(Object::toString).toList();
    }
    public Map<String,Object> control() {actors.staff().require("yunji:notification:manage"); var c=db.one("SELECT * FROM reminder_control WHERE id=1");c.put("slots",slots(c));return c;}
    @Transactional public Map<String,Object> configure(Control b) {
        actors.staff().require("yunji:notification:manage"); var old=lock();
        BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion(),409,"设置已变化，请刷新");
        try{ZoneId.of(b.timezone());}catch(Exception e){throw new BusinessException(422,"时区无效");}
        BusinessException.require(b.slots()!=null&&b.slots().size()==3,422,"需要三个提醒时段");
        var times=b.slots().stream().map(this::time).map(t->t.withSecond(0).withNano(0).toString()).sorted().toList();
        BusinessException.require(new HashSet<>(times).size()==3,422,"提醒时段不能重复");
        var before=slots(old);
        // Keep each user's slot position when operators change the schedule.
        for(var pref:db.rows("SELECT user_id,slot FROM reminder_preference")) {
            int index=before.indexOf(time(pref.get("slot").toString()).toString());
            db.update("UPDATE reminder_preference SET slot=?,lock_version=lock_version+1 WHERE user_id=?",times.get(Math.max(0,index)),pref.get("userId"));
        }
        db.update("UPDATE reminder_control SET daily_enabled=?,instant_enabled=?,timezone=?,slots=?,daily_template=?,instant_template=?,lock_version=lock_version+1 WHERE id=1",
                b.dailyEnabled(),b.instantEnabled(),b.timezone(),db.json(times),Objects.toString(b.dailyTemplate(),""),Objects.toString(b.instantTemplate(),""));
        db.audit(actors.staff().id(),"STAFF","REMINDER_CONTROL","REMINDER",1,b);return control();
    }
    public Map<String,Object> settings(long user) {
        var c=db.one("SELECT * FROM reminder_control WHERE id=1");var p=db.rows("SELECT * FROM reminder_preference WHERE user_id=?",user);
        var result=new LinkedHashMap<String,Object>();result.put("slots",slots(c));result.put("timezone",c.get("timezone"));
        result.put("enabled",p.isEmpty()||Db.number(p.get(0).get("enabled"))==1);
        result.put("slot",p.isEmpty()?slots(c).get(0):time(p.get(0).get("slot").toString()).toString());
        result.put("lockVersion",p.isEmpty()?0:p.get(0).get("lockVersion"));result.put("localSimulation",local);
        result.put("realProviderReady",false);result.put("instantTemplate",c.get("instantTemplate"));
        var perms=db.rows("SELECT wecom_bound,available,reserved,consumed,wecom_contact_state FROM reminder_permission WHERE user_id=?",user);
        result.put("wecomState",perms.isEmpty()?"NONE":Objects.toString(perms.get(0).get("wecomContactState"),"NONE"));
        result.put("permissions",perms);return result;
    }
    @Transactional public Map<String,Object> preference(long user,Preference b) {
        var c=lock();active(user);String slot=time(b.slot()).toString();BusinessException.require(slots(c).contains(slot),422,"请选择当前提供的时段");
        var rows=db.rows("SELECT lock_version FROM reminder_preference WHERE user_id=?",user);
        BusinessException.require((rows.isEmpty()?0:Db.number(rows.get(0).get("lockVersion")))==b.expectedVersion(),409,"设置已变化，请刷新");
        db.update("INSERT INTO reminder_preference(user_id,enabled,slot,lock_version) VALUES(?,?,?,1) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),slot=VALUES(slot),lock_version=lock_version+1",user,b.enabled(),slot);
        db.audit(user,"CUSTOMER","REMINDER_PREFERENCE","USER",user,b);return settings(user);
    }
    @Transactional public Map<String,Object> requestWecomContact(long user) {
        lock();active(user);
        var p=db.rows("SELECT wecom_bound FROM reminder_permission WHERE user_id=?",user);
        boolean bound=!p.isEmpty()&&Db.number(p.get(0).get("wecomBound"))==1;
        if(!bound)db.update("INSERT INTO reminder_permission(user_id,wecom_contact_state) VALUES(?,?) ON DUPLICATE KEY UPDATE wecom_contact_state=VALUES(wecom_contact_state)",user,"CONTACTED");
        db.audit(user,"CUSTOMER","REMINDER_WECOM_REQUEST","USER",user,Map.of());return settings(user);
    }
    @Transactional public Map<String,Object> wecomLink(long user) {
        lock();active(user);
        var p=db.rows("SELECT wecom_bound,wecom_contact_state FROM reminder_permission WHERE user_id=?",user);
        boolean bound=!p.isEmpty()&&Db.number(p.get(0).get("wecomBound"))==1;
        String state=p.isEmpty()?"NONE":Objects.toString(p.get(0).get("wecomContactState"),"NONE");
        if(!bound)BusinessException.require("CONTACTED".equals(state),409,"你没有添加企业微信");
        db.update("INSERT INTO reminder_permission(user_id,wecom_bound,wecom_contact_state) VALUES(?,1,'BOUND') ON DUPLICATE KEY UPDATE wecom_bound=1,wecom_contact_state='BOUND'",user);
        db.audit(user,"CUSTOMER","REMINDER_WECOM_LINK","USER",user,Map.of());return settings(user);
    }
    @Transactional public Map<String,Object> save(long user,Long id,Tracking b) {
        lock();active(user);
        BusinessException.require(Set.of("ARTICLE","KEYWORD").contains(b.targetType())&&Set.of("DAILY","INSTANT").contains(b.mode()),422,"跟踪对象或模式无效");
        if(id==null||b.enabled()) {
            if(b.targetType().equals("ARTICLE"))articles.visible(b.targetId());
            else db.one("SELECT id FROM keyword WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",b.targetId());
        }
        var existing=db.rows("SELECT * FROM reminder_tracking WHERE user_id=? AND target_type=? AND target_id=? FOR UPDATE",user,b.targetType(),b.targetId());
        BusinessException.require(id!=null||existing.isEmpty(),409,"此对象已有跟踪，请编辑现有规则");
        long key=id==null?db.id():id;
        if(id!=null){var old=db.one("SELECT * FROM reminder_tracking WHERE id=? AND user_id=? FOR UPDATE",id,user);
            BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion(),409,"跟踪已变化，请刷新");
            BusinessException.require(old.get("targetType").equals(b.targetType())&&Db.number(old.get("targetId"))==b.targetId(),422,"编辑不能更换跟踪对象");}
        if(b.enabled()&&b.mode().equals("DAILY"))BusinessException.require(db.count("SELECT COUNT(*) FROM reminder_permission WHERE user_id=? AND wecom_bound=1",user)>0,409,"请先添加并绑定企业微信联系人");
        if(b.enabled()&&b.mode().equals("INSTANT"))BusinessException.require(db.count("SELECT COUNT(*) FROM reminder_permission WHERE user_id=? AND available>0",user)>0,409,"需要有效的一次性订阅授权；本地验收请先配置模拟授权");
        if(b.enabled()) {
            db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
            db.update("INSERT INTO user_follow(id,user_id,target_type,target_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE deleted_at=NULL",db.id(),user,b.targetType(),b.targetId());
        }
        if(id==null)db.update("INSERT INTO reminder_tracking(id,user_id,target_type,target_id,mode,enabled) VALUES(?,?,?,?,?,?)",key,user,b.targetType(),b.targetId(),b.mode(),b.enabled());
        else {db.update("UPDATE reminder_tracking SET mode=?,enabled=?,lock_version=lock_version+1 WHERE id=?",b.mode(),b.enabled(),key);
            db.update("UPDATE reminder_match m JOIN reminder_change c ON c.id=m.change_id JOIN reminder_tracking t ON t.id=m.tracking_id SET m.tracking_version=t.lock_version WHERE t.id=? AND c.state='PENDING'",key);}
        db.update("INSERT IGNORE INTO reminder_preference(user_id,slot) VALUES(?,?)",user,slots(db.one("SELECT * FROM reminder_control WHERE id=1")).get(0));
        db.audit(user,"CUSTOMER","REMINDER_TRACKING_SAVE","TRACKING",key,b);return db.one("SELECT * FROM reminder_tracking WHERE id=?",key);
    }
    /** Idempotent quick opt-in; existing instant consent/mode is never silently replaced. */
    @Transactional public Map<String,Object> daily(long user,String type,long target) {
        lock();active(user);
        BusinessException.require(Set.of("ARTICLE","KEYWORD").contains(type),422,"跟踪对象无效");
        if(type.equals("ARTICLE"))articles.visible(target);
        else db.one("SELECT id FROM keyword WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",target);
        // Current read is essential under REPEATABLE READ: authentication may already
        // have established a snapshot before this request waited for the control lock.
        var existing=db.rows("SELECT * FROM reminder_tracking WHERE user_id=? AND target_type=? AND target_id=? FOR UPDATE",user,type,target);
        if(existing.isEmpty())return save(user,null,new Tracking(type,target,"DAILY",true,0));
        var r=existing.get(0);
        if(Db.number(r.get("enabled"))==1){
            db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
            db.update("INSERT INTO user_follow(id,user_id,target_type,target_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE deleted_at=NULL",db.id(),user,type,target);
            return r;
        }
        return save(user,Db.number(r.get("id")),new Tracking(type,target,r.get("mode").toString(),true,(int)Db.number(r.get("lockVersion"))));
    }
    /** Operators may stop a rule, but cannot mint consent or resume it for the customer. */
    @Transactional public Map<String,Object> pauseByStaff(long id,int expectedVersion,String reason) {
        var staff=actors.staff();staff.require("yunji:notification:manage");lock();
        String proof=Db.text(reason,500);BusinessException.require(proof.length()>=10,422,"请填写至少十个字符的暂停原因");
        var rule=db.one("SELECT * FROM reminder_tracking WHERE id=?",id);
        BusinessException.require(Db.number(rule.get("lockVersion"))==expectedVersion,409,"规则已变化，请刷新");
        BusinessException.require(Db.number(rule.get("enabled"))==1,409,"规则已暂停");
        db.update("UPDATE reminder_tracking SET enabled=0,lock_version=lock_version+1 WHERE id=?",id);
        db.audit(staff.id(),"STAFF","REMINDER_OPERATOR_PAUSE","TRACKING",id,Map.of("reason",proof,"expectedVersion",expectedVersion));
        return detail(Db.number(rule.get("userId")),id);
    }
    public Map<String,Object> list(long user,int page,int size,String mode) {return list(user,page,size,mode,"",null);}
    public Map<String,Object> list(long user,int page,int size,String mode,String type,Long target) {return list(user,page,size,mode,type,target,false);}
    public Map<String,Object> list(long user,int page,int size,String mode,String type,Long target,boolean enabledOnly) {
        BusinessException.require(Set.of("","DAILY","INSTANT").contains(mode)&&Set.of("","ARTICLE","KEYWORD").contains(type),422,"提醒模式或对象无效");
        BusinessException.require(target==null||!type.isBlank(),422,"指定对象时必须提供对象类型");
        String condition=" WHERE user_id=? AND (?='' OR mode=?) AND (?='' OR target_type=?) AND (? IS NULL OR target_id=?)"+(enabledOnly?" AND enabled=1":"");
        Object[] args={user,mode,mode,type,type,target,target};
        var values=new ArrayList<Object>(Arrays.asList(args));values.add(size);values.add((page-1)*size);
        return Map.of("items",db.rows("SELECT t.*,CASE WHEN target_type='ARTICLE' THEN (SELECT title FROM article WHERE id=t.target_id) ELSE (SELECT display_name FROM keyword WHERE id=t.target_id) END title,CASE WHEN target_type='ARTICLE' THEN (SELECT content_type FROM article WHERE id=t.target_id) ELSE 'KEYWORD' END content_type,(SELECT slot FROM reminder_preference WHERE user_id=t.user_id) slot,(SELECT available FROM reminder_permission WHERE user_id=t.user_id) available,(SELECT COUNT(*) FROM reminder_change c WHERE c.user_id=t.user_id AND c.article_id=t.target_id AND c.state='PENDING') pending_count FROM reminder_tracking t"+condition+" ORDER BY id DESC LIMIT ? OFFSET ?",values.toArray()),"total",db.count("SELECT COUNT(*) FROM reminder_tracking"+condition,args),"page",page,"size",size);
    }
    public Map<String,Object> detail(long user,long id) {
        return db.one("SELECT t.*,CASE WHEN target_type='ARTICLE' THEN (SELECT title FROM article WHERE id=t.target_id) ELSE (SELECT display_name FROM keyword WHERE id=t.target_id) END title FROM reminder_tracking t WHERE user_id=? AND id=?",user,id);
    }
    public record KeywordSummary(String id,String name,long newCount,String latestArticleId,String latestTitle,String latestDate,Long trackingId,boolean enabled){}
    public List<KeywordSummary> keywords(long user){
        // Window aggregation avoids one article query per followed keyword and returns exact counts.
        return db.rows("SELECT * FROM (SELECT f.target_id id,k.display_name name,COALESCE(t.enabled,0) enabled,t.id tracking_id,f.created_at followed_at,COUNT(a.id) OVER(PARTITION BY f.id) new_count,a.id latest_article_id,a.title latest_title,a.published_at latest_date,ROW_NUMBER() OVER(PARTITION BY f.id ORDER BY a.published_at DESC,a.id DESC) position_no FROM user_follow f JOIN keyword k ON k.id=f.target_id LEFT JOIN reminder_tracking t ON t.user_id=f.user_id AND t.target_type='KEYWORD' AND t.target_id=f.target_id LEFT JOIN article_keyword_rel ak ON ak.keyword_id=f.target_id LEFT JOIN article a ON a.id=ak.article_id AND a.published_at>=f.created_at AND "+ArticleAccess.VISIBLE+" AND NOT EXISTS(SELECT 1 FROM user_read_state r WHERE r.user_id=f.user_id AND r.article_id=a.id AND r.read_version_id=a.current_version_id) WHERE f.user_id=? AND f.target_type='KEYWORD' AND f.deleted_at IS NULL AND k.tenant_id='000000' AND k.status='ENABLED' AND k.deleted_at IS NULL) ranked WHERE position_no=1 ORDER BY followed_at DESC,id DESC LIMIT 200",user).stream().map(r->new KeywordSummary(r.get("id").toString(),r.get("name").toString(),Db.number(r.get("newCount")),(String)r.get("latestArticleId"),(String)r.get("latestTitle"),Objects.toString(r.get("latestDate"),""),r.get("trackingId")==null?null:Db.number(r.get("trackingId")),Db.number(r.get("enabled"))==1)).toList();
    }
    public Map<String,Object> changes(long user,int page,int size) {return changes(user,page,size,"");}
    public Map<String,Object> changes(long user,int page,int size,String mode) {
        BusinessException.require(Set.of("","DAILY","INSTANT").contains(mode),422,"提醒方式无效");
        String scoped=mode.isBlank()?"":" AND EXISTS(SELECT 1 FROM reminder_match m JOIN reminder_tracking t ON t.id=m.tracking_id WHERE m.change_id=c.id AND t.enabled=1 AND t.mode='"+mode+"' AND m.tracking_version=t.lock_version) ";
        return page("SELECT c.*,v.title FROM reminder_change c JOIN article_version v ON v.id=c.version_id JOIN article a ON a.id=c.article_id WHERE c.user_id=? AND c.state IN('PENDING','BATCHED') AND "+ArticleAccess.VISIBLE+scoped+" ORDER BY c.created_at DESC,c.id DESC",
            "SELECT COUNT(*) FROM reminder_change c JOIN article a ON a.id=c.article_id WHERE c.user_id=? AND c.state IN('PENDING','BATCHED') AND "+ArticleAccess.VISIBLE+scoped,user,page,size);
    }
    private Map<String,Object> page(String sql,String count,long user,int page,int size) {return Map.of("items",db.rows(sql+" LIMIT ? OFFSET ?",user,size,(page-1)*size),"total",db.count(count,user),"page",page,"size",size);}
    @Transactional public void dismiss(long user,long id) {
        lock();var c=db.one("SELECT * FROM reminder_change WHERE id=? AND user_id=?",id,user);
        BusinessException.require(c.get("state").equals("PENDING"),409,"更新已进入发送批次，不能假称撤回");
        db.update("UPDATE reminder_change SET state='DISMISSED' WHERE id=?",id);db.audit(user,"CUSTOMER","REMINDER_DISMISS","CHANGE",id,Map.of());
    }
    @Transactional public void read(long user,long article,long version) {
        lock();db.update("UPDATE reminder_change c JOIN article_version v ON v.id=c.version_id JOIN article_version readv ON readv.id=? SET c.state='READ' WHERE c.user_id=? AND c.article_id=? AND c.state='PENDING' AND v.version_no<=readv.version_no",version,user,article);
    }
    @Transactional public void published(long article,long version,boolean first) {lock();capture(article,version,first?"PUBLISHED":"REVISED",first);}
    private void capture(long article,long version,String event,boolean keywordMatches) {
        var rules=db.rows("SELECT t.* FROM reminder_tracking t JOIN app_user u ON u.id=t.user_id WHERE t.enabled=1 AND u.status='ACTIVE' AND u.deleted_at IS NULL AND ((t.target_type='ARTICLE' AND t.target_id=?) OR (? AND t.target_type='KEYWORD' AND EXISTS(SELECT 1 FROM article_keyword_rel k WHERE k.article_id=? AND k.keyword_id=t.target_id))) ORDER BY t.user_id,t.id",article,keywordMatches,article);
        for(var t:rules) {
            long user=Db.number(t.get("userId"));
            if(db.count("SELECT COUNT(*) FROM user_read_state r JOIN article_version v ON v.id=r.read_version_id JOIN article_version nv ON nv.id=? WHERE r.user_id=? AND r.article_id=? AND v.version_no>=nv.version_no",version,user,article)>0)continue;
            db.update("UPDATE reminder_change SET state='SUPERSEDED' WHERE user_id=? AND article_id=? AND version_id<>? AND state='PENDING'",user,article,version);
            db.update("INSERT IGNORE INTO reminder_change(id,user_id,article_id,version_id,event_type) VALUES(?,?,?,?,?)",db.id(),user,article,version,event);
            var c=db.one("SELECT id FROM reminder_change WHERE user_id=? AND article_id=? AND version_id=?",user,article,version);
            db.update("INSERT IGNORE INTO reminder_match(change_id,tracking_id,tracking_version) VALUES(?,?,?)",c.get("id"),t.get("id"),t.get("lockVersion"));
        }
    }
    @Transactional public void replaced(long old,long newer,long version) {
        lock();
        for(var t:db.rows("SELECT * FROM reminder_tracking WHERE target_type='ARTICLE' AND target_id=?",old)) {
            var duplicate=db.rows("SELECT id FROM reminder_tracking WHERE user_id=? AND target_type='ARTICLE' AND target_id=?",t.get("userId"),newer);
            if(duplicate.isEmpty())db.update("UPDATE reminder_tracking SET target_id=?,lock_version=lock_version+1 WHERE id=?",newer,t.get("id"));
            else db.update("UPDATE reminder_tracking SET enabled=0,lock_version=lock_version+1 WHERE id=?",t.get("id"));
        }
        db.update("UPDATE reminder_change SET state='SUPERSEDED' WHERE article_id=? AND state='PENDING'",old);capture(newer,version,"REPLACED",false);
    }
    @Transactional public void close(long user) {lock();db.update("UPDATE reminder_tracking SET enabled=0,lock_version=lock_version+1 WHERE user_id=?",user);db.update("UPDATE reminder_change SET state='CANCELLED' WHERE user_id=? AND state='PENDING'",user);db.update("UPDATE reminder_permission SET wecom_bound=0,available=0,wecom_contact_state='NONE' WHERE user_id=?",user);}
    @Transactional public Map<String,Object> authorize(long user,Authorization b,String supplied) {
        BusinessException.require(local&&!secret.isBlank()&&java.security.MessageDigest.isEqual(secret.getBytes(),Objects.toString(supplied,"").getBytes()),404,"接口不存在");
        lock();active(user);BusinessException.require(Set.of("SUCCESS","FAILURE","UNKNOWN","RETRY").contains(b.outcome()),422,"模拟结果无效");
        String hash=Db.hash(user+":"+db.json(b));var old=db.rows("SELECT payload_hash FROM reminder_local_receipt WHERE receipt_key=?",b.receiptKey());
        if(!old.isEmpty()){BusinessException.require(hash.equals(old.get(0).get("payloadHash")),409,"重复键内容冲突");return settings(user);}
        db.update("INSERT INTO reminder_local_receipt(receipt_key,payload_hash) VALUES(?,?)",b.receiptKey(),hash);
        db.update("INSERT INTO reminder_permission(user_id,wecom_bound,available,simulation_outcome) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE wecom_bound=VALUES(wecom_bound),available=available+VALUES(available),simulation_outcome=VALUES(simulation_outcome)",user,b.wecomBound(),b.amount(),b.outcome());return settings(user);
    }
}
