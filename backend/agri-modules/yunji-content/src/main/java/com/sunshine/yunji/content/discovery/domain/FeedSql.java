package com.sunshine.yunji.content.discovery.domain;

/** Fixed SQL fragments; request values are always bound parameters. */
public final class FeedSql {
    private FeedSql() {}
    public static final String DAY="DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)";
    public static final String HOT="LEAST(30,GREATEST(0,COALESCE(DATEDIFF(a.hot_until,"+DAY+"),0)))";
    public static String count(boolean personal){return personal?"COALESCE((SELECT e.visible_count FROM user_exposure_total e WHERE e.user_id=:customer AND e.article_id=a.id),0)":"0";}
    public static String score(boolean personal){
        String age="GREATEST(0,TIMESTAMPDIFF(SECOND,IF(a.lifespan='SCHEDULED',COALESCE(a.effective_from,a.published_at),a.published_at),UTC_TIMESTAMP())/3600.0)";
        String half="CASE a.lifespan WHEN 'URGENT' THEN 6 WHEN 'SHORT' THEN 24 WHEN 'SCHEDULED' THEN 48 ELSE 168 END";
        String base="(10*a.content_weight+5*"+HOT+"+50*POWER(2,-("+age+")/("+half+")))";
        if(!personal)return base;
        String matched="(SELECT COUNT(*) FROM user_follow f WHERE f.user_id=:customer AND f.deleted_at IS NULL AND ((f.target_type='ARTICLE' AND f.target_id=a.id) OR (f.target_type='KEYWORD' AND EXISTS(SELECT 1 FROM article_keyword_rel r WHERE r.article_id=a.id AND r.keyword_id=f.target_id)) OR (f.target_type='LABEL' AND EXISTS(SELECT 1 FROM article_label_rel r WHERE r.article_id=a.id AND r.label_id=f.target_id))))";
        String exposure="CASE "+count(true)+" WHEN 0 THEN 1 WHEN 1 THEN 1 WHEN 2 THEN .95 WHEN 3 THEN .9 WHEN 4 THEN .8 WHEN 5 THEN .7 WHEN 6 THEN .6 WHEN 7 THEN .45 WHEN 8 THEN .3 ELSE 0 END";
        return base+"*(CASE "+matched+" WHEN 0 THEN 1 WHEN 1 THEN 1.5 ELSE 2 END)*(CASE WHEN a.pinned=1 THEN 1 ELSE "+exposure+" END)";
    }
    public static String unread(String user){return "NOT EXISTS(SELECT 1 FROM user_read_state rs WHERE rs.user_id="+user+" AND rs.article_id=a.id) AND NOT EXISTS(SELECT 1 FROM user_exposure_total et WHERE et.user_id="+user+" AND et.article_id=a.id AND et.visible_count>=9)";}
}
