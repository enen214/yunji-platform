package com.sunshine.yunji.content.notification.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class NotificationService {
 private final Db db;private final Actors actors;private final ArticleAccess articles;private final boolean local;private final String secret;
 public NotificationService(Db db,Actors actors,ArticleAccess articles,@Value("${yunji.local-enabled:false}") boolean local,@Value("${yunji.local-secret:}") String secret){this.db=db;this.actors=actors;this.articles=articles;this.local=local;this.secret=secret;}
 public record Rule(String targetType,long targetId,String mode,String channel,String timezone,Instant scheduledAt,String localSendTime,String quietStart,String quietEnd,boolean enabled,int expectedVersion){}
 public record Authorization(String provider,String appId,String callbackKey,int amount,String outcome){}
 private void localSecret(String supplied){BusinessException.require(local&&!secret.isBlank()&&java.security.MessageDigest.isEqual(secret.getBytes(),Objects.toString(supplied,"").getBytes()),404,"接口不存在");}
 private LocalTime time(String value){if(value==null||value.isBlank())return null;try{return LocalTime.parse(value);}catch(Exception e){throw new BusinessException(422,"时间格式错误");}}
 public static String ruleHash(Db db,String targetType,long targetId,String mode,String channel,String timezone,String scheduledAt,String localSendTime){
  String at=scheduledAt.isBlank()?"":Instant.parse(scheduledAt).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
  return Db.hash(db.json(new TreeMap<>(Map.of("targetType",targetType,"targetId",Long.toString(targetId),"mode",mode,"channel",channel,"zone",timezone,"at",at,"time",localSendTime))));
 }
 @Transactional public Map<String,Object> rule(long user,Long id,Rule b){
  db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
  BusinessException.require(Set.of("ARTICLE","KEYWORD").contains(b.targetType)&&Set.of("INSTANT","AT_TIME","DAILY").contains(b.mode)&&Set.of("WECHAT_SUBSCRIBE","WECom").contains(b.channel),422,"提醒目标、模式或通道错误");
  if(b.targetType.equals("ARTICLE"))articles.readable(b.targetId);else db.one("SELECT id FROM keyword WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",b.targetId);
  ZoneId zone;try{zone=ZoneId.of(b.timezone);}catch(Exception e){throw new BusinessException(422,"时区错误");}LocalTime send=time(b.localSendTime),quietStart=time(b.quietStart),quietEnd=time(b.quietEnd);
  BusinessException.require((quietStart==null)==(quietEnd==null)&&(quietStart==null||!quietStart.equals(quietEnd)),422,"静默时段需要不同的起止时间");Instant next=null;
  if(b.mode.equals("AT_TIME")){BusinessException.require(b.scheduledAt!=null&&b.scheduledAt.isAfter(Instant.now()),422,"提醒时间必须在未来");next=b.scheduledAt;}
  if(b.mode.equals("DAILY")){BusinessException.require(send!=null,422,"每日提醒需要时间");var z=ZonedDateTime.now(zone).with(send);if(!z.toInstant().isAfter(Instant.now()))z=z.plusDays(1);next=z.toInstant();}
  String hash=ruleHash(db,b.targetType,b.targetId,b.mode,b.channel,b.timezone,Objects.toString(b.scheduledAt,""),Objects.toString(send,""));long key=id==null?db.id():id;
  if(id==null)db.update("INSERT INTO notification_rule(id,user_id,target_type,target_id,mode,channel,timezone,scheduled_at,local_send_time,quiet_start,quiet_end,enabled,rule_hash,next_run_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",key,user,b.targetType,b.targetId,b.mode,b.channel,b.timezone,b.scheduledAt==null?null:Timestamp.from(b.scheduledAt),send,quietStart,quietEnd,b.enabled,hash,next==null?null:Timestamp.from(next));
  else{var old=db.one("SELECT * FROM notification_rule WHERE id=? AND user_id=? FOR UPDATE",id,user);BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion,409,"提醒规则已变化");db.update("UPDATE notification_rule SET target_type=?,target_id=?,mode=?,channel=?,timezone=?,scheduled_at=?,local_send_time=?,quiet_start=?,quiet_end=?,enabled=?,rule_hash=?,next_run_at=?,lock_version=lock_version+1 WHERE id=?",b.targetType,b.targetId,b.mode,b.channel,b.timezone,b.scheduledAt==null?null:Timestamp.from(b.scheduledAt),send,quietStart,quietEnd,b.enabled,hash,next==null?null:Timestamp.from(next),key);}
  db.audit(user,"CUSTOMER","REMINDER_RULE_SAVE","NOTIFICATION_RULE",key,Map.of("enabled",b.enabled));return db.one("SELECT * FROM notification_rule WHERE id=?",key);
 }
 public List<Map<String,Object>> rules(long user){return db.rows("SELECT * FROM notification_rule WHERE user_id=? ORDER BY id DESC LIMIT 100",user);}
 @Transactional public void cancel(long user,long id){db.one("SELECT id FROM notification_rule WHERE id=? AND user_id=? FOR UPDATE",id,user);db.update("UPDATE notification_rule SET enabled=0,lock_version=lock_version+1 WHERE id=?",id);db.audit(user,"CUSTOMER","REMINDER_CANCEL","NOTIFICATION_RULE",id,Map.of());}
 public Map<String,Object> status(long user){return Map.of("bindings",db.rows("SELECT provider,app_id,binding_state FROM notification_binding WHERE user_id=?",user),"quotas",db.rows("SELECT id,app_id,channel,template_key,available,reserved,consumed FROM notification_quota WHERE user_id=?",user),"platformStatus",local?"LOCAL_FAKE":"PLATFORM_PENDING");}
 @Transactional public Map<String,Object> authorizeLocal(long user,Authorization b,String supplied){
  localSecret(supplied);BusinessException.require(Set.of("WECHAT_SUBSCRIBE","WECom").contains(b.provider)&&b.appId.equals("local-app")&&b.amount>=0&&b.amount<=10,422,"本地授权参数错误");BusinessException.require(Set.of("SENT","PERMANENT_FAILURE","RETRYABLE_FAILURE","UNKNOWN").contains(b.outcome),422,"模拟结果错误");
  String key=Db.text(b.callbackKey,100),hash=Db.hash(user+":"+db.json(b));var old=db.rows("SELECT payload_hash FROM notification_callback_receipt WHERE provider=? AND callback_key=? FOR UPDATE",b.provider,key);
  if(!old.isEmpty()){BusinessException.require(hash.equals(old.get(0).get("payloadHash")),409,"回调键内容冲突");return status(user);}
  db.update("INSERT INTO notification_callback_receipt(provider,callback_key,payload_hash) VALUES(?,?,?)",b.provider,key,hash);
  // Synthetic provider evidence only: production cannot call this endpoint or mint quota from client acceptance.
  db.update("INSERT INTO notification_binding(id,user_id,provider,app_id,subject_hash,binding_state) VALUES(?,?,?,?,?,'BOUND') ON DUPLICATE KEY UPDATE binding_state='BOUND'",db.id(),user,b.provider,b.appId,Db.hash("local-only:"+user+":"+b.provider));
  db.update("INSERT INTO notification_quota(id,user_id,app_id,channel,template_key,available) VALUES(?,?,?,?,'CONTENT',?) ON DUPLICATE KEY UPDATE available=available+VALUES(available),lock_version=lock_version+1",db.id(),user,b.appId,b.provider,b.amount);
  db.update("INSERT INTO local_notification_outcome(user_id,outcome) VALUES(?,?) ON DUPLICATE KEY UPDATE outcome=VALUES(outcome)",user,b.outcome);return status(user);
 }
 public List<Map<String,Object>> deliveries(long user){return db.rows("SELECT id,rule_id,article_id,article_version_id,channel,status,attempts,error_code,sent_at,created_at FROM notification_delivery WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 200",user);}
 private void enqueue(Map<String,Object> rule,Long article,Long version,String dedupe){
  List<Map<String,Object>> content;
  if(version!=null)content=db.rows("SELECT id version_id,article_id,title,summary FROM article_version WHERE id=? AND article_id=?",version,article);
  else content=db.rows("SELECT a.id article_id,a.current_version_id version_id,a.title,a.summary FROM article a WHERE "+ArticleAccess.VISIBLE+" AND EXISTS(SELECT 1 FROM article_keyword_rel k WHERE k.article_id=a.id AND k.keyword_id=?) ORDER BY a.published_at DESC,a.id DESC LIMIT 20",rule.get("targetId"));
  db.update("INSERT IGNORE INTO notification_delivery(id,user_id,article_id,article_version_id,channel,template_key,rule_id,rule_version,dedupe_key,rule_snapshot,content_snapshot) VALUES(?,?,?,?,?,'CONTENT',?,?,?,?,?)",db.id(),rule.get("userId"),article,version,rule.get("channel"),rule.get("id"),rule.get("lockVersion"),dedupe,db.json(rule),db.json(Map.of("items",content)));
 }
 @Transactional public Map<String,Object> tick(){
  actors.staff().require("yunji:notification:manage");BusinessException.require(local,503,"真实通知供应商尚未配置");int planned=0;
  var events=db.rows("SELECT e.* FROM outbox_event e WHERE e.event_type='ARTICLE_PUBLISHED' AND NOT EXISTS(SELECT 1 FROM event_consumer_receipt r WHERE r.consumer_name='notifications' AND r.event_id=e.id) ORDER BY e.created_at,e.id LIMIT 100 FOR UPDATE SKIP LOCKED");
  for(var event:events){var payload=db.object(event.get("payload").toString());long article=Db.number(payload.get("articleId")),version=Db.number(payload.get("versionId"));
   var snapshot=db.object(db.one("SELECT relation_snapshot FROM article_version WHERE id=? AND article_id=?",version,article).get("relationSnapshot").toString());
   if(db.count("SELECT COUNT(*) FROM article a WHERE "+ArticleAccess.CURRENT+" AND a.id=?",article)==0){db.update("INSERT INTO event_consumer_receipt(consumer_name,event_id) VALUES('notifications',?)",event.get("id"));continue;}
   var rules=db.rows("SELECT r.* FROM notification_rule r WHERE r.enabled=1 AND r.mode='INSTANT' AND r.created_at<=? ORDER BY r.id FOR UPDATE",Timestamp.from(Instant.parse(event.get("createdAt").toString())));
   for(var rule:rules){boolean match=rule.get("targetType").equals("ARTICLE")?Db.number(rule.get("targetId"))==article:((List<?>)snapshot.get("keywordIds")).stream().anyMatch(k->Db.number(k)==Db.number(rule.get("targetId")));if(match){enqueue(rule,article,version,"event:"+event.get("id")+":rule:"+rule.get("id"));planned++;}}
   db.update("INSERT INTO event_consumer_receipt(consumer_name,event_id) VALUES('notifications',?)",event.get("id"));}
  var due=db.rows("SELECT * FROM notification_rule WHERE enabled=1 AND mode IN('AT_TIME','DAILY') AND next_run_at<=UTC_TIMESTAMP() ORDER BY next_run_at,id LIMIT 100 FOR UPDATE SKIP LOCKED");
  for(var rule:due){if(rule.get("targetType").equals("ARTICLE")&&db.count("SELECT COUNT(*) FROM article a WHERE "+ArticleAccess.CURRENT+" AND a.id=?",rule.get("targetId"))==0){db.update("UPDATE notification_rule SET next_run_at=NULL,enabled=0,lock_version=lock_version+1 WHERE id=?",rule.get("id"));continue;}Long article=rule.get("targetType").equals("ARTICLE")?Db.number(rule.get("targetId")):null;Long version=article==null?null:Db.number(articles.readable(article).get("currentVersionId"));enqueue(rule,article,version,"rule:"+rule.get("id")+":v:"+rule.get("lockVersion")+":due:"+rule.get("nextRunAt"));planned++;
   if(rule.get("mode").equals("AT_TIME"))db.update("UPDATE notification_rule SET next_run_at=NULL WHERE id=?",rule.get("id"));else{Instant next=Instant.parse(rule.get("nextRunAt").toString()).atZone(ZoneId.of(rule.get("timezone").toString())).plusDays(1).toInstant();db.update("UPDATE notification_rule SET next_run_at=? WHERE id=?",Timestamp.from(next),rule.get("id"));}}
  var rows=db.rows("SELECT * FROM notification_delivery WHERE status IN('PENDING','RETRYABLE_FAILURE','WAITING_QUOTA') AND next_attempt_at<=UTC_TIMESTAMP() ORDER BY next_attempt_at,id LIMIT 100 FOR UPDATE SKIP LOCKED");int processed=0;for(var delivery:rows){process(delivery);processed++;}
  return Map.of("planned",planned,"processed",processed,"platformStatus","LOCAL_FAKE");
 }
 private void settle(Map<String,Object> d,String operation){long id=Db.number(d.get("id"));var reserve=db.rows("SELECT q.* FROM notification_quota q JOIN notification_quota_ledger l ON l.quota_id=q.id WHERE l.delivery_id=? AND l.operation='RESERVE' FOR UPDATE",id);if(reserve.isEmpty())return;var q=reserve.get(0);if(db.count("SELECT COUNT(*) FROM notification_quota_ledger WHERE delivery_id=? AND operation IN('COMMIT','RELEASE')",id)>0)return;
  db.update("UPDATE notification_quota SET reserved=reserved-1,"+(operation.equals("COMMIT")?"consumed=consumed+1":"available=available+1")+",lock_version=lock_version+1 WHERE id=? AND reserved>0",q.get("id"));db.update("INSERT INTO notification_quota_ledger(id,quota_id,delivery_id,operation) VALUES(?,?,?,?)",db.id(),q.get("id"),id,operation);
 }
 private void process(Map<String,Object> d){long id=Db.number(d.get("id"));var rules=db.rows("SELECT r.* FROM notification_rule r JOIN app_user u ON u.id=r.user_id WHERE r.id=? AND u.status='ACTIVE' AND u.deleted_at IS NULL FOR UPDATE",d.get("ruleId"));
  if(rules.isEmpty()||Db.number(rules.get(0).get("enabled"))==0||Db.number(rules.get(0).get("lockVersion"))!=Db.number(d.get("ruleVersion"))){settle(d,"RELEASE");db.update("UPDATE notification_delivery SET status='CANCELLED' WHERE id=?",id);return;}var rule=rules.get(0);
  if(d.get("articleId")!=null){try{db.one("SELECT a.id FROM article a WHERE "+ArticleAccess.CURRENT+" AND a.id=?",d.get("articleId"));}catch(BusinessException e){settle(d,"RELEASE");db.update("UPDATE notification_delivery SET status='CANCELLED',error_code='HIDDEN_CONTENT' WHERE id=?",id);return;}}
  if(rule.get("quietStart")!=null){LocalTime start=time(rule.get("quietStart").toString()),end=time(rule.get("quietEnd").toString());var z=ZonedDateTime.now(ZoneId.of(rule.get("timezone").toString()));LocalTime t=z.toLocalTime();boolean quiet=start.equals(end)||start.isBefore(end)?!t.isBefore(start)&&t.isBefore(end):!t.isBefore(start)||t.isBefore(end);if(quiet){var next=z.with(end);if(!next.isAfter(z))next=next.plusDays(1);db.update("UPDATE notification_delivery SET next_attempt_at=? WHERE id=?",Timestamp.from(next.toInstant()),id);return;}}
  var qs=db.rows("SELECT q.* FROM notification_quota q JOIN notification_binding b ON b.user_id=q.user_id AND b.provider=q.channel AND b.app_id=q.app_id WHERE q.user_id=? AND q.channel=? AND q.template_key='CONTENT' AND q.app_id='local-app' AND b.binding_state='BOUND' FOR UPDATE",d.get("userId"),d.get("channel"));
  if(qs.isEmpty()){db.update("UPDATE notification_delivery SET status='WAITING_QUOTA',next_attempt_at=UTC_TIMESTAMP()+INTERVAL 1 MINUTE WHERE id=?",id);return;}var q=qs.get(0);
  if(Db.number(d.get("quotaReserved"))==0){if(Db.number(q.get("available"))<1){db.update("UPDATE notification_delivery SET status='WAITING_QUOTA',next_attempt_at=UTC_TIMESTAMP()+INTERVAL 1 MINUTE WHERE id=?",id);return;}db.update("UPDATE notification_quota SET available=available-1,reserved=reserved+1,lock_version=lock_version+1 WHERE id=?",q.get("id"));db.update("INSERT INTO notification_quota_ledger(id,quota_id,delivery_id,operation) VALUES(?,?,?,'RESERVE')",db.id(),q.get("id"),id);db.update("UPDATE notification_delivery SET quota_reserved=1 WHERE id=?",id);}
  String outcome=db.one("SELECT outcome FROM local_notification_outcome WHERE user_id=?",d.get("userId")).get("outcome").toString();long attempts=Db.number(d.get("attempts"))+1;if(attempts>=5&&outcome.equals("RETRYABLE_FAILURE"))outcome="PERMANENT_FAILURE";
  db.update("INSERT INTO notification_attempt(id,delivery_id,attempt_no,request_hash,outcome,provider_code,finished_at) VALUES(?,?,?,?,?,'LOCAL_FAKE',UTC_TIMESTAMP())",db.id(),id,attempts,Db.hash(db.json(Map.of("deliveryId",Long.toString(id),"dedupeKey",d.get("dedupeKey"),"content",Objects.toString(d.get("contentSnapshot"),"{}")))),outcome);
  db.update("UPDATE notification_delivery SET status=?,attempts=?,provider_message_id=?,error_code=?,sent_at=IF(?='SENT',UTC_TIMESTAMP(),NULL),next_attempt_at=UTC_TIMESTAMP()+INTERVAL ? SECOND WHERE id=?",outcome,attempts,outcome.equals("SENT")?"local:"+id:null,outcome.equals("SENT")?null:"LOCAL_"+outcome,outcome,(int)Math.min(3600,30*Math.pow(2,attempts)),id);
  if(outcome.equals("SENT"))settle(d,"COMMIT");else if(outcome.equals("PERMANENT_FAILURE"))settle(d,"RELEASE");
  // UNKNOWN remains reserved and excluded from retries. No claim of exactly-once delivery.
 }
 @Transactional public Map<String,Object> reconcile(long id,String outcome,String evidence){actors.staff().require("yunji:notification:manage");BusinessException.require(Set.of("SENT","PERMANENT_FAILURE").contains(outcome),422,"只允许有证据的终态");String proof=Db.text(evidence,1000);BusinessException.require(proof.length()>=10,422,"核实证据需要至少10字符");var d=db.one("SELECT * FROM notification_delivery WHERE id=? AND status='UNKNOWN' FOR UPDATE",id);settle(d,outcome.equals("SENT")?"COMMIT":"RELEASE");db.update("UPDATE notification_delivery SET status=?,sent_at=IF(?='SENT',UTC_TIMESTAMP(),NULL) WHERE id=?",outcome,outcome,id);db.audit(actors.staff().id(),"STAFF","NOTIFICATION_RECONCILE","DELIVERY",id,Map.of("outcome",outcome,"evidence",proof));return db.one("SELECT * FROM notification_delivery WHERE id=?",id);}
 public List<Map<String,Object>> ledger(){actors.staff().require("yunji:notification:manage");return db.rows("SELECT d.*,q.operation,q.amount FROM notification_delivery d LEFT JOIN notification_quota_ledger q ON q.delivery_id=d.id ORDER BY d.created_at DESC,d.id DESC LIMIT 500");}
 public List<Map<String,Object>> adminRules(){actors.staff().require("yunji:notification:manage");return db.rows("SELECT * FROM notification_rule WHERE tenant_id='000000' ORDER BY updated_at DESC,id DESC LIMIT 200");}
 public Map<String,Object> capabilities(){actors.staff().require("yunji:notification:manage");return Map.of("localSimulation",local,"realProviderReady",false);}
}
