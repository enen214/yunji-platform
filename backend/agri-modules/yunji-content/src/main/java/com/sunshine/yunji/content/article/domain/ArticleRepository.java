package com.sunshine.yunji.content.article.domain;

import java.util.Optional;
public interface ArticleRepository {
    ArticlePage page(String tenantId, int pageNum, int pageSize, String q, Long categoryId, Long organizationId, Long customerId, boolean hotOnly, String cursor, String snapshotAt);
    default ArticlePage search(String tenantId,int pageNum,int pageSize,String q,Long categoryId,Long organizationId,Long customerId,boolean hotOnly,String cursor,String snapshotAt,ArticleFilter filter) {
        return page(tenantId,pageNum,pageSize,q,categoryId,organizationId,customerId,hotOnly,cursor,snapshotAt);
    }
    Optional<ArticleDetail> detail(String tenantId, long id);
}
