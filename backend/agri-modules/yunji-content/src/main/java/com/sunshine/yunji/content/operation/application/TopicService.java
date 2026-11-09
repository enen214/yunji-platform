package com.sunshine.yunji.content.operation.application;

import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.util.*;

@Service
public class TopicService {
 private final Db db;private final Actors actors;
 public TopicService(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Draft(@NotBlank @Size(max=150) String title,@Size(max=300) String subtitle,
  @Size(max=500) String imageUrl,@Size(max=30000) String description,@NotBlank String status,
  int sortOrder,@Min(0) int expectedVersion,@NotNull @Size(max=100) List<@NotNull @Positive Long> articleIds){}
 public record Item(String id,String title,String summary,String contentType,String publishedAt,String coverUrl,String categoryName){}
 public record Topic(String id,String title,String subtitle,String imageUrl,String description,String status,
  Integer sortOrder,Integer lockVersion,List<String> articleIds,List<Item> items){}
 private void manage(){actors.staff().require("yunji:operation:manage");}
 public List<Topic> list(boolean admin){if(admin)manage();return db.rows("SELECT * FROM activity_topic WHERE tenant_id='000000' AND deleted_at IS NULL"+(admin?"":" AND status='PUBLISHED'")+" ORDER BY sort_order,id LIMIT 100").stream().map(r->view(r,admin,false)).toList();}
 public Topic detail(long id,boolean admin){if(admin)manage();return view(db.one("SELECT * FROM activity_topic WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL"+(admin?"":" AND status='PUBLISHED'"),id),admin,true);}
 private Topic view(Map<String,Object> row,boolean admin,boolean details){
  long id=Db.number(row.get("id"));
  var items=details?db.rows("SELECT a.id,a.title,a.summary,a.content_type,a.published_at,a.cover_url,(SELECT c.name FROM article_category_rel cr JOIN content_category c ON c.id=cr.category_id WHERE cr.article_id=a.id AND c.tenant_id=a.tenant_id AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY c.sort_order,c.id LIMIT 1) category_name FROM activity_topic_article r JOIN article a ON a.id=r.article_id WHERE r.topic_id=? AND "+ArticleAccess.VISIBLE+" ORDER BY r.sort_order,a.id",id).stream().map(r->new Item(r.get("id").toString(),r.get("title").toString(),(String)r.get("summary"),(String)r.get("contentType"),(String)r.get("publishedAt"),(String)r.get("coverUrl"),(String)r.get("categoryName"))).toList():List.<Item>of();
  var ids=admin&&details?db.rows("SELECT article_id FROM activity_topic_article WHERE topic_id=? ORDER BY sort_order,article_id",id).stream().map(r->r.get("articleId").toString()).toList():items.stream().map(Item::id).toList();
  return new Topic(row.get("id").toString(),(String)row.get("title"),(String)row.get("subtitle"),(String)row.get("imageUrl"),(String)row.get("description"),(String)row.get("status"),(int)Db.number(row.get("sortOrder")),(int)Db.number(row.get("lockVersion")),ids,items);
 }
 @Transactional public Topic save(Long id,Draft b){
  manage();String title=Db.text(b.title(),150);
  BusinessException.require(Set.of("DRAFT","PUBLISHED","OFFLINE").contains(b.status()),422,"专题状态错误");
  BusinessException.require(b.imageUrl()==null||b.imageUrl().isBlank()||b.imageUrl().matches("https?://[^\\s]+"),422,"图片地址仅允许HTTP(S)");
  BusinessException.require(new HashSet<>(b.articleIds()).size()==b.articleIds().size(),422,"关联资讯不能重复");
  for(long article:b.articleIds())db.one("SELECT id FROM article WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL",article);
  String description=b.description()==null?null:Jsoup.clean(b.description(),Safelist.basic());long key=id==null?db.id():id;long staff=actors.staff().id();
  if(id==null){BusinessException.require(b.expectedVersion()==0,409,"新增专题版本无效");db.update("INSERT INTO activity_topic(id,title,subtitle,image_url,description,status,sort_order,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?)",key,title,b.subtitle(),b.imageUrl(),description,b.status(),b.sortOrder(),staff,staff);}
  else {var old=db.one("SELECT lock_version FROM activity_topic WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion(),409,"专题已变化，请刷新后重试");db.update("UPDATE activity_topic SET title=?,subtitle=?,image_url=?,description=?,status=?,sort_order=?,updated_by=?,lock_version=lock_version+1 WHERE id=?",title,b.subtitle(),b.imageUrl(),description,b.status(),b.sortOrder(),staff,key);}
  db.update("DELETE FROM activity_topic_article WHERE topic_id=?",key);int order=0;for(long article:b.articleIds())db.update("INSERT INTO activity_topic_article(topic_id,article_id,sort_order) VALUES(?,?,?)",key,article,order++);
  db.audit(staff,"STAFF","TOPIC_SAVE","TOPIC",key,Map.of("status",b.status(),"articleCount",b.articleIds().size()));return detail(key,true);
 }
 @Transactional public void delete(long id,int expected){manage();var old=db.one("SELECT lock_version FROM activity_topic WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);BusinessException.require(Db.number(old.get("lockVersion"))==expected,409,"专题已变化，请刷新");db.update("UPDATE activity_topic SET status='OFFLINE',deleted_at=UTC_TIMESTAMP(),updated_by=?,lock_version=lock_version+1 WHERE id=?",actors.staff().id(),id);db.audit(actors.staff().id(),"STAFF","TOPIC_DELETE","TOPIC",id,Map.of());}
}
