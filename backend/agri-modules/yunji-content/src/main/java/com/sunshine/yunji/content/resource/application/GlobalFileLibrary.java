package com.sunshine.yunji.content.resource.application;

import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

/** Global files own immutable versions; activities only reference them. No package/plugin model. */
@Service
public class GlobalFileLibrary {
 private final Db db;private final Actors actors;private final FileService files;private final CloudFiles cloud;
 public GlobalFileLibrary(Db db,Actors actors,FileService files,CloudFiles cloud){this.db=db;this.actors=actors;this.files=files;this.cloud=cloud;}
 public List<Map<String,Object>> list(){actors.staff();return db.rows("SELECT f.*,(SELECT COUNT(*) FROM activity_file a WHERE a.file_id=f.id) activity_count,(SELECT COUNT(*) FROM activity_download d WHERE d.file_id=f.id) download_count FROM global_file f WHERE deleted_at IS NULL ORDER BY created_at DESC,id DESC LIMIT 500");}
 public Map<String,Object> detail(long id){actors.staff();var f=db.one("SELECT * FROM global_file WHERE id=? AND deleted_at IS NULL",id);f.put("versions",versions(id));f.put("activities",db.rows("SELECT a.id,a.title FROM activity_file r JOIN activity a ON a.id=r.activity_id WHERE r.file_id=? ORDER BY a.id",id));return f;}
 private List<Map<String,Object>> versions(long id){return db.rows("SELECT v.version_no,v.note,v.created_at,o.display_name,o.size_bytes,o.media_type,o.sha256 FROM global_file_version v JOIN file_object o ON o.id=v.object_id WHERE v.file_id=? ORDER BY v.version_no DESC",id);}
 @Transactional public Map<String,Object> upload(Long id,MultipartFile file,String name,String description,String note,int expected){var a=actors.staff();Db.text(name,150);BusinessException.require(description!=null&&description.length()<=2000&&note!=null&&note.length()<=500,422,"说明或版本备注过长");long key=id==null?db.id():id;int version=1;
  if(id==null)db.update("INSERT INTO global_file(id,name,description,created_by) VALUES(?,?,?,?)",key,name,description,a.id());
  else{var old=db.one("SELECT * FROM global_file WHERE id=? AND deleted_at IS NULL FOR UPDATE",key);BusinessException.require(Db.number(old.get("lockVersion"))==expected,409,"文件已变化");version=(int)Db.number(old.get("currentVersion"))+1;}
  var object=files.upload(files.staff(),file,"PRIVATE");String cloudId=null;if(!cloud.local())try{cloudId=cloud.upload(file.getBytes(),Objects.toString(file.getOriginalFilename(),"file"));}catch(java.io.IOException e){throw new BusinessException(422,"读取文件失败");}
  db.update("INSERT INTO global_file_version(file_id,version_no,object_id,provider,cloud_file_id,note) VALUES(?,?,?,?,?,?)",key,version,object.get("id"),cloud.local()?"LOCAL":"WECHAT_CLOUD",cloudId,note);
  db.update("INSERT IGNORE INTO file_reference(file_id,owner_type,owner_id,reference_version_id) VALUES(?,'GLOBAL_LIBRARY',?,?)",object.get("id"),key,version);
  db.update("UPDATE global_file SET name=?,description=?,current_version=?,lock_version=lock_version+1 WHERE id=?",name,description,version,key);db.audit(a.id(),"STAFF","LIBRARY_VERSION_UPLOAD","GLOBAL_FILE",key,Map.of("version",version));return detail(key);
 }
 public record Edit(String name,String description,boolean enabled,int expectedVersion){}
 @Transactional public void edit(long id,Edit b){var a=actors.staff();Db.text(b.name(),150);BusinessException.require(b.description()!=null&&b.description().length()<=2000,422,"文件说明过长");BusinessException.require(db.update("UPDATE global_file SET name=?,description=?,enabled=?,lock_version=lock_version+1 WHERE id=? AND deleted_at IS NULL AND lock_version=?",b.name(),b.description(),b.enabled(),id,b.expectedVersion())==1,409,"文件已变化");db.audit(a.id(),"STAFF","LIBRARY_EDIT","GLOBAL_FILE",id,Map.of("enabled",b.enabled()));}
 @Transactional public void delete(long id,int expected){var a=actors.staff();db.one("SELECT id FROM global_file WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);BusinessException.require(db.count("SELECT COUNT(*) FROM activity_file WHERE file_id=?",id)==0,409,"文件仍被活动引用，请先解除引用");BusinessException.require(db.update("UPDATE global_file SET deleted_at=UTC_TIMESTAMP(),enabled=0,lock_version=lock_version+1 WHERE id=? AND lock_version=?",id,expected)==1,409,"文件已变化");db.audit(a.id(),"STAFF","LIBRARY_DELETE","GLOBAL_FILE",id,Map.of());}
 public record FileReference(long fileId,boolean showHistory,int sortOrder){}
 public record References(List<FileReference> files,List<Long> reviewIds,int expectedVersion){}
 @Transactional public void references(long activity,References b){var a=actors.staff();db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000' FOR UPDATE",activity);BusinessException.require(b.files()!=null&&b.files().size()<=100&&b.reviewIds()!=null&&b.reviewIds().size()<=30,422,"活动引用数量错误");BusinessException.require(new HashSet<>(b.files().stream().map(FileReference::fileId).toList()).size()==b.files().size()&&new HashSet<>(b.reviewIds()).size()==b.reviewIds().size(),422,"活动引用重复");
  BusinessException.require(db.update("UPDATE activity SET lock_version=lock_version+1 WHERE id=? AND lock_version=?",activity,b.expectedVersion())==1,409,"活动已变化");
  for(var f:b.files())db.one("SELECT id FROM global_file WHERE id=? AND enabled=1 AND deleted_at IS NULL FOR UPDATE",f.fileId());for(long review:b.reviewIds())db.one("SELECT id FROM review_campaign WHERE id=? AND tenant_id='000000'",review);
  db.update("DELETE FROM activity_file WHERE activity_id=?",activity);db.update("DELETE FROM activity_review WHERE activity_id=?",activity);
  for(var f:b.files())db.update("INSERT INTO activity_file(activity_id,file_id,show_history,sort_order) VALUES(?,?,?,?)",activity,f.fileId(),f.showHistory(),f.sortOrder());for(long review:b.reviewIds())db.update("INSERT INTO activity_review VALUES(?,?)",activity,review);db.audit(a.id(),"STAFF","ACTIVITY_FUNCTIONS_SAVE","ACTIVITY",activity,b);
 }
 public Map<String,Object> references(long activity){actors.staff();db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000'",activity);return Map.of("files",db.rows("SELECT * FROM activity_file WHERE activity_id=? ORDER BY sort_order,file_id",activity),"reviewIds",db.rows("SELECT campaign_id id FROM activity_review WHERE activity_id=?",activity).stream().map(x->x.get("id")).toList());}
 private void activity(long activity){db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000' AND status='PUBLISHED'",activity);}
 public List<Map<String,Object>> available(long activity){activity(activity);return db.rows("SELECT f.id,f.name,f.description,f.current_version,r.show_history,(SELECT COUNT(*) FROM activity_download d WHERE d.activity_id=r.activity_id AND d.file_id=f.id) download_count FROM activity_file r JOIN global_file f ON f.id=r.file_id WHERE r.activity_id=? AND f.enabled=1 AND f.deleted_at IS NULL ORDER BY r.sort_order,f.id",activity);}
 public List<Map<String,Object>> history(long activity,long file){activity(activity);db.one("SELECT f.id FROM global_file f JOIN activity_file r ON r.file_id=f.id WHERE r.activity_id=? AND f.id=? AND r.show_history=1 AND f.enabled=1 AND f.deleted_at IS NULL",activity,file);return versions(file);}
 @Transactional public void completed(long user,String ticket){files.libraryCompleted(user,ticket);}
 @Transactional public Map<String,Object> download(long user,long activity,long file,Integer version){activity(activity);var f=db.one("SELECT f.*,r.show_history FROM global_file f JOIN activity_file r ON r.file_id=f.id WHERE r.activity_id=? AND f.id=? AND f.enabled=1 AND f.deleted_at IS NULL FOR SHARE",activity,file);int v=version==null?(int)Db.number(f.get("currentVersion")):version;BusinessException.require(v==Db.number(f.get("currentVersion"))||Db.number(f.get("showHistory"))==1,404,"历史版本未开放");var object=db.one("SELECT * FROM global_file_version WHERE file_id=? AND version_no=?",file,v);Map<String,Object> link;
  if(object.get("provider").equals("LOCAL")){BusinessException.require(cloud.local(),503,"生产环境禁止读取本地测试文件");link=files.libraryTicket(user,Db.number(object.get("objectId")),activity,file,v);}else {link=new HashMap<>(files.libraryTicket(user,Db.number(object.get("objectId")),activity,file,v));link.put("url",cloud.download(object.get("cloudFileId").toString()));}
  return link;
 }
}
