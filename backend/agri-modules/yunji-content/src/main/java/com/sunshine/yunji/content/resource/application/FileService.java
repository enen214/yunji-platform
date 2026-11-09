package com.sunshine.yunji.content.resource.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.io.IOException;

@Service
public class FileService {
 public record Principal(String realm,long id){}
 public record Download(byte[] bytes,String name,String mediaType){}
 private final Db db;private final Actors actors;private final Path root;
 public FileService(Db db,Actors actors,@Value("${yunji.storage-root}") String root){this.db=db;this.actors=actors;this.root=Path.of(root).toAbsolutePath().normalize();}
 public Principal staff(){var a=actors.staff();a.require("yunji:file:manage");return new Principal("STAFF",a.id());}
 private String mime(byte[] bytes,String name){
  String ext=name.toLowerCase(Locale.ROOT);if(ext.endsWith(".pdf")&&bytes.length>=5&&new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";
  if(ext.endsWith(".png")&&bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
  if((ext.endsWith(".jpg")||ext.endsWith(".jpeg"))&&bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)return "image/jpeg";
  if(ext.endsWith(".zip")&&bytes.length>=4&&bytes[0]==80&&bytes[1]==75&&bytes[2]==3&&bytes[3]==4)return "application/zip";
  if(ext.endsWith(".txt")){try{java.nio.charset.StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes));}catch(Exception e){throw new BusinessException(422,"文本必须为UTF-8");}for(byte b:bytes)BusinessException.require(b!=0,422,"文本含二进制数据");return "text/plain";}
  throw new BusinessException(422,"文件扩展名或实际类型不受支持");
 }
 @Transactional public Map<String,Object> upload(Principal p,MultipartFile file,String privacy){
  BusinessException.require(file!=null&&!file.isEmpty()&&file.getSize()<=10485760,422,"文件为空或超过10MB");BusinessException.require(Set.of("PUBLIC","PRIVATE").contains(privacy),422,"文件隐私类型错误");
  String name=Db.text(Objects.toString(file.getOriginalFilename(),""),150);BusinessException.require(!name.contains("/")&&!name.contains("\\")&&!name.contains("\r")&&!name.contains("\n"),422,"文件名非法");
  try{byte[] bytes=file.getBytes();String type=mime(bytes,name),sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));var old=db.rows("SELECT id,sha256,size_bytes,media_type,display_name,privacy,status FROM file_object WHERE owner_realm=? AND owner_id=? AND sha256=? AND privacy=? AND status='VERIFIED'",p.realm,p.id,sha,privacy);if(!old.isEmpty())return old.get(0);
   long id=db.id();String key=db.token();Path destination=root.resolve(key).normalize();BusinessException.require(destination.startsWith(root),422,"文件路径非法");Files.createDirectories(root);Files.write(destination,bytes,StandardOpenOption.CREATE_NEW);
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(destination);}catch(IOException ignored){}}});
   db.update("INSERT INTO file_object(id,owner_realm,owner_id,object_key,sha256,size_bytes,media_type,display_name,privacy) VALUES(?,?,?,?,?,?,?,?,?)",id,p.realm,p.id,key,sha,bytes.length,type,name,privacy);
   db.audit(p.id,p.realm,"FILE_UPLOAD","FILE",id,Map.of("size",bytes.length,"sha256",sha));return db.one("SELECT id,sha256,size_bytes,media_type,display_name,privacy,status FROM file_object WHERE id=?",id);
  }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(422,"文件存储失败");}
 }
 public Map<String,Object> authorize(Principal p,long id){
  var f=db.one("SELECT * FROM file_object WHERE id=? AND tenant_id='000000' AND status='VERIFIED'",id);
  if(p.realm.equals(f.get("ownerRealm"))&&Long.toString(p.id).equals(f.get("ownerId")))return f;
  if(p.realm.equals("STAFF")&&actors.staff().admin())return f;
  if(p.realm.equals("CUSTOMER")&&db.count("SELECT COUNT(*) FROM resource_file f JOIN resource_package r ON r.id=f.package_id AND r.current_version_no=f.version_no WHERE f.file_id=? AND r.status='PUBLISHED' AND r.tenant_id='000000'",id)>0)return f;
  if(p.realm.equals("CUSTOMER")&&db.count("SELECT COUNT(*) FROM artifact_version v JOIN artifact a ON a.id=v.artifact_id WHERE v.file_id=? AND v.status='PUBLISHED' AND a.status='PUBLISHED' AND a.tenant_id='000000'",id)>0)return f;
  if(p.realm.equals("STAFF")){
   actors.staff().require("yunji:review:decide");long n=db.count("SELECT COUNT(*) FROM review_submission_file sf JOIN review_submission s ON s.id=sf.submission_id JOIN review_application a ON a.id=s.application_id JOIN review_campaign c ON c.id=a.campaign_id JOIN reviewer_scope scope ON scope.organization_id=c.organization_id WHERE sf.file_id=? AND s.submitted_at IS NOT NULL AND c.tenant_id='000000' AND scope.user_id=?",id,p.id);
   if(n>0)return f;
  }throw new BusinessException(404,"文件不存在或不可访问");
 }
 @Transactional public Map<String,Object> ticket(Principal p,long id){authorize(p,id);String token=db.token();String hash=Db.hash(token);db.update("INSERT INTO download_ticket(token_hash,file_id,principal_realm,principal_id,expires_at) VALUES(?,?,?,?,UTC_TIMESTAMP()+INTERVAL 2 MINUTE)",hash,id,p.realm,p.id);db.update("INSERT INTO download_event(id,token_hash,event_type) VALUES(?,?,'ISSUED')",db.id(),hash);return Map.of("ticket",token,"expiresIn",120,"path","/api/v1/files/download/"+token);}
 @Transactional public Map<String,Object> libraryTicket(long user,long object,long activity,long file,int version){String token=db.token(),hash=Db.hash(token);db.update("INSERT INTO download_ticket(token_hash,file_id,principal_realm,principal_id,expires_at,library_activity_id,library_file_id,library_version) VALUES(?,?,'CUSTOMER',?,UTC_TIMESTAMP()+INTERVAL 2 MINUTE,?,?,?)",hash,object,user,activity,file,version);db.update("INSERT INTO download_event(id,token_hash,event_type) VALUES(?,?,'ISSUED')",db.id(),hash);return Map.of("ticket",token,"expiresIn",120,"path","/api/v1/files/download/"+token);}
 @Transactional public void libraryDownloaded(long user,long activity,long file,int version){db.update("INSERT IGNORE INTO activity_download(user_id,file_id,activity_id,day_key,version_no) VALUES(?,?,?,DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR),?)",user,file,activity,version);}
 /** Receipt is client-reported, not proof of saving/opening a file. Never count link issuance. */
 @Transactional public void libraryCompleted(long user,String token){
  String hash=Db.hash(Db.text(token,100));
  var t=db.one("SELECT * FROM download_ticket WHERE token_hash=? AND principal_realm='CUSTOMER' AND principal_id=? AND library_file_id IS NOT NULL AND expires_at+INTERVAL 10 MINUTE>UTC_TIMESTAMP() FOR UPDATE",hash,user);
  ticketObject(new Principal("CUSTOMER",user),t);
  var version=db.one("SELECT provider FROM global_file_version WHERE file_id=? AND version_no=?",t.get("libraryFileId"),t.get("libraryVersion"));
  if("LOCAL".equals(version.get("provider")))BusinessException.require(db.count("SELECT COUNT(*) FROM download_event WHERE token_hash=? AND event_type='SERVER_READ'",hash)>0,409,"尚未成功读取文件，不能确认下载");
  if(db.count("SELECT COUNT(*) FROM download_event WHERE token_hash=? AND event_type='CLIENT_CONFIRMED'",hash)==0){
   db.update("UPDATE download_ticket SET status='CONSUMED' WHERE token_hash=?",hash);
   db.update("INSERT INTO download_event(id,token_hash,event_type) VALUES(?,?,'CLIENT_CONFIRMED')",db.id(),hash);
   libraryDownloaded(user,Db.number(t.get("libraryActivityId")),Db.number(t.get("libraryFileId")),(int)Db.number(t.get("libraryVersion")));
  }
 }
 private Map<String,Object> ticketObject(Principal p,Map<String,Object> t){if(t.get("libraryFileId")==null)return authorize(p,Db.number(t.get("fileId")));BusinessException.require(p.realm().equals("CUSTOMER"),404,"下载主体错误");return db.one("SELECT o.* FROM global_file f JOIN global_file_version v ON v.file_id=f.id JOIN activity_file r ON r.file_id=f.id JOIN activity a ON a.id=r.activity_id JOIN file_object o ON o.id=v.object_id WHERE f.id=? AND a.id=? AND v.version_no=? AND o.id=? AND o.status='VERIFIED' AND f.enabled=1 AND f.deleted_at IS NULL AND a.status='PUBLISHED' AND a.tenant_id='000000' AND (v.version_no=f.current_version OR r.show_history=1)",t.get("libraryFileId"),t.get("libraryActivityId"),t.get("libraryVersion"),t.get("fileId"));}
 @Transactional public Download download(Principal p,String token){String hash=Db.hash(Db.text(token,100));var t=db.one("SELECT * FROM download_ticket WHERE token_hash=? AND principal_realm=? AND principal_id=? AND status='ISSUED' AND expires_at>UTC_TIMESTAMP() FOR UPDATE",hash,p.realm,p.id);var file=ticketObject(p,t);Path path=root.resolve(file.get("objectKey").toString()).normalize();BusinessException.require(path.startsWith(root)&&Files.isRegularFile(path)&&!Files.isSymbolicLink(path),404,"文件不可用");try{byte[] bytes=Files.readAllBytes(path);BusinessException.require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(file.get("sha256")),409,"文件校验和不匹配");db.update("UPDATE download_ticket SET status='CONSUMED' WHERE token_hash=?",hash);db.update("INSERT INTO download_event(id,token_hash,event_type) VALUES(?,?,'SERVER_READ')",db.id(),hash);return new Download(bytes,file.get("displayName").toString(),file.get("mediaType").toString());}catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(503,"文件暂时不可读");}}
 public List<Map<String,Object>> list(){actors.staff().require("yunji:file:manage");return db.rows("SELECT id,owner_realm,owner_id,sha256,size_bytes,media_type,display_name,privacy,status,created_at FROM file_object WHERE tenant_id='000000' ORDER BY created_at DESC,id DESC LIMIT 200");}
 @Transactional public Map<String,Object> cleanup(boolean dryRun,int limit){var a=actors.staff();a.require("yunji:file:manage");return cleanupObjects(dryRun,limit,a.id(),"STAFF");}
 @Transactional public Map<String,Object> scheduledCleanup(int limit){return cleanupObjects(false,limit,0,"SYSTEM");}
 private Map<String,Object> cleanupObjects(boolean dryRun,int limit,long actor,String realm){
  BusinessException.require(limit>=1&&limit<=500,422,"回收数量限制错误");
  var candidates=db.rows("SELECT f.* FROM file_object f WHERE f.tenant_id='000000' AND f.status='DELETED' AND f.deleted_at<=UTC_TIMESTAMP()-INTERVAL 7 DAY AND NOT EXISTS(SELECT 1 FROM file_reference r WHERE r.file_id=f.id) ORDER BY f.deleted_at,f.id LIMIT "+limit+" FOR UPDATE SKIP LOCKED");
  int removed=0,missing=0,failed=0,purged=0;List<String> failures=new ArrayList<>();
  if(!dryRun)for(var f:candidates){
   try{
    String key=f.get("objectKey").toString();BusinessException.require(key.matches("[A-Za-z0-9_-]{43}"),422,"对象标识错误");
    Path path=root.resolve(key).normalize();BusinessException.require(path.startsWith(root)&&!Files.isSymbolicLink(root)&&!Files.isSymbolicLink(path),422,"文件路径非法");
    BusinessException.require(db.count("SELECT COUNT(*) FROM file_reference WHERE file_id=?",f.get("id"))==0,409,"文件新增了引用");
    boolean existed=Files.deleteIfExists(path);if(existed)removed++;else missing++;
    db.update("UPDATE file_object SET status='PURGED',purged_at=UTC_TIMESTAMP() WHERE id=? AND status='DELETED'",f.get("id"));purged++;
    db.audit(actor,realm,"FILE_PHYSICAL_DELETE","FILE",Db.number(f.get("id")),Map.of("sha256",f.get("sha256"),"alreadyMissing",!existed));
   }catch(Exception e){failed++;failures.add(f.get("id").toString());}
  }
  return Map.of("dryRun",dryRun,"retentionDays",7,"candidates",candidates.size(),"removed",removed,"alreadyMissing",missing,"purged",purged,"failed",failed,"failedIds",failures);
 }
 @Transactional public void delete(long id){var a=actors.staff();a.require("yunji:file:manage");db.one("SELECT id FROM file_object WHERE id=? AND tenant_id='000000' FOR UPDATE",id);authorize(new Principal("STAFF",a.id()),id);BusinessException.require(db.count("SELECT COUNT(*) FROM file_reference WHERE file_id=?",id)==0,409,"文件仍被引用");db.update("UPDATE file_object SET status='DELETED',deleted_at=UTC_TIMESTAMP() WHERE id=?",id);db.audit(a.id(),"STAFF","FILE_DELETE","FILE",id,Map.of());}
}
