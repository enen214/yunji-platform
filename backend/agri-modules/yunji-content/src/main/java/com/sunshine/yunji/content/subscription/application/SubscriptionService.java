package com.sunshine.yunji.content.subscription.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class SubscriptionService {
 private final Db db;private final ArticleAccess articles;private final com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders;
 @Transactional public void followLabels(long user,List<Long> ids,boolean enabled){for(long id:new TreeSet<>(ids))follow(user,"LABEL",id,enabled);}
 public SubscriptionService(Db db,ArticleAccess articles,com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders){this.db=db;this.articles=articles;this.reminders=reminders;}
 @Transactional public void acceptReplacement(long user,long old,long target){
  reminders.lock();db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
  var view=articles.replacement(old);
  BusinessException.require(view.target()!=null&&view.target().id().equals(Long.toString(target)),409,"替换目标已变化或不可见，请重新查看");
  var migrated=db.rows("SELECT follow_id,new_article_id FROM user_replacement_notice WHERE user_id=? AND old_article_id=?",user,old);
  if(!migrated.isEmpty()){
   BusinessException.require(Db.number(migrated.get(0).get("newArticleId"))==target,409,"替换目标已变化或不可见，请重新查看");
   return;
  }
  var follow=db.one("SELECT id FROM user_follow WHERE user_id=? AND target_type='ARTICLE' AND target_id=? AND deleted_at IS NULL FOR UPDATE",user,old);
  follow(user,"ARTICLE",target,true);db.update("UPDATE user_follow SET deleted_at=UTC_TIMESTAMP() WHERE id=?",follow.get("id"));
  db.audit(user,"CUSTOMER","FOLLOW_REPLACEMENT_ACCEPT","ARTICLE",old,Map.of("targetId",Long.toString(target)));
 }
 @Transactional public void follow(long user,String type,long id,boolean enabled){follow(user,type,id,enabled,false);}
 @Transactional public void follow(long user,String type,long id,boolean enabled,boolean stopReminders){
  reminders.lock();db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
  BusinessException.require(Set.of("KEYWORD","ARTICLE","LABEL").contains(type),422,"订阅类型不支持");
  if(enabled){if(type.equals("LABEL"))db.one("SELECT l.id FROM content_label l JOIN content_category c ON c.id=l.category_id WHERE l.id=? AND l.status='ENABLED' AND l.tenant_id='000000' AND l.deleted_at IS NULL AND c.status='ENABLED' AND c.deleted_at IS NULL",id);else if(type.equals("ARTICLE"))articles.readable(id);else if(type.equals("KEYWORD"))db.one("SELECT id FROM keyword WHERE id=? AND status='ENABLED' AND tenant_id='000000' AND deleted_at IS NULL",id);else db.one("SELECT id FROM content_category WHERE id=? AND status='ENABLED' AND tenant_id='000000' AND deleted_at IS NULL",id);
   db.update("INSERT INTO user_follow(id,user_id,target_type,target_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE deleted_at=NULL",db.id(),user,type,id);
  }else db.update("UPDATE user_follow SET deleted_at=UTC_TIMESTAMP() WHERE user_id=? AND target_type=? AND target_id=?",user,type,id);
  if(!enabled&&stopReminders&&Set.of("ARTICLE","KEYWORD").contains(type))db.update("UPDATE reminder_tracking SET enabled=0,lock_version=lock_version+1 WHERE user_id=? AND target_type=? AND target_id=? AND enabled=1",user,type,id);
  db.audit(user,"CUSTOMER","FOLLOW_SET",type,id,Map.of("enabled",enabled));
 }
 public List<Map<String,Object>> follows(long user){var rows=db.rows("SELECT f.*,COALESCE(k.display_name,c.name,a.title,l.name) name,COALESCE(p.hidden,0) hidden,COALESCE(p.push_enabled,1) push_enabled FROM user_follow f LEFT JOIN keyword k ON f.target_type='KEYWORD' AND k.id=f.target_id LEFT JOIN content_category c ON f.target_type='CATEGORY' AND c.id=f.target_id LEFT JOIN article a ON f.target_type='ARTICLE' AND a.id=f.target_id LEFT JOIN content_label l ON f.target_type='LABEL' AND l.id=f.target_id LEFT JOIN subscription_box_preference p ON p.follow_id=f.id AND p.user_id=f.user_id WHERE f.user_id=? AND f.deleted_at IS NULL ORDER BY f.created_at DESC,f.id DESC LIMIT 200",user);for(var row:rows)row.put("boxKind",Set.of("CATEGORY","LABEL").contains(row.get("targetType"))?"CATEGORY":"ARTICLE".equals(row.get("targetType"))?"FOLLOWING":"FOLLOWING");attachLabels(user,rows);return rows;}
 public record RestoreRule(long id,int expectedVersion){}
 public record Undo(long followId,String targetType,long targetId,List<RestoreRule> rules){}
 /** All restored rules and the follow commit together; concurrent rule edits are never overwritten. */
 @Transactional public void undo(long user,Undo b){
  reminders.lock();db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
  BusinessException.require(b.rules()!=null&&b.rules().size()<=100,422,"撤销数据不完整");
  db.one("SELECT id FROM user_follow WHERE id=? AND user_id=? AND target_type=? AND target_id=? AND deleted_at IS NOT NULL FOR UPDATE",b.followId(),user,b.targetType(),b.targetId());
  for(var rule:b.rules()){
   var current=reminders.detail(user,rule.id());
   BusinessException.require(b.targetType().equals(current.get("targetType"))&&b.targetId()==Db.number(current.get("targetId"))&&Db.number(current.get("enabled"))==0,409,"提醒状态已变化，请刷新");
   reminders.save(user,rule.id(),new com.sunshine.yunji.content.notification.application.ReminderTrackingService.Tracking(b.targetType(),b.targetId(),current.get("mode").toString(),true,rule.expectedVersion()));
  }
  follow(user,b.targetType(),b.targetId(),true,false);
 }
 private void attachLabels(long user,List<Map<String,Object>> follows){
  if(follows.isEmpty())return;
  String placeholders=String.join(",",Collections.nCopies(follows.size(),"?"));
  var args=new ArrayList<Object>();args.add(user);for(var f:follows)args.add(f.get("id"));
  var links=db.rows("SELECT DISTINCT f.id follow_id,l.id,l.name,c.id category_id,c.name category_name,l.sort_order FROM user_follow f JOIN content_label l ON ((f.target_type='LABEL' AND f.target_id=l.id) OR (f.target_type='ARTICLE' AND EXISTS(SELECT 1 FROM article_label_rel al JOIN article a ON a.id=al.article_id WHERE al.article_id=f.target_id AND al.label_id=l.id AND "+ArticleAccess.READABLE+")) OR (f.target_type='KEYWORD' AND (EXISTS(SELECT 1 FROM label_keyword_curation ck WHERE ck.label_id=l.id AND ck.keyword_id=f.target_id) OR EXISTS(SELECT 1 FROM article_keyword_rel ak JOIN article_label_rel al ON al.article_id=ak.article_id JOIN article a ON a.id=ak.article_id WHERE ak.keyword_id=f.target_id AND al.label_id=l.id AND "+ArticleAccess.CURRENT+")))) JOIN content_category c ON c.id=l.category_id WHERE f.user_id=? AND f.id IN ("+placeholders+") AND l.tenant_id='000000' AND l.status='ENABLED' AND l.deleted_at IS NULL AND c.tenant_id='000000' AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY l.sort_order,l.id",args.toArray());
  for(var f:follows)f.put("labels",links.stream().filter(l->Objects.toString(l.get("followId")).equals(Objects.toString(f.get("id")))).toList());
 }
 public List<Map<String,Object>> boxes(long user){
  var output=new ArrayList<Map<String,Object>>();var higher=new HashSet<String>();
  String eligible=" AND a.push_enabled=1 AND (a.pinned=1 OR ("+com.sunshine.yunji.content.discovery.domain.FeedSql.unread(Long.toString(user))+" AND NOT EXISTS(SELECT 1 FROM user_exposure_total e WHERE e.user_id="+user+" AND e.article_id=a.id AND e.day_key="+com.sunshine.yunji.content.discovery.domain.FeedSql.DAY+" AND e.day_count>=3))) ";
  for(var notice:db.rows("SELECT n.*,a.title FROM user_replacement_notice n JOIN article a ON a.id=n.old_article_id WHERE n.user_id=? ORDER BY n.created_at DESC,n.old_article_id DESC LIMIT 200",user)){
   var items=db.rows("SELECT a.id,a.title,a.summary,a.content_type,a.published_at,a.current_version_id,1 unread FROM article a WHERE "+ArticleAccess.CURRENT+eligible+" AND a.id=?",notice.get("newArticleId"));
   if(items.isEmpty())continue;String id=items.get(0).get("id").toString();if(!higher.add(id))continue;
   var box=new LinkedHashMap<String,Object>();box.put("id",notice.get("followId"));box.put("userId",Long.toString(user));box.put("targetType","ARTICLE");box.put("targetId",notice.get("newArticleId"));box.put("name",notice.get("title"));box.put("createdAt",notice.get("createdAt"));box.put("hidden",false);box.put("pushEnabled",true);box.put("boxKind","REPLACEMENT");box.put("items",items);box.put("unreadCount",items.size());box.put("replacement",articles.replacement(Db.number(notice.get("oldArticleId"))));output.add(box);
  }
  var follows=follows(user);
  for(var kind:List.of("FOLLOWING","CATEGORY")){
   var thisTier=new HashSet<String>();
   for(var f:follows){
    if(!kind.equals(f.get("boxKind")))continue;
    String match=switch(f.get("targetType").toString()){
     case "ARTICLE" -> "a.id=?";
     case "KEYWORD" -> "EXISTS(SELECT 1 FROM article_keyword_rel r WHERE r.article_id=a.id AND r.keyword_id=?)";
     case "LABEL" -> "EXISTS(SELECT 1 FROM article_label_rel r WHERE r.article_id=a.id AND r.label_id=?)";
     default -> "FALSE AND a.id=?";
    };
    var items=db.rows("SELECT a.id,a.title,a.summary,a.content_type,a.published_at,a.current_version_id,1 unread FROM article a WHERE "+ArticleAccess.CURRENT+eligible+" AND "+match+" ORDER BY a.urgent DESC,a.pinned DESC,("+com.sunshine.yunji.content.discovery.domain.FeedSql.score(true).replace(":customer",Long.toString(user))+") DESC,a.published_at DESC,a.id DESC LIMIT 200",f.get("targetId"));
    items=items.stream().filter(item->!higher.contains(item.get("id").toString())).toList();
    if(items.isEmpty())continue;
    for(var item:items)thisTier.add(item.get("id").toString());
    f.put("hidden",false);f.put("pushEnabled",true);f.put("items",items);f.put("unreadCount",items.size());output.add(f);
   }
   higher.addAll(thisTier);
  }
  return output;
 }
}
