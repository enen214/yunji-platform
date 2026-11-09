package com.sunshine.yunji.content.article.infrastructure;

import com.sunshine.yunji.content.article.domain.*;
import com.sunshine.yunji.content.shared.BusinessException;
import com.sunshine.yunji.content.shared.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import com.sunshine.yunji.content.discovery.domain.FeedSql;
import java.util.*;

@Repository
public class JdbcArticleRepository implements ArticleRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private static final String VISIBLE = " a.tenant_id=:tenant AND a.deleted_at IS NULL AND a.status='PUBLISHED' AND a.published_at <= UTC_TIMESTAMP() AND (a.effective_to IS NULL OR a.effective_to>UTC_TIMESTAMP()) AND a.current_version_id IS NOT NULL ";
    private static final RowMapper<ArticleCard> CARD = (r,i) -> card(r);
    public JdbcArticleRepository(NamedParameterJdbcTemplate jdbc,ObjectMapper mapper) { this.jdbc=jdbc;this.mapper=mapper; }
    private static Instant time(ResultSet r,String name) throws SQLException {
        Timestamp t=r.getTimestamp(name, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return t==null?null:t.toInstant();
    }
    private static ArticleCard card(ResultSet r) throws SQLException {
        return new ArticleCard(r.getString("id"),r.getString("title"),r.getString("summary"),r.getString("cover_url"),r.getString("content_type"),r.getString("source_name"),time(r,"published_at"),time(r,"effective_to"),r.getInt("score"),time(r,"effective_from"),r.getString("carrier_type"),r.getBoolean("abnormal"),r.getTimestamp("effective_to")!=null&&time(r,"effective_to").isBefore(Instant.now())?"EXPIRED":r.getTimestamp("effective_from")!=null&&time(r,"effective_from").isAfter(Instant.now())?"UPCOMING":"ACTIVE",r.getString("category_name"));
    }
    private String where(MapSqlParameterSource p,String q,Long categoryId,Long organizationId,Long customerId,boolean hotOnly,ArticleFilter filter) {
        String where=(filter.includeExpired()!=null?filter.includeExpired():(q!=null&&!q.isBlank()&&!hotOnly))?VISIBLE.replace(" AND (a.effective_to IS NULL OR a.effective_to>UTC_TIMESTAMP())",""):VISIBLE;
        if(!filter.contentTypes().isEmpty()){where+=" AND a.content_type IN (:contentTypes)";p.addValue("contentTypes",filter.contentTypes());}
        if(!filter.keywordIds().isEmpty()){where+=" AND EXISTS(SELECT 1 FROM article_keyword_rel kr JOIN keyword k ON k.id=kr.keyword_id WHERE kr.article_id=a.id AND kr.keyword_id IN (:keywords) AND k.tenant_id=:tenant AND k.status='ENABLED' AND k.deleted_at IS NULL)";p.addValue("keywords",filter.keywordIds());}
        if(filter.keywordId()!=null){where+=" AND EXISTS(SELECT 1 FROM article_keyword_rel kr JOIN keyword k ON k.id=kr.keyword_id WHERE kr.article_id=a.id AND kr.keyword_id=:keyword AND k.tenant_id=:tenant AND k.status='ENABLED' AND k.deleted_at IS NULL)";p.addValue("keyword",filter.keywordId());}
        if(!filter.labelIds().isEmpty()){where+=" AND EXISTS(SELECT 1 FROM article_label_rel lr JOIN content_label l ON l.id=lr.label_id JOIN content_category lc ON lc.id=l.category_id WHERE lr.article_id=a.id AND lr.label_id IN (:labels) AND l.tenant_id=:tenant AND l.status='ENABLED' AND l.deleted_at IS NULL AND lc.status='ENABLED' AND lc.deleted_at IS NULL)";p.addValue("labels",filter.labelIds());}
        if(!filter.categoryIds().isEmpty()){where+=" AND EXISTS(SELECT 1 FROM article_category_rel cr JOIN content_category cc ON cc.id=cr.category_id WHERE cr.article_id=a.id AND cr.category_id IN (:categories) AND cc.tenant_id=:tenant AND cc.status='ENABLED' AND cc.deleted_at IS NULL)";p.addValue("categories",filter.categoryIds());}
        if(hotOnly)where+=" AND a.hot_until>"+FeedSql.DAY;
        if(q!=null&&!q.isBlank()) {
            where+=" AND (a.title LIKE :q ESCAPE '!' OR a.summary LIKE :q ESCAPE '!' OR a.source_name LIKE :q ESCAPE '!')";
            p.addValue("q","%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
        if(categoryId!=null) {
            where+=" AND EXISTS (SELECT 1 FROM article_category_rel r JOIN content_category c ON c.id=r.category_id WHERE r.article_id=a.id AND r.category_id=:category AND c.tenant_id=:tenant AND c.deleted_at IS NULL AND c.status='ENABLED')";
            p.addValue("category",categoryId);
        }
        if(organizationId!=null) {
            where+=" AND EXISTS (SELECT 1 FROM article_organization_rel r JOIN organization o ON o.id=r.organization_id WHERE r.article_id=a.id AND r.organization_id=:organization AND o.tenant_id=:tenant AND o.deleted_at IS NULL AND o.status='ENABLED')";
            p.addValue("organization",organizationId);
        }
        return where;
    }
    @Override @Transactional public ArticlePage page(String tenantId,int pageNum,int pageSize,String q,Long categoryId,Long organizationId,Long customerId,boolean hotOnly,String cursor,String snapshotAt) {
        return search(tenantId,pageNum,pageSize,q,categoryId,organizationId,customerId,hotOnly,cursor,snapshotAt,ArticleFilter.legacy());
    }
    @Override @Transactional public ArticlePage search(String tenantId,int pageNum,int pageSize,String q,Long categoryId,Long organizationId,Long customerId,boolean hotOnly,String cursor,String snapshotAt,ArticleFilter filter) {
        var p=new MapSqlParameterSource("tenant",tenantId);
        String fingerprint=Db.hash(tenantId+"|"+Objects.toString(q,"")+"|"+categoryId+"|"+organizationId+"|"+customerId+"|"+hotOnly+"|"+pageSize+"|"+filter);
        List<ArticleCard> frozen;
        String token; Instant issued; int offset=(pageNum-1)*pageSize;
        try {
            if(customerId!=null)p.addValue("customer",customerId);
            if(cursor==null||cursor.isBlank()) {
                BusinessException.require(snapshotAt==null||snapshotAt.isBlank(),400,"重新查询请清除旧快照");
                String tier="CASE WHEN a.urgent=1 THEN 2 WHEN a.pinned=1 THEN 1 ELSE 0 END";
                if(hotOnly&&categoryId==null&&filter.categoryIds().isEmpty())tier="CASE WHEN EXISTS(SELECT 1 FROM home_recommendation hr WHERE hr.article_id=a.id AND hr.status='PUBLISHED' AND (hr.effective_from IS NULL OR hr.effective_from<=UTC_TIMESTAMP()) AND (hr.effective_to IS NULL OR hr.effective_to>UTC_TIMESTAMP())) THEN 3 WHEN a.urgent=1 THEN 2 WHEN a.pinned=1 THEN 1 ELSE 0 END";
                frozen=jdbc.query("SELECT a.*,(SELECT c.name FROM article_category_rel cr JOIN content_category c ON c.id=cr.category_id WHERE cr.article_id=a.id AND c.tenant_id=a.tenant_id AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY c.sort_order,c.id LIMIT 1) category_name,("+FeedSql.score(customerId!=null)+") score FROM article a WHERE "+where(p,q,categoryId,organizationId,customerId,hotOnly,filter)+" ORDER BY "+tier+" DESC,score DESC,a.published_at DESC,a.id DESC LIMIT 10001",p,CARD);
                BusinessException.require(frozen.size()<=10000,422,"结果超过10000条，请按分类或组织缩小查询");
                jdbc.update("DELETE FROM article_feed_snapshot WHERE tenant_id=:tenant AND token_hash NOT IN (SELECT token_hash FROM (SELECT token_hash FROM article_feed_snapshot WHERE tenant_id=:tenant ORDER BY issued_at DESC LIMIT 999) keepers)",p);
                token=UUID.randomUUID().toString(); issued=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                p.addValue("token",Db.hash(token)).addValue("fingerprint",fingerprint).addValue("issued",Timestamp.from(issued))
                    .addValue("cards",mapper.writeValueAsString(frozen));
                jdbc.update("DELETE FROM article_feed_snapshot WHERE expires_at<UTC_TIMESTAMP() LIMIT 100",p);
                jdbc.update("INSERT INTO article_feed_snapshot(token_hash,tenant_id,query_hash,issued_at,expires_at,cards_json) VALUES(:token,:tenant,:fingerprint,:issued,UTC_TIMESTAMP()+INTERVAL 1 DAY,:cards)",p);
            } else {
                String[] parts=cursor.split(":",-1);
                BusinessException.require(parts.length==2&&parts[0].matches("[a-f0-9-]{36}")&&parts[1].matches("[0-9]{1,6}"),400,"cursor 无效");
                token=parts[0];offset=Integer.parseInt(parts[1]);p.addValue("token",Db.hash(token));
                var found=jdbc.queryForList("SELECT *,DATE_FORMAT(issued_at,'%Y-%m-%dT%H:%i:%s.%fZ') issued_iso FROM article_feed_snapshot WHERE token_hash=:token AND tenant_id=:tenant AND expires_at>UTC_TIMESTAMP()",p);
                BusinessException.require(!found.isEmpty(),409,"资讯快照已过期，请刷新列表");
                var row=found.get(0);BusinessException.require(fingerprint.equals(row.get("query_hash")),400,"cursor 与查询条件不匹配");
                jdbc.update("UPDATE article_feed_snapshot SET expires_at=UTC_TIMESTAMP()+INTERVAL 1 DAY WHERE token_hash=:token",p);
                issued=Instant.parse(row.get("issued_iso").toString());
                BusinessException.require(snapshotAt!=null&&issued.equals(Instant.parse(snapshotAt)),400,"快照标识不匹配");
                frozen=mapper.readValue(row.get("cards_json").toString(),new TypeReference<List<ArticleCard>>(){});
            }
        } catch(BusinessException e) { throw e; }
        catch(Exception e) { throw new BusinessException(400,"资讯快照或cursor 无效"); }
        BusinessException.require(offset>=0&&offset<=frozen.size(),400,"cursor 越界");
        // Membership/order survives new publications. Current visibility is rechecked so
        // revoked content never leaks through an earlier public snapshot.
        Set<String> visible=new HashSet<>();
        if(!frozen.isEmpty()) {
            p.addValue("ids",frozen.stream().map(ArticleCard::id).toList());
            // Exposure membership is frozen with ranking at query start. Only content
            // revocation/expiry changes membership while a cursor is being consumed.
            visible.addAll(jdbc.query("SELECT a.id FROM article a WHERE "+where(p,q,categoryId,organizationId,null,hotOnly,filter)+" AND a.id IN (:ids)",p,(r,i)->r.getString(1)));
        }
        List<ArticleCard> items=new ArrayList<>();int next=offset;
        while(next<frozen.size()&&items.size()<pageSize) { ArticleCard item=frozen.get(next++);if(visible.contains(item.id()))items.add(item); }
        boolean hasMore=frozen.subList(next,frozen.size()).stream().anyMatch(c->visible.contains(c.id()));
        return new ArticlePage(items,visible.size(),pageNum,pageSize,issued.toString(),hasMore?token+":"+next:null);
    }
    private ArticlePresentation presentation(String snapshot){
        try {var root=mapper.readTree(snapshot);return root.hasNonNull("presentation")?mapper.treeToValue(root.get("presentation"),ArticlePresentation.class):ArticlePresentation.empty();}
        catch(Exception e){throw new IllegalStateException("Invalid versioned article presentation",e);}
    }
    @Override public Optional<ArticleDetail> detail(String tenantId,long id) {
        var p=new MapSqlParameterSource("tenant",tenantId).addValue("id",id);
        List<ArticleDetail> rows=jdbc.query("SELECT a.*,(SELECT c.name FROM article_category_rel cr JOIN content_category c ON c.id=cr.category_id WHERE cr.article_id=a.id AND c.tenant_id=a.tenant_id AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY c.sort_order,c.id LIMIT 1) category_name,("+FeedSql.score(false)+") score,v.rich_text,v.interpretation,v.original_text,v.relation_snapshot FROM article a JOIN article_version v ON v.id=a.current_version_id AND v.article_id=a.id WHERE "+VISIBLE.replace(" AND (a.effective_to IS NULL OR a.effective_to>UTC_TIMESTAMP())","")+" AND a.id=:id",p,(r,i)->new ArticleDetail(card(r),r.getString("rich_text"),r.getString("source_url"),r.getString("current_version_id"),r.getString("interpretation"),r.getString("original_text"),java.util.List.of(),java.util.List.of(),java.util.List.of(),presentation(r.getString("relation_snapshot")),java.util.List.of()));
        if(rows.isEmpty())return Optional.empty();
        ArticleDetail item=rows.get(0);
        var categories=jdbc.query("SELECT CAST(c.id AS CHAR) id,c.name FROM article_category_rel r JOIN content_category c ON c.id=r.category_id WHERE r.article_id=:id AND c.tenant_id=:tenant AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY c.sort_order,c.id",p,(r,i)->new ArticleDetail.Topic(r.getString("id"),r.getString("name")));
        var labels=jdbc.query("SELECT CAST(l.id AS CHAR) id,l.name FROM article_label_rel r JOIN content_label l ON l.id=r.label_id JOIN content_category c ON c.id=l.category_id WHERE r.article_id=:id AND l.tenant_id=:tenant AND c.tenant_id=:tenant AND l.status='ENABLED' AND l.deleted_at IS NULL AND c.status='ENABLED' AND c.deleted_at IS NULL ORDER BY l.sort_order,l.id",p,(r,i)->new ArticleDetail.Topic(r.getString("id"),r.getString("name")));
        var keywords=jdbc.query("SELECT CAST(k.id AS CHAR) id,k.display_name name FROM article_keyword_rel r JOIN keyword k ON k.id=r.keyword_id WHERE r.article_id=:id AND k.tenant_id=:tenant AND k.status='ENABLED' AND k.deleted_at IS NULL ORDER BY k.id",p,(r,i)->new ArticleDetail.Topic(r.getString("id"),r.getString("name")));
        var relatedIds=item.presentation().relatedArticleIds();
        List<ArticleDetail.Topic> related=List.of();
        if(relatedIds!=null&&!relatedIds.isEmpty()) {p.addValue("related",relatedIds);related=jdbc.query("SELECT a.id,a.title FROM article a WHERE "+VISIBLE+" AND a.id IN (:related) AND a.id<>:id ORDER BY a.published_at DESC,a.id DESC",p,(r,i)->new ArticleDetail.Topic(r.getString("id"),r.getString("title")));}
        return Optional.of(new ArticleDetail(item.article(),item.richText(),item.sourceUrl(),item.versionId(),item.interpretation(),item.originalText(),categories,labels,keywords,item.presentation(),related));
    }
}
