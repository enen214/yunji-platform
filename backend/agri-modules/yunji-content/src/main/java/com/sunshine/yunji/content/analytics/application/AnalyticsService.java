package com.sunshine.yunji.content.analytics.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service
public class AnalyticsService {
 private final Db db;private final Actors actors;public AnalyticsService(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record UserTotals(long activeAccounts,long closedAccounts,long uniqueUsersToday,long uniqueUsers30Days,String timezone){}
 public UserTotals users(){actors.staff().require("yunji:analytics:read");return new UserTotals(db.count("SELECT COUNT(*) FROM app_user WHERE tenant_id='000000' AND status='ACTIVE' AND deleted_at IS NULL"),db.count("SELECT COUNT(*) FROM app_user WHERE tenant_id='000000' AND status='CLOSED'"),db.count("SELECT COUNT(DISTINCT user_id) FROM behavior_event WHERE tenant_id='000000' AND ingested_at>=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)-INTERVAL 8 HOUR"),db.count("SELECT COUNT(DISTINCT user_id) FROM behavior_event WHERE tenant_id='000000' AND ingested_at>=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)-INTERVAL 8 HOUR-INTERVAL 29 DAY"),"Asia/Shanghai");}
 public Map<String,Object> overview(){actors.staff().require("yunji:analytics:read");return Map.of("publicationCount",db.count("SELECT COUNT(*) FROM article WHERE tenant_id='000000' AND status='PUBLISHED' AND deleted_at IS NULL"),"content",db.rows("SELECT a.id,a.title,COALESCE(e.events,0) events,COALESCE(e.view_count,0) view_count,COALESCE(e.viewer_count,0) viewer_count,COALESCE(f.favorite_count,0) favorite_count FROM article a LEFT JOIN(SELECT object_id,COUNT(*) events,SUM(event_type='PAGE_VIEW') view_count,COUNT(DISTINCT CASE WHEN event_type='PAGE_VIEW' THEN user_id END) viewer_count FROM behavior_event WHERE tenant_id='000000' GROUP BY object_id)e ON e.object_id=a.id LEFT JOIN(SELECT article_id,COUNT(*) favorite_count FROM user_favorite WHERE deleted_at IS NULL GROUP BY article_id)f ON f.article_id=a.id WHERE a.tenant_id='000000' AND a.deleted_at IS NULL ORDER BY events DESC,a.id DESC LIMIT 100"),"daily",db.rows("SELECT metric_date,metric_key,value FROM analytics_daily WHERE tenant_id='000000' ORDER BY metric_date DESC,metric_key LIMIT 300"));}
 @Transactional public Map<String,Object> aggregate(LocalDate date){actors.staff().require("yunji:analytics:read");BusinessException.require(date!=null&&!date.isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))),422,"聚合日期错误");
  // Replace the complete day inside one transaction: retry does not increment old aggregates.
  db.update("DELETE FROM analytics_daily WHERE tenant_id='000000' AND metric_date=?",date);
  String dim=Db.hash("ALL");var events=db.rows("SELECT event_type,COUNT(*) n FROM behavior_event WHERE event_type NOT IN('PAGE_STAY','SEARCH_CLICK') AND tenant_id='000000' AND DATE(ingested_at+INTERVAL 8 HOUR)=? GROUP BY event_type",date);for(var e:events)db.update("INSERT INTO analytics_daily(metric_date,metric_key,dimension_hash,value) VALUES(?,?,?,?)",date,e.get("eventType"),dim,e.get("n"));
  db.update("INSERT INTO analytics_daily(metric_date,metric_key,dimension_hash,value) SELECT ?,'UV',?,COUNT(DISTINCT user_id) FROM behavior_event WHERE tenant_id='000000' AND DATE(ingested_at+INTERVAL 8 HOUR)=?",date,dim,date);
  db.update("INSERT INTO analytics_daily(metric_date,metric_key,dimension_hash,value) SELECT ?,'DOWNLOAD_TICKETS',?,COUNT(*) FROM download_event WHERE event_type='ISSUED' AND DATE(received_at+INTERVAL 8 HOUR)=?",date,dim,date);
  db.update("INSERT INTO analytics_daily(metric_date,metric_key,dimension_hash,value) SELECT ?,'DOWNLOAD_SERVER_READS',?,COUNT(*) FROM download_event WHERE event_type='SERVER_READ' AND DATE(received_at+INTERVAL 8 HOUR)=?",date,dim,date);
  db.audit(actors.staff().id(),"STAFF","ANALYTICS_AGGREGATE","ANALYTICS",0,Map.of("date",date.toString()));return Map.of("date",date.toString(),"timezone","Asia/Shanghai","metrics",db.rows("SELECT metric_key,value FROM analytics_daily WHERE metric_date=? ORDER BY metric_key",date));
 }
}
