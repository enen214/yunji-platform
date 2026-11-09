package com.sunshine.yunji.content.review.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;

@Service
public class ReviewService {
 private final Db db;private final Actors actors;
 public ReviewService(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Requirement(String key,String label,boolean required,List<String> mediaTypes,long maxSize,Long organizationId){}
 public record Campaign(@NotBlank @Size(max=255) String title,Long organizationId,Long articleId,@NotNull Instant openAt,@NotNull Instant closeAt,@NotBlank String status,List<Requirement> requirements){}
 public record Materials(Map<String,Long> files,int expectedVersion){}
 public record Action(@NotBlank String action,long submissionId,int expectedVersion,@Size(max=1000) String comment){}
 public record CampaignUpdate(@NotBlank @Size(max=255) String title,Long organizationId,Long articleId,@NotNull Instant openAt,@NotNull Instant closeAt,@NotBlank String status,List<Requirement> requirements,int expectedVersion){}
 public Map<String,Object> adminCampaign(long id){actors.staff().require("yunji:review:manage");return campaign(id,false);}
 @Transactional public Map<String,Object> updateCampaign(long id,CampaignUpdate b){
  var actor=actors.staff();actor.require("yunji:review:manage");
  var old=db.one("SELECT * FROM review_campaign WHERE id=? AND tenant_id='000000' FOR UPDATE",id);
  BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion,409,"评审项目已变化");
  BusinessException.require(b.closeAt.isAfter(b.openAt)&&Set.of("DRAFT","PUBLISHED","CLOSED").contains(b.status),422,"项目时间或状态错误");
  BusinessException.require(b.organizationId==null,422,"本期不支持评审组织归属");
  if(b.articleId!=null)db.one("SELECT id FROM article WHERE id=? AND tenant_id='000000'",b.articleId);
  BusinessException.require(b.requirements!=null&&!b.requirements.isEmpty()&&b.requirements.size()<=20,422,"材料要求数量错误");
  Set<String> keys=new HashSet<>();for(var r:b.requirements){BusinessException.require(r.key!=null&&r.key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")&&keys.add(r.key)&&r.maxSize>0&&r.maxSize<=10485760&&r.mediaTypes!=null&&!r.mediaTypes.isEmpty()&&Set.of("application/pdf","image/png","image/jpeg","text/plain").containsAll(r.mediaTypes),422,"材料要求配置错误");Db.text(r.label,150);BusinessException.require(r.organizationId==null,422,"本期不支持组织资格条件");}
  var current=requirements(id,Db.number(old.get("ruleVersion")));
  List<Requirement> previous=current.stream().map(r->{var eligibility=db.object(r.get("eligibilityJson").toString());return new Requirement(r.get("requirementKey").toString(),r.get("label").toString(),Db.number(r.get("required"))!=0,db.object("{\"types\":"+r.get("fileTypeAllowlist")+"}").get("types") instanceof List<?> types?types.stream().map(Object::toString).toList():List.of(),Db.number(r.get("maxSize")),eligibility.containsKey("organizationId")?Db.number(eligibility.get("organizationId")):null);}).toList();
  boolean requirementsChanged=!canonical(previous).equals(canonical(b.requirements));
  boolean organizationChanged=!Objects.equals(old.get("organizationId"),b.organizationId==null?null:Long.toString(b.organizationId));
  if(requirementsChanged||organizationChanged)BusinessException.require(db.count("SELECT COUNT(*) FROM review_application WHERE campaign_id=?",id)==0,409,"已有申请时不能改变组织或材料要求；新建评审项目保留旧申请快照");
  long version=Db.number(old.get("ruleVersion"));
  if(requirementsChanged){version++;for(var r:b.requirements)db.update("INSERT INTO review_requirement(campaign_id,rule_version,requirement_key,label,required,file_type_allowlist,max_size,eligibility_json) VALUES(?,?,?,?,?,?,?,?)",id,version,r.key,r.label,r.required,db.json(r.mediaTypes),r.maxSize,db.json(r.organizationId==null?Map.of():Map.of("organizationId",Long.toString(r.organizationId))));}
  db.update("UPDATE review_campaign SET title=?,organization_id=?,article_id=?,open_at=?,close_at=?,status=?,rule_version=?,lock_version=lock_version+1 WHERE id=?",b.title,b.organizationId,b.articleId,Timestamp.from(b.openAt),Timestamp.from(b.closeAt),b.status,version,id);
  db.audit(actor.id(),"STAFF","CAMPAIGN_UPDATE","REVIEW_CAMPAIGN",id,Map.of("ruleVersion",version,"status",b.status));return campaign(id,false);
 }
 private List<Requirement> canonical(List<Requirement> rules){return rules.stream().map(r->new Requirement(r.key,r.label,r.required,r.mediaTypes.stream().distinct().sorted().toList(),r.maxSize,r.organizationId)).sorted(Comparator.comparing(Requirement::key)).toList();}
 @Transactional public Map<String,Object> create(Campaign b){
  var a=actors.staff();a.require("yunji:review:manage");BusinessException.require(b.organizationId==null,422,"本期不支持评审组织归属");BusinessException.require(b.closeAt.isAfter(b.openAt)&&Set.of("DRAFT","PUBLISHED","CLOSED").contains(b.status),422,"项目时间或状态错误");BusinessException.require(b.requirements!=null&&!b.requirements.isEmpty()&&b.requirements.size()<=20,422,"材料要求数量错误");
  Set<String> keys=new HashSet<>();for(var r:b.requirements){BusinessException.require(r.key!=null&&r.key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")&&keys.add(r.key)&&r.maxSize>0&&r.maxSize<=10485760&&r.mediaTypes!=null&&!r.mediaTypes.isEmpty()&&Set.of("application/pdf","image/png","image/jpeg","text/plain").containsAll(r.mediaTypes),422,"材料要求配置错误");Db.text(r.label,150);BusinessException.require(r.organizationId==null,422,"本期不支持组织资格条件");}
  long id=db.id();db.update("INSERT INTO review_campaign(id,title,organization_id,article_id,open_at,close_at,status,created_by) VALUES(?,?,?,?,?,?,?,?)",id,b.title,b.organizationId,b.articleId,Timestamp.from(b.openAt),Timestamp.from(b.closeAt),b.status,a.id());
  for(var r:b.requirements)db.update("INSERT INTO review_requirement(campaign_id,rule_version,requirement_key,label,required,file_type_allowlist,max_size,eligibility_json) VALUES(?,1,?,?,?,?,?,?)",id,r.key,r.label,r.required,db.json(r.mediaTypes),r.maxSize,db.json(r.organizationId==null?Map.of():Map.of("organizationId",Long.toString(r.organizationId))));
  db.audit(a.id(),"STAFF","CAMPAIGN_CREATE","REVIEW_CAMPAIGN",id,Map.of("title",b.title));return campaign(id,false);
 }
 public List<Map<String,Object>> campaigns(){return db.rows("SELECT * FROM review_campaign WHERE tenant_id='000000' AND status='PUBLISHED' ORDER BY close_at,id LIMIT 200");}
 public Map<String,Object> campaign(long id,boolean publicOnly){var row=db.one("SELECT * FROM review_campaign WHERE id=? AND tenant_id='000000'"+(publicOnly?" AND status='PUBLISHED'":""),id);row.put("requirements",requirements(id,Db.number(row.get("ruleVersion"))));return row;}
 private List<Map<String,Object>> requirements(long id,long version){return db.rows("SELECT * FROM review_requirement WHERE campaign_id=? AND rule_version=? ORDER BY requirement_key",id,version);}
 private void window(long campaign){BusinessException.require(db.count("SELECT COUNT(*) FROM review_campaign WHERE id=? AND tenant_id='000000' AND status='PUBLISHED' AND open_at<=UTC_TIMESTAMP() AND close_at>UTC_TIMESTAMP()",campaign)==1,409,"当前不在申报窗口");}
 public Map<String,Object> eligibility(long user,long campaign){campaign(campaign,true);db.one("SELECT id FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user);return Map.of("eligible",true,"failedRequirementKeys",List.of());}
 @Transactional public Map<String,Object> draft(long user,long campaign,Materials b){
  db.one("SELECT id FROM review_campaign WHERE id=? AND tenant_id='000000' FOR UPDATE",campaign);window(campaign);var c=campaign(campaign,true);db.one("SELECT id FROM app_user WHERE id=? FOR UPDATE",user);
  var old=db.rows("SELECT * FROM review_application WHERE campaign_id=? AND user_id=? FOR UPDATE",campaign,user);long app;
  if(old.isEmpty()){BusinessException.require(b.expectedVersion==0,409,"申请版本冲突");app=db.id();db.update("INSERT INTO review_application(id,campaign_id,user_id) VALUES(?,?,?)",app,campaign,user);}
  else{var a=old.get(0);BusinessException.require(Set.of("DRAFT","CORRECTION","WITHDRAWN").contains(a.get("status"))&&Db.number(a.get("lockVersion"))==b.expectedVersion,409,"申请状态或版本不允许编辑");app=Db.number(a.get("id"));}
  List<Map<String,Object>> req=requirements(campaign,Db.number(c.get("ruleVersion")));BusinessException.require(b.files!=null&&b.files.size()<=20,422,"材料列表错误");Set<String> keys=new HashSet<>();for(var r:req)keys.add(r.get("requirementKey").toString());BusinessException.require(keys.containsAll(b.files.keySet()),422,"含未定义材料要求");
  for(var f:b.files.entrySet()){var file=db.one("SELECT * FROM file_object WHERE id=? AND owner_realm='CUSTOMER' AND owner_id=? AND privacy='PRIVATE' AND status='VERIFIED' AND tenant_id='000000' FOR UPDATE",f.getValue(),user);var rule=req.stream().filter(r->r.get("requirementKey").equals(f.getKey())).findFirst().orElseThrow();BusinessException.require(Db.number(file.get("sizeBytes"))<=Db.number(rule.get("maxSize"))&&rule.get("fileTypeAllowlist").toString().contains("\""+file.get("mediaType")+"\""),422,"材料类型或大小不符合要求");}
  long version=db.id(),n=db.count("SELECT COALESCE(MAX(version_no),0)+1 FROM review_submission WHERE application_id=?",app);db.update("INSERT INTO review_submission(id,application_id,version_no,requirement_snapshot,eligibility_snapshot) VALUES(?,?,?,?,?)",version,app,n,db.json(Map.of("requirements",req)),db.json(eligibility(user,campaign)));
  for(var f:b.files.entrySet()){db.update("INSERT INTO review_submission_file(submission_id,requirement_key,file_id) VALUES(?,?,?)",version,f.getKey(),f.getValue());db.update("INSERT INTO file_reference(file_id,owner_type,owner_id,reference_version_id) VALUES(?,'REVIEW',?,?)",f.getValue(),app,version);}
  db.update("UPDATE review_application SET status='DRAFT',latest_submission_id=?,lock_version=lock_version+1 WHERE id=?",version,app);db.audit(user,"CUSTOMER","APPLICATION_DRAFT","REVIEW_APPLICATION",app,Map.of("submissionId",Long.toString(version)));return mineDetail(user,app);
 }
 public List<Map<String,Object>> mine(long user){return db.rows("SELECT a.*,c.title FROM review_application a JOIN review_campaign c ON c.id=a.campaign_id WHERE a.user_id=? ORDER BY a.id DESC LIMIT 100",user);}
 public Map<String,Object> mineDetail(long user,long app){var a=db.one("SELECT * FROM review_application WHERE id=? AND user_id=?",app,user);return details(a);}
 private Map<String,Object> details(Map<String,Object> a){a.put("submissions",db.rows("SELECT * FROM review_submission WHERE application_id=? ORDER BY version_no DESC",a.get("id")));a.put("files",db.rows("SELECT sf.* FROM review_submission_file sf JOIN review_submission s ON s.id=sf.submission_id WHERE s.application_id=? ORDER BY s.version_no,sf.requirement_key",a.get("id")));a.put("decisions",db.rows("SELECT d.* FROM review_decision d JOIN review_submission s ON s.id=d.submission_id WHERE d.application_id=? ORDER BY s.version_no,d.decided_at,d.id",a.get("id")));return a;}
 @Transactional public Map<String,Object> action(long user,long app,Action b){
  var a=db.one("SELECT * FROM review_application WHERE id=? AND user_id=? FOR UPDATE",app,user);BusinessException.require(Db.number(a.get("lockVersion"))==b.expectedVersion,409,"申请已变化");long campaign=Db.number(a.get("campaignId"));window(campaign);
  if(b.action.equals("SUBMIT")){
   BusinessException.require(Set.of("DRAFT","WITHDRAWN").contains(a.get("status"))&&Objects.equals(a.get("latestSubmissionId"),Long.toString(b.submissionId)),409,"只能提交最新草稿");
   var eligibility=eligibility(user,campaign);BusinessException.require(Boolean.TRUE.equals(eligibility.get("eligible")),422,"不符合申报资格");var c=campaign(campaign,true);
   for(var req:requirements(campaign,Db.number(c.get("ruleVersion"))))if(Db.number(req.get("required"))==1)BusinessException.require(db.count("SELECT COUNT(*) FROM review_submission_file sf JOIN file_object f ON f.id=sf.file_id WHERE sf.submission_id=? AND sf.requirement_key=? AND f.owner_id=? AND f.owner_realm='CUSTOMER' AND f.status='VERIFIED' AND f.privacy='PRIVATE'",b.submissionId,req.get("requirementKey"),user)==1,422,"缺少必需的已验证材料");
   BusinessException.require(db.count("SELECT COUNT(*) FROM review_submission WHERE id=? AND application_id=? AND submitted_at IS NULL",b.submissionId,app)==1,409,"提交版本已冻结，请建立新版本");
   db.update("UPDATE review_submission SET submitted_at=UTC_TIMESTAMP() WHERE id=?",b.submissionId);db.update("UPDATE review_application SET status='SUBMITTED',current_submission_id=?,lock_version=lock_version+1 WHERE id=?",b.submissionId,app);
  }else if(b.action.equals("WITHDRAW")){BusinessException.require(a.get("status").equals("SUBMITTED")&&Objects.equals(a.get("currentSubmissionId"),Long.toString(b.submissionId)),409,"只有当前提交可撤回");db.update("UPDATE review_application SET status='WITHDRAWN',lock_version=lock_version+1 WHERE id=?",app);}else throw new BusinessException(422,"申报操作未知");
  db.event("REVIEW_APPLICATION",app,"APPLICATION_"+b.action,Map.of("submissionId",Long.toString(b.submissionId)));return mineDetail(user,app);
 }
 private void scope(Actors.Staff a){a.require("yunji:review:decide");}
 public List<Map<String,Object>> queue(){scope(actors.staff());return db.rows("SELECT a.*,c.title,c.organization_id FROM review_application a JOIN review_campaign c ON c.id=a.campaign_id WHERE c.tenant_id='000000' ORDER BY a.id DESC LIMIT 200");}
 public Map<String,Object> staffDetail(long id){var a=actors.staff();var row=db.one("SELECT a.*,c.organization_id FROM review_application a JOIN review_campaign c ON c.id=a.campaign_id WHERE a.id=? AND c.tenant_id='000000'",id);scope(a);return details(row);}
 @Transactional public Map<String,Object> decide(long id,Action b){var a=actors.staff();var row=db.one("SELECT a.*,c.organization_id FROM review_application a JOIN review_campaign c ON c.id=a.campaign_id WHERE a.id=? AND c.tenant_id='000000' FOR UPDATE",id);scope(a);BusinessException.require(row.get("status").equals("SUBMITTED")&&Objects.equals(row.get("currentSubmissionId"),Long.toString(b.submissionId))&&Db.number(row.get("lockVersion"))==b.expectedVersion,409,"申报版本已变化");BusinessException.require(Set.of("APPROVED","REJECTED","CORRECTION").contains(b.action),422,"评审决定错误");Db.text(b.comment,1000);db.update("INSERT INTO review_decision(id,application_id,submission_id,decision,comment,reviewer_id) VALUES(?,?,?,?,?,?)",db.id(),id,b.submissionId,b.action,b.comment,a.id());db.update("UPDATE review_application SET status=?,lock_version=lock_version+1 WHERE id=?",b.action,id);db.audit(a.id(),"STAFF","APPLICATION_DECIDE","REVIEW_APPLICATION",id,Map.of("submissionId",Long.toString(b.submissionId),"decision",b.action));return staffDetail(id);}
 public List<Map<String,Object>> adminCampaigns(){actors.staff().require("yunji:review:manage");return db.rows("SELECT * FROM review_campaign WHERE tenant_id='000000' ORDER BY id DESC LIMIT 100");}
}
