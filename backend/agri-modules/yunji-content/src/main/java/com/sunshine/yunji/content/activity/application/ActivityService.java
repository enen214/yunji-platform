package com.sunshine.yunji.content.activity.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class ActivityService {
 private final Db db;private final Actors actors;
 public ActivityService(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Field(String key,String label,String type,boolean required,int maxLength,List<String> choices){}
 public record Config(@NotBlank @Size(max=255) String title,Long organizationId,Long articleId,@NotNull Instant registrationStart,@NotNull Instant registrationEnd,@NotNull Instant eventStart,@NotNull Instant eventEnd,@Min(0) int capacity,boolean waitlistEnabled,@NotBlank String status,List<Field> fields,int expectedVersion){}
 public record Registration(long formVersionId,Map<String,Object> answers){}
 private static Timestamp ts(Instant v){return Timestamp.from(v);}
 public static void schema(List<Field> fields){BusinessException.require(fields!=null&&fields.size()<=30,422,"表单字段数量错误");Set<String> seen=new HashSet<>();for(var f:fields){BusinessException.require(f.key!=null&&f.key.matches("[a-zA-Z][a-zA-Z0-9_]{0,63}")&&seen.add(f.key),422,"表单字段名重复或非法");Db.text(f.label,150);BusinessException.require(Set.of("TEXT","NUMBER","CHOICE").contains(f.type)&&f.maxLength>0&&f.maxLength<=2000,422,"不支持的表单类型或长度");if(f.type.equals("CHOICE"))BusinessException.require(f.choices!=null&&!f.choices.isEmpty()&&f.choices.size()<=20,422,"选项配置错误");}}
 @SuppressWarnings("unchecked") public static void answers(Object schema,Map<String,Object> answers){
  BusinessException.require(answers!=null&&answers.size()<=30,422,"报名答案错误");var fields=(List<Map<String,Object>>)schema;Set<String> allowed=new HashSet<>();
  for(var f:fields){String key=f.get("key").toString();allowed.add(key);Object value=answers.get(key);boolean required=Boolean.TRUE.equals(f.get("required"));BusinessException.require(!required||(value!=null&&!value.toString().isBlank()),422,"缺少必填字段: "+f.get("label"));if(value==null)continue;
   BusinessException.require(!(value instanceof Map)&&!(value instanceof List)&&value.toString().length()<=Db.number(f.get("maxLength")),422,"字段长度或格式错误");
   String type=f.get("type").toString();if(type.equals("NUMBER"))BusinessException.require(value.toString().matches("-?\\d+(\\.\\d+)?"),422,"字段必须为数字");if(type.equals("CHOICE"))BusinessException.require(((List<Object>)f.get("choices")).contains(value),422,"请选择允许的选项");
  }BusinessException.require(allowed.containsAll(answers.keySet()),422,"含未定义字段");
 }
 @Transactional public Map<String,Object> save(Long id,Config b){
  var actor=actors.staff();actor.require("yunji:activity:manage");schema(b.fields);BusinessException.require(b.registrationEnd.isAfter(b.registrationStart)&&b.eventEnd.isAfter(b.eventStart)&&b.eventStart.compareTo(b.registrationEnd)>=0,422,"活动时间顺序错误");BusinessException.require(Set.of("DRAFT","PUBLISHED","CLOSED").contains(b.status),422,"活动状态错误");
  BusinessException.require(b.organizationId==null,422,"本期不支持活动组织归属");
  if(b.articleId!=null)db.one("SELECT id FROM article WHERE id=? AND tenant_id='000000'",b.articleId);
  long key=id==null?db.id():id;int version=1;
  if(id==null)db.update("INSERT INTO activity(id,title,organization_id,article_id,registration_start,registration_end,event_start,event_end,capacity,waitlist_enabled,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",key,b.title,b.organizationId,b.articleId,ts(b.registrationStart),ts(b.registrationEnd),ts(b.eventStart),ts(b.eventEnd),b.capacity,b.waitlistEnabled,b.status,actor.id());
  else{var old=db.one("SELECT * FROM activity WHERE id=? AND tenant_id='000000' FOR UPDATE",key);BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion&&Db.number(old.get("confirmedCount"))<=b.capacity,409,"活动版本或容量冲突");
   db.update("UPDATE activity SET title=?,registration_start=?,registration_end=?,event_start=?,event_end=?,capacity=?,waitlist_enabled=?,status=?,lock_version=lock_version+1 WHERE id=?",b.title,ts(b.registrationStart),ts(b.registrationEnd),ts(b.eventStart),ts(b.eventEnd),b.capacity,b.waitlistEnabled,b.status,key);
   version=(int)db.count("SELECT MAX(version_no)+1 FROM activity_form_version WHERE activity_id=?",key);
  }
  db.update("INSERT INTO activity_form_version(id,activity_id,version_no,schema_json) VALUES(?,?,?,?)",db.id(),key,version,db.json(Map.of("fields",b.fields)));
  db.audit(actor.id(),"STAFF","ACTIVITY_CONFIG","ACTIVITY",key,Map.of("version",version));return adminDetail(key);
 }
 public List<Map<String,Object>> list(){return db.rows("SELECT a.*,capacity-confirmed_count remaining FROM activity a WHERE tenant_id='000000' AND status='PUBLISHED' ORDER BY event_start,id LIMIT 200");}
 public Map<String,Object> detail(long id){var a=db.one("SELECT *,capacity-confirmed_count remaining FROM activity WHERE id=? AND tenant_id='000000' AND status='PUBLISHED'",id);a.put("form",db.one("SELECT id,version_no,schema_json FROM activity_form_version WHERE activity_id=? ORDER BY version_no DESC LIMIT 1",id));return a;}
 @Transactional public Map<String,Object> register(long user,long activity,Registration b){
  var a=db.one("SELECT * FROM activity WHERE id=? AND tenant_id='000000' FOR UPDATE",activity);
  var old=db.rows("SELECT * FROM activity_registration WHERE activity_id=? AND user_id=?",activity,user);if(!old.isEmpty()&&!old.get(0).get("status").equals("CANCELLED"))return old.get(0);
  BusinessException.require(db.count("SELECT COUNT(*) FROM activity WHERE id=? AND status='PUBLISHED' AND registration_start<=UTC_TIMESTAMP() AND registration_end>UTC_TIMESTAMP()",activity)==1,409,"当前不在报名窗口");
  var form=db.one("SELECT * FROM activity_form_version WHERE activity_id=? ORDER BY version_no DESC LIMIT 1",activity);BusinessException.require(Objects.equals(form.get("id"),Long.toString(b.formVersionId)),409,"报名表单已更新");answers(db.object(form.get("schemaJson").toString()).get("fields"),b.answers);
  String state;boolean available=Db.number(a.get("confirmedCount"))<Db.number(a.get("capacity"));
  if(available){state="CONFIRMED";db.update("UPDATE activity SET confirmed_count=confirmed_count+1,lock_version=lock_version+1 WHERE id=?",activity);}else{BusinessException.require(Db.number(a.get("waitlistEnabled"))==1,409,"名额已满");state="WAITLISTED";}
  long seq=db.count("SELECT COALESCE(MAX(queue_seq),0)+1 FROM activity_registration WHERE activity_id=?",activity),id=old.isEmpty()?db.id():Db.number(old.get(0).get("id"));
  if(old.isEmpty())db.update("INSERT INTO activity_registration(id,activity_id,user_id,form_version_id,answer_snapshot,status,queue_seq) VALUES(?,?,?,?,?,?,?)",id,activity,user,b.formVersionId,db.json(b.answers),state,seq);
  else db.update("UPDATE activity_registration SET form_version_id=?,answer_snapshot=?,status=?,queue_seq=?,submitted_at=UTC_TIMESTAMP(),cancelled_at=NULL,lock_version=lock_version+1 WHERE id=?",b.formVersionId,db.json(b.answers),state,seq,id);
  db.event("ACTIVITY",activity,"REGISTRATION_CREATED",Map.of("registrationId",Long.toString(id)));return db.one("SELECT * FROM activity_registration WHERE id=?",id);
 }
 @Transactional public void cancel(long user,long activity){
  db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000' FOR UPDATE",activity);var r=db.one("SELECT * FROM activity_registration WHERE activity_id=? AND user_id=? FOR UPDATE",activity,user);String state=r.get("status").toString();if(state.equals("CANCELLED"))return;
  db.update("UPDATE activity_registration SET status='CANCELLED',cancelled_at=UTC_TIMESTAMP(),lock_version=lock_version+1 WHERE id=?",r.get("id"));
  if(state.equals("CONFIRMED")){var queue=db.rows("SELECT id FROM activity_registration WHERE activity_id=? AND status='WAITLISTED' ORDER BY queue_seq,id LIMIT 1 FOR UPDATE",activity);if(queue.isEmpty())db.update("UPDATE activity SET confirmed_count=confirmed_count-1,lock_version=lock_version+1 WHERE id=?",activity);else{db.update("UPDATE activity_registration SET status='CONFIRMED',lock_version=lock_version+1 WHERE id=?",queue.get(0).get("id"));db.update("UPDATE activity SET lock_version=lock_version+1 WHERE id=?",activity);}}
  db.event("ACTIVITY",activity,"REGISTRATION_CANCELLED",Map.of("registrationId",r.get("id")));
 }
 public List<Map<String,Object>> mine(long user){return db.rows("SELECT r.*,a.title,a.event_start FROM activity_registration r JOIN activity a ON a.id=r.activity_id WHERE r.user_id=? ORDER BY r.submitted_at DESC,r.id DESC LIMIT 200",user);}
 public Map<String,Object> adminDetail(long id){var a=actors.staff();a.require("yunji:activity:manage");var row=db.one("SELECT * FROM activity WHERE id=? AND tenant_id='000000'",id);row.put("form",db.one("SELECT * FROM activity_form_version WHERE activity_id=? ORDER BY version_no DESC LIMIT 1",id));row.put("registrations",db.rows("SELECT * FROM activity_registration WHERE activity_id=? ORDER BY queue_seq,id LIMIT 500",id));return row;}
 public List<Map<String,Object>> adminList(){actors.staff().require("yunji:activity:manage");return db.rows("SELECT * FROM activity WHERE tenant_id='000000' ORDER BY event_start,id LIMIT 200");}
}
