package com.sunshine.yunji.content.article.application;

import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

/** Direct publication. Existing content IDs remain stable for delivery/read ledgers. */
@Service
public class ArticleCommands {
    private final Db db;private final Actors actors;private final com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders;
    public ArticleCommands(Db db,Actors actors,com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders){this.db=db;this.actors=actors;this.reminders=reminders;}
    public record Draft(@NotBlank @Size(max=255) String title,@Size(max=1000) String summary,@Size(max=100000) String richText,@Size(max=150) String sourceName,@Size(max=500) String sourceUrl,@NotBlank String contentType,Instant effectiveFrom,Instant effectiveTo,List<Long> categoryIds,List<Long> organizationIds,List<Long> keywordIds,@Size(max=500) String changeSummary,Boolean pinned,Integer contentWeight,Integer hotWeight,Integer freshnessWeight,@Size(max=30000) String interpretation,@Size(max=100000) String originalText,String carrierType,int expectedVersion,List<Long> labelIds,@jakarta.validation.Valid com.sunshine.yunji.content.article.domain.ArticlePresentation presentation,@Size(max=30) List<@NotBlank @Size(max=100) String> keywordNames,Integer hotDays,String lifespan,Boolean urgent,Boolean pushEnabled,@Size(max=500) String longImageUrl){}
    private Timestamp time(Instant t){return t==null?null:Timestamp.from(t);}
    private Map<String,Object> lock(long id,Actors.Staff a){return db.one("SELECT * FROM article WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);}
    private List<Long> references(List<Long> ids,String table){
        if(ids==null)return List.of();BusinessException.require(ids.size()<=30&&new HashSet<>(ids).size()==ids.size(),422,"分类或关键词过多/重复");
        for(Long id:ids)db.one("SELECT id FROM "+table+" WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",id);
        return List.copyOf(ids);
    }
    public static String normalizedKeyword(String name){return java.text.Normalizer.normalize(Db.text(name,100),java.text.Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);}
    private Map<String,Object> validate(Draft b,Long batch){
        Db.text(b.title(),255);
        var type=db.one("SELECT * FROM content_type_definition WHERE code=? AND enabled=1",b.contentType());
        BusinessException.require(b.effectiveFrom()==null||b.effectiveTo()==null||b.effectiveTo().isAfter(b.effectiveFrom()),422,"有效期顺序错误");
        BusinessException.require(b.sourceUrl()==null||b.sourceUrl().isBlank()||b.sourceUrl().matches("https?://[^\\s]+"),422,"来源链接仅允许HTTP(S)");
        String text=Objects.toString(b.richText(),"");
        if(Db.number(type.get("requireBody"))==1)BusinessException.require(!Jsoup.parse(text).text().isBlank()||!Jsoup.parse(text).select("img[src]").isEmpty(),422,"此类型必须填写二级正文");
        if(Db.number(type.get("requireOriginal"))==1)BusinessException.require(b.originalText()!=null&&!b.originalText().isBlank()||b.sourceUrl()!=null&&!b.sourceUrl().isBlank(),422,"此类型必须填写一级原文或原文链接");
        if(Db.number(type.get("requireSummary"))==1)Db.text(b.summary(),1000);
        if(Db.number(type.get("requireDates"))==1)BusinessException.require(b.effectiveFrom()!=null&&b.effectiveTo()!=null,422,"此类型必须填写起止时间");
        if(Db.number(type.get("requireImage"))==1)BusinessException.require(b.longImageUrl()!=null&&b.longImageUrl().matches("https?://[^\\s<>\"]+"),422,"攻略必须填写HTTP(S)长图链接");
        var categories=references(b.categoryIds(),"content_category");BusinessException.require(categories.size()==1,422,"必须且只能选择一个板块");
        var labels=references(b.labelIds(),"content_label");BusinessException.require(!labels.isEmpty(),422,"至少选择一个板块标签");
        for(long label:labels)BusinessException.require(db.count("SELECT COUNT(*) FROM content_label WHERE id=? AND category_id=?",label,categories.get(0))==1,422,"标签必须属于当前板块");
        var words=new LinkedHashSet<>(references(b.keywordIds(),"keyword"));
        for(String raw:b.keywordNames()==null?List.<String>of():b.keywordNames()){
            String name=Db.text(raw,100),normalized=normalizedKeyword(name);
            BusinessException.require(db.count("SELECT COUNT(*) FROM keyword_blacklist WHERE tenant_id='000000' AND normalized_name=?",normalized)==0,422,"关键词已禁用");
            var aliases=db.rows("SELECT k.id,k.status,k.deleted_at FROM keyword_alias a JOIN keyword k ON k.id=a.keyword_id WHERE a.tenant_id='000000' AND a.normalized_alias=? FOR UPDATE",normalized);
            if(!aliases.isEmpty()){
                var word=aliases.get(0);BusinessException.require("ENABLED".equals(word.get("status"))&&word.get("deletedAt")==null,422,"关键词已停用");words.add(Db.number(word.get("id")));continue;
            }
            db.update("INSERT INTO keyword(id,display_name,normalized_name,import_batch_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE normalized_name=VALUES(normalized_name)",db.id(),name,normalized,batch);
            // Authentication can establish an older REPEATABLE READ snapshot before
            // waiting for the content lock. Resolve the upsert using a current read.
            var word=db.one("SELECT id,status,deleted_at FROM keyword WHERE tenant_id='000000' AND normalized_name=? FOR UPDATE",normalized);
            BusinessException.require("ENABLED".equals(word.get("status"))&&word.get("deletedAt")==null,422,"关键词已停用");words.add(Db.number(word.get("id")));
        }
        BusinessException.require(!words.isEmpty()&&words.size()<=30,422,"关键词需1至30个");
        for(long word:words)for(long label:labels)db.update("INSERT IGNORE INTO label_keyword_curation(label_id,keyword_id) VALUES(?,?)",label,word);
        int importance=b.contentWeight()==null?1:b.contentWeight(),hot=b.hotDays()!=null?b.hotDays():b.hotWeight()==null?0:b.hotWeight();
        BusinessException.require(importance>=1&&importance<=5&&hot>=0&&hot<=30,422,"重要度应为1至5，热点天数应为0至30");
        String life=b.lifespan()==null?(b.contentType().equals("NOTICE")?"SCHEDULED":"LONG"):b.lifespan();
        BusinessException.require(Set.of("URGENT","SHORT","LONG","SCHEDULED").contains(life),422,"时限类型错误");
        BusinessException.require(!life.equals("SCHEDULED")||b.effectiveFrom()!=null,422,"定时资讯需要生效时间");
        var rel=new LinkedHashMap<String,Object>();rel.put("categoryIds",categories);rel.put("labelIds",labels);rel.put("keywordIds",List.copyOf(words));rel.put("organizationIds",List.of());
        rel.put("presentation",b.presentation()==null?com.sunshine.yunji.content.article.domain.ArticlePresentation.empty():b.presentation());
        rel.put("importance",importance);rel.put("hotDays",hot);rel.put("lifespan",life);
        rel.put("renderer",type.get("renderer"));return rel;
    }
    @Transactional public Map<String,Object> create(Draft b){return create(b,null,true);}
    @Transactional public Map<String,Object> create(Draft b,Long batch,boolean detect){
        var actor=actors.staff();reminders.lock();db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
        var rel=validate(b,batch);long id=db.id(),content=db.id();
        db.update("INSERT INTO article(id,title,content_type,created_by,created_dept_id,import_batch_id) VALUES(?,?,?,?,?,?)",id,b.title(),b.contentType(),actor.id(),actor.department(),batch);
        // One mutable content row, never appended on edits. The FK identity is not a product version.
        db.update("INSERT INTO article_version(id,article_id,version_no,title,relation_snapshot) VALUES(?,?,1,?,?)",content,id,b.title(),db.json(rel));
        write(id,content,b,rel,actor,true);
        if(detect)detect(id,null);
        if(b.pushEnabled()==null||b.pushEnabled())reminders.published(id,content,true);
        db.event("ARTICLE",id,"ARTICLE_PUBLISHED",Map.of("articleId",Long.toString(id),"contentId",Long.toString(content)));
        db.audit(actor.id(),"STAFF","ARTICLE_CREATE","ARTICLE",id,Map.of("title",b.title()));return detail(id);
    }
    @Transactional public Map<String,Object> edit(long id,Draft b){
        var a=actors.staff();reminders.lock();var row=lock(id,a);
        BusinessException.require(Db.number(row.get("lockVersion"))==b.expectedVersion(),409,"内容已变化，请刷新后重试");
        BusinessException.require(db.count("SELECT COUNT(*) FROM article_replacement WHERE old_article_id=? AND status='RESOLVED'",id)==0,409,"已更替资讯不可编辑，请编辑当前信息");
        var rel=validate(b,null);write(id,Db.number(row.get("currentVersionId")),b,rel,a,false);
        db.audit(a.id(),"STAFF","ARTICLE_CORRECTION","ARTICLE",id,Map.of("title",b.title()));return detail(id);
    }
    @SuppressWarnings("unchecked") private void write(long id,long content,Draft b,Map<String,Object> rel,Actors.Staff a,boolean first){
        String html=Objects.toString(b.richText(),"");
        if("LONG_IMAGE".equals(rel.get("renderer")))html="<img src=\""+org.jsoup.nodes.Entities.escape(b.longImageUrl())+"\" />";
        html=Jsoup.clean(html,Safelist.basicWithImages());
        db.update("UPDATE article_version SET title=?,summary=?,rich_text=?,source_name=?,source_url=?,content_type=?,effective_from=?,effective_to=?,relation_snapshot=?,original_text=?,interpretation=NULL,carrier_type=?,pinned=?,content_weight=?,hot_weight=? WHERE id=?",b.title(),b.summary(),html,b.sourceName(),"LONG_IMAGE".equals(rel.get("renderer"))?null:b.sourceUrl(),b.contentType(),time(b.effectiveFrom()),time(b.effectiveTo()),db.json(rel),"LONG_IMAGE".equals(rel.get("renderer"))?null:b.originalText(),"LONG_IMAGE".equals(rel.get("renderer"))?"LONG_IMAGE":"TEXT_IMAGE",b.pinned()!=null&&b.pinned(),rel.get("importance"),rel.get("hotDays"),content);
        String taxonomy=Db.hash(rel.get("categoryIds")+"|"+new TreeSet<>((List<Long>)rel.get("labelIds"))+"|"+new TreeSet<>((List<Long>)rel.get("keywordIds")));
        var today=LocalDate.now(com.sunshine.yunji.content.discovery.domain.FeedRanking.BUSINESS_ZONE);int hot=((Number)rel.get("hotDays")).intValue();
        db.update("UPDATE article SET title=?,summary=?,source_name=?,source_url=?,content_type=?,effective_from=?,effective_to=?,pinned=?,content_weight=?,hot_weight=?,hot_until=?,hot_archived_at=NULL,lifespan=?,urgent=?,push_enabled=?,taxonomy_hash=?,carrier_type=?,status=IF(?,'PUBLISHED',status),current_version_id=?,latest_version_id=?,version_no=1,submitted_version_id=NULL,published_at=COALESCE(published_at,UTC_TIMESTAMP()),lock_version=lock_version+1,updated_by=? WHERE id=?",b.title(),b.summary(),b.sourceName(),"LONG_IMAGE".equals(rel.get("renderer"))?null:b.sourceUrl(),b.contentType(),time(b.effectiveFrom()),time(b.effectiveTo()),b.pinned()!=null&&b.pinned(),rel.get("importance"),hot,hotUntil(id,first,hot,today),rel.get("lifespan"),b.urgent()!=null&&b.urgent()||"URGENT".equals(rel.get("lifespan")),b.pushEnabled()==null||b.pushEnabled(),taxonomy,"LONG_IMAGE".equals(rel.get("renderer"))?"LONG_IMAGE":"TEXT_IMAGE",first,content,content,a.id(),id);
        for(var pair:List.of(new String[]{"categoryIds","article_category_rel","category_id"},new String[]{"labelIds","article_label_rel","label_id"},new String[]{"keywordIds","article_keyword_rel","keyword_id"})){
            db.update("DELETE FROM "+pair[1]+" WHERE article_id=?",id);for(long value:(List<Long>)rel.get(pair[0]))db.update("INSERT INTO "+pair[1]+"(article_id,"+pair[2]+") VALUES(?,?)",id,value);
        }
    }
    private java.sql.Date hotUntil(long id,boolean first,int days,LocalDate today){
        if(days==0)return null;
        if(!first){var old=db.one("SELECT hot_weight,hot_until FROM article WHERE id=?",id);if(Db.number(old.get("hotWeight"))==days&&old.get("hotUntil")!=null)return java.sql.Date.valueOf(old.get("hotUntil").toString());}
        return java.sql.Date.valueOf(today.plusDays(days));
    }
    @Transactional public void detect(long id,Long batch){
        String extra=batch==null?" AND old.created_at<newer.created_at ":" AND (old.import_batch_id IS NULL OR old.import_batch_id<>?) AND old.created_at<(SELECT created_at FROM article_import_batch WHERE id=?) ";
        var args=new ArrayList<Object>();args.add(id);if(batch!=null){args.add(batch);args.add(batch);}
        var olds=db.rows("SELECT old.id FROM article old JOIN article newer ON newer.id=? AND newer.taxonomy_hash=old.taxonomy_hash WHERE old.id<>newer.id AND old.status='PUBLISHED' AND old.deleted_at IS NULL AND (old.effective_to IS NULL OR old.effective_to>UTC_TIMESTAMP()) "+extra,args.toArray());
        for(var old:olds)db.update("INSERT IGNORE INTO article_replacement_candidate(id,old_article_id,new_article_id) VALUES(?,?,?)",db.id(),old.get("id"),id);
    }
    public List<Map<String,Object>> list(){actors.staff();return db.rows("SELECT * FROM article WHERE tenant_id='000000' AND deleted_at IS NULL ORDER BY updated_at DESC,id DESC LIMIT 200");}
    public Map<String,Object> page(int page,int size,String query){
        actors.staff();String term="%"+query.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String where="tenant_id='000000' AND deleted_at IS NULL AND title LIKE ? ESCAPE '!'";
        return Map.of("items",db.rows("SELECT * FROM article WHERE "+where+" ORDER BY updated_at DESC,id DESC LIMIT ? OFFSET ?",term,size,(page-1)*size),"total",db.count("SELECT COUNT(*) FROM article WHERE "+where,term),"page",page,"size",size);
    }
    public Map<String,Object> detail(long id){actors.staff();var row=db.one("SELECT a.*,v.rich_text,v.original_text,v.relation_snapshot FROM article a LEFT JOIN article_version v ON v.id=a.current_version_id WHERE a.id=? AND a.tenant_id='000000' AND a.deleted_at IS NULL",id);return row;}
    @Transactional public Map<String,Object> state(long id,int expected,String action){
        var actor=actors.staff();reminders.lock();var row=lock(id,actor);BusinessException.require(Db.number(row.get("lockVersion"))==expected,409,"内容已变化，请刷新");
        BusinessException.require(Set.of("OFFLINE","RESTORE","DELETE","END").contains(action),422,"未知操作");
        if(action.equals("RESTORE"))BusinessException.require(db.count("SELECT COUNT(*) FROM article_replacement WHERE old_article_id=? AND status='RESOLVED'",id)==0,409,"已更替资讯不可恢复主动展示");
        if(action.equals("END"))db.update("UPDATE article SET effective_from=IF(effective_from>=UTC_TIMESTAMP(),NULL,effective_from),effective_to=UTC_TIMESTAMP(),lock_version=lock_version+1 WHERE id=?",id);
        else if(action.equals("DELETE"))db.update("UPDATE article SET deleted_at=UTC_TIMESTAMP(),lock_version=lock_version+1 WHERE id=?",id);
        else db.update("UPDATE article SET status=?,lock_version=lock_version+1 WHERE id=?",action.equals("OFFLINE")?"OFFLINE":"PUBLISHED",id);
        if(!action.equals("RESTORE"))db.update("UPDATE reminder_change SET state='CANCELLED' WHERE article_id=? AND state='PENDING'",id);
        db.audit(actor.id(),"STAFF","ARTICLE_"+action,"ARTICLE",id,Map.of());return Map.of("id",Long.toString(id),"action",action);
    }
    public List<Map<String,Object>> candidates(){actors.staff();return db.rows("SELECT c.*,old.title old_title,newer.title new_title FROM article_replacement_candidate c JOIN article old ON old.id=c.old_article_id JOIN article newer ON newer.id=c.new_article_id WHERE c.status='PENDING' AND old.deleted_at IS NULL AND newer.deleted_at IS NULL ORDER BY c.created_at,c.id LIMIT 500");}
    @Transactional public void ignore(long id){var a=actors.staff();db.update("UPDATE article_replacement_candidate SET status='IGNORED',resolved_by=?,resolved_at=UTC_TIMESTAMP() WHERE id=? AND status='PENDING'",a.id(),id);db.audit(a.id(),"STAFF","REPLACEMENT_IGNORE","CANDIDATE",id,Map.of());}
    private void migrateSubscriptions(long old,long newer,Actors.Staff actor){
        var users=new HashSet<Long>();
        for(var f:db.rows("SELECT * FROM user_follow WHERE target_type='ARTICLE' AND target_id=? AND deleted_at IS NULL ORDER BY user_id FOR UPDATE",old)){
            long user=Db.number(f.get("userId")),oldFollow=Db.number(f.get("id"));users.add(user);
            db.update("INSERT INTO user_follow(id,user_id,target_type,target_id) VALUES(?,?,'ARTICLE',?) ON DUPLICATE KEY UPDATE deleted_at=NULL",db.id(),user,newer);
            var target=db.one("SELECT id FROM user_follow WHERE user_id=? AND target_type='ARTICLE' AND target_id=? FOR UPDATE",user,newer);
            var preferences=db.rows("SELECT * FROM subscription_box_preference WHERE user_id=? AND follow_id=?",user,oldFollow);
            if(!preferences.isEmpty()){
                var pref=preferences.get(0);
                // An explicitly configured target preference wins over an inherited old one.
                db.update("INSERT IGNORE INTO subscription_box_preference(user_id,follow_id,hidden,push_enabled) VALUES(?,?,?,?)",user,target.get("id"),pref.get("hidden"),pref.get("pushEnabled"));
            }
            db.update("UPDATE user_follow SET deleted_at=UTC_TIMESTAMP() WHERE id=?",oldFollow);
        }
        // Reminders are independent of follows. Migrate paused rules as well as active ones.
        for(var rule:db.rows("SELECT * FROM notification_rule WHERE target_type='ARTICLE' AND target_id=? ORDER BY user_id,id FOR UPDATE",old)){
            long user=Db.number(rule.get("userId"));users.add(user);
            String hash=com.sunshine.yunji.content.notification.application.NotificationService.ruleHash(db,"ARTICLE",newer,rule.get("mode").toString(),rule.get("channel").toString(),rule.get("timezone").toString(),Objects.toString(rule.get("scheduledAt"),""),Objects.toString(rule.get("localSendTime"),""));
            var duplicates=db.rows("SELECT id FROM notification_rule WHERE user_id=? AND rule_hash=? AND id<>? FOR UPDATE",user,hash,rule.get("id"));
            // Preserve the explicit target rule; obsolete rule becomes a disabled historical copy.
            if(duplicates.isEmpty())db.update("UPDATE notification_rule SET target_id=?,rule_hash=?,lock_version=lock_version+1 WHERE id=?",newer,hash,rule.get("id"));
            else db.update("UPDATE notification_rule SET target_id=?,rule_hash=?,enabled=0,next_run_at=NULL,lock_version=lock_version+1 WHERE id=?",newer,Db.hash("REPLACED_DUPLICATE:"+rule.get("id")),rule.get("id"));
        }
        for(long user:users){
            db.update("UPDATE user_replacement_notice SET new_article_id=? WHERE user_id=? AND new_article_id=?",newer,user,old);
            db.update("INSERT INTO user_replacement_notice(user_id,follow_id,old_article_id,new_article_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE new_article_id=VALUES(new_article_id)",user,db.id(),old,newer);
            db.audit(actor.id(),"STAFF","REPLACEMENT_SUBSCRIPTIONS_MIGRATED","ARTICLE",old,Map.of("userId",Long.toString(user),"targetId",Long.toString(newer)));
        }
    }
    @Transactional public Map<String,Object> replace(long old,long newer){
        var a=actors.staff();BusinessException.require(old!=newer,422,"不能替换自身");
        reminders.lock();db.one("SELECT tenant_id FROM article_replacement_guard WHERE tenant_id='000000' FOR UPDATE");
        var source=lock(old,a);lock(newer,a);
        BusinessException.require(source.get("currentVersionId")!=null,409,"只能替换曾发布的资讯");
        db.one("SELECT a.id FROM article a WHERE "+ArticleAccess.VISIBLE+" AND a.id=?",newer);
        BusinessException.require(db.count("SELECT COUNT(*) FROM article_replacement WHERE old_article_id=? AND status='RESOLVED'",old)==0,409,"资讯已有替换目标");
        long cursor=newer;var seen=new HashSet<Long>();seen.add(old);
        for(int depth=0;;depth++){
            BusinessException.require(depth<32&&seen.add(cursor),409,"替换关系不能成环或超过32层");
            var edges=db.rows("SELECT new_article_id FROM article_replacement WHERE old_article_id=? AND status='RESOLVED'",cursor);
            if(edges.isEmpty())break;cursor=Db.number(edges.get(0).get("newArticleId"));
        }
        db.update("INSERT INTO article_replacement(id,old_article_id,new_article_id,status,created_by,resolved_by,resolved_at) VALUES(?,?,?,'RESOLVED',?,?,UTC_TIMESTAMP())",db.id(),old,newer,a.id(),a.id());
        migrateSubscriptions(old,newer,a);
        reminders.replaced(old,newer,Db.number(db.one("SELECT current_version_id FROM article WHERE id=?",newer).get("currentVersionId")));
        db.update("UPDATE article SET effective_from=IF(effective_from>=UTC_TIMESTAMP(),NULL,effective_from),effective_to=UTC_TIMESTAMP(),lock_version=lock_version+1 WHERE id=?",old);
        db.update("UPDATE article_replacement_candidate SET status=IF(new_article_id=?,'CONFIRMED','OBSOLETE'),resolved_by=?,resolved_at=UTC_TIMESTAMP() WHERE old_article_id=? AND status='PENDING'",newer,a.id(),old);
        db.audit(a.id(),"STAFF","REPLACE","ARTICLE",old,Map.of("newArticleId",Long.toString(newer)));return Map.of("oldArticleId",Long.toString(old),"newArticleId",Long.toString(newer));
    }
}
