package com.sunshine.yunji.content.engagement.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import com.sunshine.yunji.content.identity.application.Actors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;

@Service
public class EngagementService {
 private final Db db;private final ArticleAccess articles;private final Actors actors;private final com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders;
 public EngagementService(Db db,ArticleAccess articles,Actors actors,com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders){this.db=db;this.articles=articles;this.actors=actors;this.reminders=reminders;}
 @Transactional public void favorite(long user,long article,boolean enabled){if(enabled)articles.readable(article);if(enabled)db.update("INSERT INTO user_favorite(id,user_id,article_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE deleted_at=NULL",db.id(),user,article);else db.update("UPDATE user_favorite SET deleted_at=UTC_TIMESTAMP() WHERE user_id=? AND article_id=?",user,article);db.audit(user,"CUSTOMER","FAVORITE_SET","ARTICLE",article,Map.of("enabled",enabled));}
 public List<Map<String,Object>> favorites(long user){return db.rows("SELECT a.id,a.title,a.summary,a.current_version_id,f.created_at FROM user_favorite f JOIN article a ON a.id=f.article_id WHERE f.user_id=? AND f.deleted_at IS NULL AND "+ArticleAccess.READABLE+" ORDER BY f.created_at DESC,a.id DESC LIMIT 200",user);}
 @Transactional public void browse(long user,long article){articles.readable(article);db.update("INSERT INTO user_browse_history(id,user_id,article_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE last_browsed_at=UTC_TIMESTAMP(),browse_count=browse_count+1,deleted_at=NULL",db.id(),user,article);db.audit(user,"CUSTOMER","HISTORY_RECORD","ARTICLE",article,Map.of());}
 public List<Map<String,Object>> history(long user){return db.rows("SELECT a.id,a.title,h.last_browsed_at,h.browse_count FROM user_browse_history h JOIN article a ON a.id=h.article_id WHERE h.user_id=? AND h.deleted_at IS NULL AND "+ArticleAccess.READABLE+" ORDER BY h.last_browsed_at DESC,a.id DESC LIMIT 200",user);}
 @Transactional public void clearHistory(long user){db.update("UPDATE user_browse_history SET deleted_at=UTC_TIMESTAMP() WHERE user_id=?",user);db.audit(user,"CUSTOMER","HISTORY_CLEAR","USER",user,Map.of());}
 @Transactional public void removeHistory(long user,long article){db.update("UPDATE user_browse_history SET deleted_at=UTC_TIMESTAMP() WHERE user_id=? AND article_id=?",user,article);db.audit(user,"CUSTOMER","HISTORY_REMOVE","ARTICLE",article,Map.of());}
 @Transactional public void clearFavorites(long user){db.update("UPDATE user_favorite SET deleted_at=UTC_TIMESTAMP() WHERE user_id=?",user);db.audit(user,"CUSTOMER","FAVORITES_CLEAR","USER",user,Map.of());}
 @Transactional public void read(long user,long article,long version){reminders.lock();var a=articles.readable(article);BusinessException.require(Objects.equals(a.get("currentVersionId"),Long.toString(version)),409,"内容已更新，请读取当前版本");db.update("INSERT INTO user_read_state(user_id,article_id,read_version_id,source) VALUES(?,?,?,'EXPLICIT') ON DUPLICATE KEY UPDATE read_version_id=VALUES(read_version_id),read_at=UTC_TIMESTAMP(),source='EXPLICIT'",user,article,version);reminders.read(user,article,version);db.audit(user,"CUSTOMER","READ_CONFIRM","ARTICLE",article,Map.of("versionId",Long.toString(version)));}
 @Transactional public Map<String,Object> report(long user,long article,String type,String reason){articles.readable(article);BusinessException.require(Set.of("CONTENT","SOURCE","OTHER").contains(type),422,"举报类型错误");db.one("SELECT id FROM article WHERE id=? FOR UPDATE",article);long id=db.id();db.update("INSERT INTO user_report(id,user_id,article_id,report_type,reason) VALUES(?,?,?,?,?)",id,user,article,type,Db.text(reason,1000));db.update("UPDATE article SET abnormal=1 WHERE id=?",article);db.audit(user,"CUSTOMER","REQUEST_CREATE","REQUEST",id,Map.of());return Map.of("id",Long.toString(id),"status","PENDING");}
 @Transactional public Map<String,Object> feedback(long user,String text){return feedback(user,text,null,null);}
 @Transactional public Map<String,Object> feedback(long user,String text,String type,String contact){long id=db.id();String description=text+(contact==null||contact.isBlank()?"":"\n联系方式（用户填写，未验证）："+contact);db.update("INSERT INTO feedback(id,user_id,type,description) VALUES(?,?,?,?)",id,user,type==null?"GENERAL":type,Db.text(description,2000));db.audit(user,"CUSTOMER","REQUEST_CREATE","REQUEST",id,Map.of());return Map.of("id",Long.toString(id),"status","PENDING");}
 public List<Map<String,Object>> feedback(long user){return db.rows("SELECT id,type,description,status,created_at,handled_at FROM feedback WHERE user_id=? ORDER BY created_at DESC,id DESC LIMIT 100",user);}
 public List<Map<String,Object>> moderation(String kind){var a=actors.staff();a.require("yunji:engagement:manage");String table=kind.equals("reports")?"user_report":kind.equals("feedback")?"feedback":null;BusinessException.require(table!=null,404,"不存在此工单类型");return db.rows("SELECT * FROM "+table+" ORDER BY created_at DESC,id DESC LIMIT 200");}
 @Transactional public void handle(String kind,long id,String status){var a=actors.staff();a.require("yunji:engagement:manage");BusinessException.require(Set.of("RESOLVED","DISMISSED").contains(status),422,"处理状态错误");String table=kind.equals("reports")?"user_report":kind.equals("feedback")?"feedback":null;BusinessException.require(table!=null,404,"不存在此工单类型");var ticket=db.one("SELECT * FROM "+table+" WHERE id=? AND status='PENDING'",id);if(table.equals("user_report"))db.one("SELECT id FROM article WHERE id=? FOR UPDATE",ticket.get("articleId"));db.one("SELECT id FROM "+table+" WHERE id=? AND status='PENDING' FOR UPDATE",id);db.update("UPDATE "+table+" SET status=?,"+(table.equals("feedback")?"handler_id":"handled_by")+"=?,handled_at=UTC_TIMESTAMP() WHERE id=?",status,a.id(),id);if(table.equals("user_report"))db.update("UPDATE article SET abnormal=EXISTS(SELECT 1 FROM user_report WHERE article_id=? AND status='PENDING') WHERE id=?",ticket.get("articleId"),ticket.get("articleId"));db.audit(a.id(),"STAFF","MODERATION_HANDLE",kind,id,Map.of("status",status));}
 @Transactional public void event(long user,String eventId,String type,Long objectId,Instant occurredAt,Map<String,Object> payload){
  reminders.lock();
  BusinessException.require(eventId!=null&&eventId.matches("[A-Za-z0-9_-]{8,64}"),422,"事件ID错误");
  BusinessException.require(Set.of("VISIBLE_EXPOSURE","PAGE_VIEW","PAGE_STAY","SEARCH_CLICK","FILTER","BANNER_CLICK","DOWNLOAD_INTENT","SHARE").contains(type),422,"事件类型不支持");
  Instant now=Instant.now();BusinessException.require(occurredAt!=null&&occurredAt.isAfter(now.minusSeconds(86400))&&occurredAt.isBefore(now.plusSeconds(300)),422,"客户端时间超出允许范围");
  String json=db.json(payload==null?Map.of():payload);BusinessException.require(json.length()<=2000,422,"事件载荷过大");
  if(Set.of("VISIBLE_EXPOSURE","PAGE_VIEW","PAGE_STAY","SEARCH_CLICK","SHARE").contains(type)){BusinessException.require(objectId!=null,422,"需要内容ID");articles.readable(objectId);}
  if(type.equals("PAGE_STAY")){Object duration=payload==null?null:payload.get("durationMs");BusinessException.require(duration instanceof Number&&Db.number(duration)>=1000&&Db.number(duration)<=3600000,422,"停留时长必须为1000至3600000毫秒");}
  if(type.equals("VISIBLE_EXPOSURE")){Object ratio=payload==null?null:payload.get("visibleRatio"),duration=payload==null?null:payload.get("durationMs");BusinessException.require(ratio instanceof Number&&((Number)ratio).doubleValue()>=.5&&((Number)ratio).doubleValue()<=1&&duration instanceof Number&&Db.number(duration)>=1000,422,"曝光需可视至少50%且停留至少1秒");}
  if(type.equals("PAGE_VIEW")&&db.count("SELECT COUNT(*) FROM behavior_event WHERE user_id=? AND object_id=? AND event_type='PAGE_VIEW' AND ingested_at>UTC_TIMESTAMP()-INTERVAL 30 SECOND",user,objectId)>0){var current=articles.readable(objectId);read(user,objectId,Db.number(current.get("currentVersionId")));return;}
  var old=db.rows("SELECT event_type,object_id,payload FROM behavior_event WHERE user_id=? AND client_event_id=?",user,eventId);
  if(!old.isEmpty()){var r=old.get(0);BusinessException.require(type.equals(r.get("eventType"))&&Objects.equals(objectId==null?null:objectId.toString(),r.get("objectId"))&&db.object(json).equals(db.object(r.get("payload").toString())),409,"事件ID重复但内容不同");return;}
  db.update("INSERT INTO behavior_event(id,user_id,client_event_id,event_type,object_type,object_id,payload,occurred_at) VALUES(?,?,?,?,'ARTICLE',?,?,?)",db.id(),user,eventId,type,objectId,json,Timestamp.from(occurredAt));
  if(type.equals("PAGE_VIEW")){var current=articles.readable(objectId);read(user,objectId,Db.number(current.get("currentVersionId")));}
  if(type.equals("VISIBLE_EXPOSURE")){
   db.update("INSERT IGNORE INTO exposure_receipt(event_id,user_id,article_id,request_hash) VALUES(?,?,?,?)",eventId,user,objectId,Db.hash(json));
   // Server receipt time controls cooldown; client cannot reset it through forged timestamps.
   db.update("INSERT IGNORE INTO user_exposure_total(user_id,article_id,day_key) VALUES(?,?,DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR))",user,objectId);
   var exposure=db.one("SELECT *,last_visible_at IS NULL OR last_visible_at<=UTC_TIMESTAMP()-INTERVAL 30 SECOND countable FROM user_exposure_total WHERE user_id=? AND article_id=? FOR UPDATE",user,objectId);
   if(Db.number(exposure.get("countable"))==1){
    db.update("UPDATE user_exposure_total SET visible_count=visible_count+1,day_count=IF(day_key=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR),day_count+1,1),day_key=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR),last_visible_at=UTC_TIMESTAMP(6) WHERE user_id=? AND article_id=?",user,objectId);
    if(Db.number(exposure.get("visibleCount"))+1>=9){var current=articles.readable(objectId);reminders.read(user,objectId,Db.number(current.get("currentVersionId")));}
   }
  }
 }
}
