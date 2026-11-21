package com.sunshine.yunji.content.catalog.infrastructure;

import com.sunshine.yunji.content.catalog.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

/** 显式租户过滤；JDBC 不受 MyBatis 数据权限插件保护。 */
@Repository
public class JdbcCatalogRepository implements CatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final RowMapper<CatalogItem> ROW = (r, i) -> new CatalogItem(
        r.getString("id"), "0".equals(r.getString("parent_id")) ? null : r.getString("parent_id"), r.getString("name"), r.getString("type"), r.getInt("sort_order"));
    public JdbcCatalogRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<CatalogItem> categories(String tenantId) {
        return jdbc.query("SELECT id,NULL parent_id,name,'CATEGORY' type,sort_order FROM content_category WHERE tenant_id=:tenant AND deleted_at IS NULL AND status='ENABLED' ORDER BY sort_order,id", Map.of("tenant",tenantId), ROW);
    }
    @Override public List<CatalogItem> organizations(String tenantId) {
        return jdbc.query("SELECT id,parent_id,name,type,sort_order FROM organization WHERE tenant_id=:tenant AND deleted_at IS NULL AND status='ENABLED' ORDER BY sort_order,id", Map.of("tenant",tenantId), ROW);
    }
    @Override public List<CatalogItem> keywords(String tenantId, String query, int limit) {
        String escaped = query.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.query("SELECT k.id,NULL parent_id,k.display_name name,'KEYWORD' type,k.sort_order FROM keyword k WHERE k.tenant_id=:tenant AND k.deleted_at IS NULL AND k.status='ENABLED' AND (k.display_name LIKE :query ESCAPE '!' OR EXISTS(SELECT 1 FROM keyword_alias a WHERE a.keyword_id=k.id AND a.tenant_id=:tenant AND a.normalized_alias LIKE :query ESCAPE '!')) ORDER BY k.sort_order,k.id LIMIT :limit", Map.of("tenant",tenantId,"query","%"+escaped+"%","limit",limit), ROW);
    }
}
