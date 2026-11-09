package com.sunshine.yunji.content.catalog.application;

import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import com.sunshine.yunji.content.identity.application.Actors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DiscoveryService {
    private final Db db; private final Actors actors; private final com.sunshine.yunji.content.subscription.application.SubscriptionService subscriptions;
    public DiscoveryService(Db db,Actors actors,com.sunshine.yunji.content.subscription.application.SubscriptionService subscriptions){this.db=db;this.actors=actors;this.subscriptions=subscriptions;}
    public List<Map<String,Object>> boards(){return db.rows("SELECT c.id,c.name,c.description,(SELECT COUNT(*) FROM article_category_rel r JOIN article a ON a.id=r.article_id WHERE r.category_id=c.id AND "+ArticleAccess.VISIBLE+") article_count FROM content_category c WHERE c.tenant_id='000000' AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY c.sort_order,c.id LIMIT 500");}
    public List<Map<String,Object>> keywords(long label){return db.rows("SELECT DISTINCT k.id,k.display_name name FROM keyword k JOIN article_keyword_rel kr ON kr.keyword_id=k.id JOIN article_label_rel lr ON lr.article_id=kr.article_id JOIN content_label l ON l.id=lr.label_id JOIN content_category c ON c.id=l.category_id JOIN article a ON a.id=kr.article_id WHERE l.id=? AND l.status='ENABLED' AND l.deleted_at IS NULL AND c.status='ENABLED' AND c.deleted_at IS NULL AND k.status='ENABLED' AND k.deleted_at IS NULL AND k.tenant_id='000000' AND "+ArticleAccess.VISIBLE+" ORDER BY k.display_name,k.id LIMIT 100",label);}
    public List<Map<String,Object>> history(long user){return db.rows("SELECT query,searched_at FROM customer_search_history WHERE user_id=? ORDER BY searched_at DESC,query LIMIT 20",user);}
    @Transactional public void remember(long user,String query){
        db.one("SELECT id FROM app_user WHERE id=? FOR UPDATE",user);
        db.update("INSERT INTO customer_search_log(id,user_id,query) VALUES(?,?,?)",db.id(),user,Db.text(query,100));
        db.update("INSERT INTO customer_search_history(user_id,query) VALUES(?,?) ON DUPLICATE KEY UPDATE searched_at=UTC_TIMESTAMP()",user,Db.text(query,100));
        var rows=db.rows("SELECT query FROM customer_search_history WHERE user_id=? ORDER BY searched_at DESC,query LIMIT 20,100",user);
        for(var row:rows)db.update("DELETE FROM customer_search_history WHERE user_id=? AND query=?",user,row.get("query"));
    }
    @Transactional public void clear(long user){db.update("DELETE FROM customer_search_history WHERE user_id=?",user);db.audit(user,"CUSTOMER","SEARCH_HISTORY_CLEAR","USER",user,Map.of());}
    // Only published, managed keywords may appear publicly. Arbitrary private queries never become hot terms.
    public List<Map<String,Object>> hot(){return db.rows("SELECT k.id,k.display_name name,COUNT(DISTINCT h.user_id) searchers FROM keyword k JOIN customer_search_log h ON h.query=k.display_name JOIN app_user u ON u.id=h.user_id AND u.status='ACTIVE' AND u.deleted_at IS NULL WHERE h.searched_at>=UTC_TIMESTAMP()-INTERVAL 7 DAY AND k.tenant_id='000000' AND k.status='ENABLED' AND k.deleted_at IS NULL AND EXISTS(SELECT 1 FROM article_keyword_rel r JOIN article a ON a.id=r.article_id WHERE r.keyword_id=k.id AND "+ArticleAccess.VISIBLE+") GROUP BY k.id,k.display_name ORDER BY searchers DESC,k.id LIMIT 20");}
    @Transactional public void removeHistory(long user,String query){db.update("DELETE FROM customer_search_history WHERE user_id=? AND query=?",user,Db.text(query,100));}
    public List<Map<String,Object>> popular(){return db.rows("SELECT a.id,a.title,COUNT(*) clicks FROM behavior_event e JOIN article a ON a.id=e.object_id WHERE e.event_type='PAGE_VIEW' AND e.occurred_at>=UTC_TIMESTAMP()-INTERVAL 7 DAY AND "+ArticleAccess.CURRENT+" GROUP BY a.id,a.title ORDER BY clicks DESC,a.id DESC LIMIT 10");}
    public Map<String,Object> summary(long user){
        var boxes=subscriptions.boxes(user);var updates=new HashSet<String>();var today=new HashSet<String>();
        var day=java.time.LocalDate.now(com.sunshine.yunji.content.discovery.domain.FeedRanking.BUSINESS_ZONE);
        for(var box:boxes)for(var item:(List<Map<String,Object>>)box.get("items")){
            if(!"CATEGORY".equals(box.get("boxKind")))updates.add(item.get("id").toString());
            else if(item.get("publishedAt")!=null){
                var published=java.time.Instant.parse(item.get("publishedAt").toString()).atZone(com.sunshine.yunji.content.discovery.domain.FeedRanking.BUSINESS_ZONE).toLocalDate();
                if(day.equals(published))today.add(item.get("id").toString());
            }
        }
        return Map.of("unreadCount",updates.size(),"sentToday",today.size(),"timezone","Asia/Shanghai");
    }

    @Transactional public void favorite(long user,long activity,boolean enabled){
        if(enabled){db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000' AND status='PUBLISHED'",activity);db.update("INSERT IGNORE INTO activity_favorite(user_id,activity_id) VALUES(?,?)",user,activity);}
        else db.update("DELETE FROM activity_favorite WHERE user_id=? AND activity_id=?",user,activity);
        db.audit(user,"CUSTOMER","ACTIVITY_FAVORITE","ACTIVITY",activity,Map.of("enabled",enabled));
    }
    public List<Map<String,Object>> favorites(long user){return db.rows("SELECT a.id,a.title,a.event_start,a.event_end FROM activity_favorite f JOIN activity a ON a.id=f.activity_id WHERE f.user_id=? AND a.tenant_id='000000' AND a.status='PUBLISHED' ORDER BY f.created_at DESC,a.id LIMIT 200",user);}
    public Map<String,Object> customers(int page,int size,String q){
        actors.staff();
        String term="%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String where="tenant_id='000000' AND deleted_at IS NULL AND COALESCE(nickname,'') LIKE ? ESCAPE '!'";
        return Map.of("total",db.count("SELECT COUNT(*) FROM app_user WHERE "+where,term),"items",db.rows("SELECT u.*,(SELECT MAX(e.ingested_at) FROM behavior_event e WHERE e.user_id=u.id) last_active_at,(SELECT COUNT(DISTINCT DATE(e.ingested_at+INTERVAL 8 HOUR)) FROM behavior_event e WHERE e.user_id=u.id AND e.ingested_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY) active_days30,(SELECT IF(COUNT(DISTINCT DATE(e.ingested_at+INTERVAL 8 HOUR))>1,DATEDIFF(MAX(DATE(e.ingested_at+INTERVAL 8 HOUR)),MIN(DATE(e.ingested_at+INTERVAL 8 HOUR)))/(COUNT(DISTINCT DATE(e.ingested_at+INTERVAL 8 HOUR))-1),NULL) FROM behavior_event e WHERE e.user_id=u.id AND e.ingested_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY) average_interval_days FROM (SELECT id,nickname,status,created_at,last_login_at FROM app_user WHERE "+where+" ORDER BY created_at DESC,id LIMIT ? OFFSET ?) u",term,size,(page-1)*size));
    }
}
