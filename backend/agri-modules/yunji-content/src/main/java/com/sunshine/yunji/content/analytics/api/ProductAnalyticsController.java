package com.sunshine.yunji.content.analytics.api;

import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Business-day queries over source events, not fabricated aggregates. */
@RestController
public class ProductAnalyticsController {
 private final Db db;private final Actors actors;
 public ProductAnalyticsController(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Counts(long views,long favorites,long follows,long shares,long exposures,long reports){}
 public record User(String id,String nickname,String lastActiveAt,long activeDays30,Double averageIntervalDays,long events30){}
 public record Day(String day,long activeUsers,long events){}
 public record Gap(String query,long requests,long users){}
 public record Replacement(String day,long confirmed,long involvedUsers){}
 public record Download(String activityId,String activityTitle,String fileId,String fileName,long downloads){}
 public record Event(String eventType,String objectId,String ingestedAt){}
 public record Timeline(List<Day> days,List<Event> events){}
 public record Dashboard(List<Day> activeDays,List<Gap> searchGaps,List<Replacement> replacements,List<Download> downloads){}
 public record ReplacementTotals(long confirmed,long involvedUsers,long chained){}
 public record ActivityMetric(String id,String title,long capacity,long registrations,long confirmed,long waitlisted,long cancelled){}
 public record DownloadMetric(String activityId,String fileId,String fileName,long downloads,long users){}
 public record ReviewMetric(String id,String title,long applications,long submitted,long approved,long rejected,long correction){}
 public record EngagementMetric(String id,String title,long clicks,long viewers,long favorites,long shares,long exposures,long reports){}
 public record Metrics(ReplacementTotals replacement,List<ActivityMetric> activities,List<DownloadMetric> downloads,List<ReviewMetric> reviews,List<EngagementMetric> engagement){}
 @GetMapping("/api/v1/admin/analytics/product-metrics") public ApiResponse<Metrics> metrics(){
  actors.staff();
  return ApiResponse.ok(new Metrics(
   new ReplacementTotals(db.count("SELECT COUNT(*) FROM article_replacement WHERE status='RESOLVED'"),db.count("SELECT COUNT(DISTINCT user_id) FROM user_replacement_notice"),db.count("SELECT COUNT(*) FROM article_replacement r WHERE r.status='RESOLVED' AND EXISTS(SELECT 1 FROM article_replacement n WHERE n.old_article_id=r.new_article_id AND n.status='RESOLVED')")),
   ViewMapper.list(db.rows("SELECT a.id,a.title,a.capacity,COUNT(r.id) registrations,COALESCE(SUM(r.status='CONFIRMED'),0) confirmed,COALESCE(SUM(r.status='WAITLISTED'),0) waitlisted,COALESCE(SUM(r.status='CANCELLED'),0) cancelled FROM activity a LEFT JOIN activity_registration r ON r.activity_id=a.id WHERE a.tenant_id='000000' GROUP BY a.id,a.title,a.capacity ORDER BY a.id DESC LIMIT 200"),ActivityMetric.class),
   ViewMapper.list(db.rows("SELECT d.activity_id,d.file_id,f.name file_name,COUNT(*) downloads,COUNT(DISTINCT d.user_id) users FROM activity_download d JOIN global_file f ON f.id=d.file_id GROUP BY d.activity_id,d.file_id,f.name ORDER BY downloads DESC,d.activity_id,d.file_id LIMIT 200"),DownloadMetric.class),
   ViewMapper.list(db.rows("SELECT c.id,c.title,COUNT(a.id) applications,COALESCE(SUM(a.status='SUBMITTED'),0) submitted,COALESCE(SUM(a.status='APPROVED'),0) approved,COALESCE(SUM(a.status='REJECTED'),0) rejected,COALESCE(SUM(a.status='CORRECTION'),0) correction FROM review_campaign c LEFT JOIN review_application a ON a.campaign_id=c.id WHERE c.tenant_id='000000' GROUP BY c.id,c.title ORDER BY c.id DESC LIMIT 200"),ReviewMetric.class),
   ViewMapper.list(db.rows("SELECT a.id,a.title,(SELECT COUNT(*) FROM behavior_event e WHERE e.event_type='PAGE_VIEW' AND e.object_id=a.id) clicks,(SELECT COUNT(DISTINCT user_id) FROM behavior_event e WHERE e.event_type='PAGE_VIEW' AND e.object_id=a.id) viewers,(SELECT COUNT(*) FROM user_favorite f WHERE f.article_id=a.id AND f.deleted_at IS NULL) favorites,(SELECT COUNT(*) FROM behavior_event e WHERE e.event_type='SHARE' AND e.object_id=a.id) shares,(SELECT COALESCE(SUM(visible_count),0) FROM user_exposure_total x WHERE x.article_id=a.id) exposures,(SELECT COUNT(*) FROM user_report r WHERE r.article_id=a.id) reports FROM article a WHERE a.tenant_id='000000' AND a.deleted_at IS NULL ORDER BY a.id DESC LIMIT 200"),EngagementMetric.class)));
 }
 @GetMapping("/api/v1/articles/{id}/statistics") public ApiResponse<Counts> counts(@PathVariable long id){
  db.one("SELECT a.id FROM article a WHERE a.id=? AND "+com.sunshine.yunji.content.article.application.ArticleAccess.READABLE,id);
  return ApiResponse.ok(new Counts(db.count("SELECT COUNT(*) FROM behavior_event WHERE event_type='PAGE_VIEW' AND object_id=?",id),db.count("SELECT COUNT(*) FROM user_favorite WHERE article_id=? AND deleted_at IS NULL",id),db.count("SELECT COUNT(*) FROM user_follow WHERE target_type='ARTICLE' AND target_id=? AND deleted_at IS NULL",id),db.count("SELECT COUNT(*) FROM behavior_event WHERE event_type='SHARE' AND object_id=?",id),db.count("SELECT COALESCE(SUM(visible_count),0) FROM user_exposure_total WHERE article_id=?",id),db.count("SELECT COUNT(*) FROM user_report WHERE article_id=?",id)));
 }
 @GetMapping("/api/v1/admin/analytics/product") public ApiResponse<Dashboard> dashboard(){actors.staff();
  return ApiResponse.ok(new Dashboard(ViewMapper.list(db.rows("WITH RECURSIVE days AS (SELECT DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)-INTERVAL 29 DAY day UNION ALL SELECT day+INTERVAL 1 DAY FROM days WHERE day<DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)) SELECT DATE_FORMAT(d.day,'%Y-%m-%d') day,COUNT(DISTINCT e.user_id) active_users,COUNT(e.id) events FROM days d LEFT JOIN behavior_event e ON e.tenant_id='000000' AND e.ingested_at>=d.day-INTERVAL 8 HOUR AND e.ingested_at<d.day+INTERVAL 1 DAY-INTERVAL 8 HOUR GROUP BY d.day ORDER BY d.day"),Day.class),ViewMapper.list(db.rows("SELECT query,COUNT(*) requests,COUNT(DISTINCT user_id) users FROM customer_search_log WHERE result_count=0 AND searched_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY GROUP BY query ORDER BY requests DESC,query LIMIT 50"),Gap.class),ViewMapper.list(db.rows("SELECT DATE_FORMAT(r.resolved_at+INTERVAL 8 HOUR,'%Y-%m-%d') day,COUNT(DISTINCT r.id) confirmed,COUNT(DISTINCT n.user_id) involved_users FROM article_replacement r LEFT JOIN user_replacement_notice n ON n.old_article_id=r.old_article_id WHERE r.status='RESOLVED' AND r.resolved_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY GROUP BY day ORDER BY day DESC"),Replacement.class),ViewMapper.list(db.rows("SELECT d.activity_id,a.title activity_title,d.file_id,f.name file_name,COUNT(*) downloads FROM activity_download d JOIN activity a ON a.id=d.activity_id JOIN global_file f ON f.id=d.file_id GROUP BY d.activity_id,a.title,d.file_id,f.name ORDER BY downloads DESC,d.activity_id,d.file_id LIMIT 100"),Download.class)));
 }
 @GetMapping("/api/v1/admin/analytics/customer-frequency") public ApiResponse<List<User>> users(){actors.staff();return ApiResponse.ok(ViewMapper.list(db.rows("SELECT u.id,u.nickname,MAX(e.ingested_at) last_active_at,COUNT(DISTINCT CASE WHEN e.ingested_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY THEN DATE(e.ingested_at+INTERVAL 8 HOUR) END) active_days30,IF(COUNT(DISTINCT DATE(e.ingested_at+INTERVAL 8 HOUR))>1,DATEDIFF(MAX(e.ingested_at),MIN(e.ingested_at))/(COUNT(DISTINCT DATE(e.ingested_at+INTERVAL 8 HOUR))-1),NULL) average_interval_days,SUM(e.ingested_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY) events30 FROM app_user u LEFT JOIN behavior_event e ON e.user_id=u.id WHERE u.tenant_id='000000' AND u.deleted_at IS NULL GROUP BY u.id,u.nickname ORDER BY last_active_at DESC,u.id LIMIT 200"),User.class));}
 @GetMapping("/api/v1/admin/analytics/customer-frequency/{id}") public ApiResponse<Timeline> timeline(@PathVariable long id){actors.staff();db.one("SELECT id FROM app_user WHERE id=? AND tenant_id='000000'",id);return ApiResponse.ok(new Timeline(ViewMapper.list(db.rows("SELECT DATE_FORMAT(ingested_at+INTERVAL 8 HOUR,'%Y-%m-%d') day,1 active_users,COUNT(*) events FROM behavior_event WHERE user_id=? AND ingested_at>=UTC_TIMESTAMP()-INTERVAL 30 DAY GROUP BY day ORDER BY day",id),Day.class),ViewMapper.list(db.rows("SELECT event_type,object_id,ingested_at FROM behavior_event WHERE user_id=? ORDER BY ingested_at DESC,id DESC LIMIT 200",id),Event.class)));}
}
