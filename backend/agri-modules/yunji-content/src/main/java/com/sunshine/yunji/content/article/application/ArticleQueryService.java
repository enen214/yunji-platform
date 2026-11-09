package com.sunshine.yunji.content.article.application;

import com.sunshine.yunji.content.article.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import com.sunshine.yunji.content.identity.application.Actors;

@Service
public class ArticleQueryService {
    private final ArticleRepository repository;
    private final String tenantId; private final Actors actors; private final com.sunshine.yunji.content.shared.Db db;
    public ArticleQueryService(ArticleRepository repository, @Value("${yunji.public-tenant-id:000000}") String tenantId, Actors actors,com.sunshine.yunji.content.shared.Db db) {
        this.repository = repository; this.tenantId = tenantId; this.actors=actors;this.db=db;
    }
    public ArticlePage page(int pageNum,int pageSize,String q,Long categoryId,Long organizationId,String cursor,String snapshotAt) {
        return repository.page(tenantId,pageNum,pageSize,q,categoryId,organizationId,null,false,cursor,snapshotAt);
    }
    public ArticlePage page(HttpServletRequest request,int pageNum,int pageSize,String q,Long categoryId,Long organizationId,boolean hotOnly,String cursor,String snapshotAt) {
        return repository.page(tenantId,pageNum,pageSize,q,categoryId,organizationId,actors.optionalCustomer(request),hotOnly,cursor,snapshotAt);
    }
    public ArticlePage feed(HttpServletRequest request,int page,int size,String q,Long category,boolean hot,String cursor,String snapshot,ArticleFilter filter){
        return repository.search(tenantId,page,size,q,category,null,actors.optionalCustomer(request),hot,cursor,snapshot,filter);
    }
    public ArticlePage search(HttpServletRequest request,int pageNum,int pageSize,String q,Long categoryId,Long organizationId,String cursor,String snapshotAt,ArticleFilter filter) {

        Long customer=actors.optionalCustomer(request);
        var result=repository.search(tenantId,pageNum,pageSize,q,categoryId,organizationId,customer,false,cursor,snapshotAt,filter);
        if(customer!=null&&(cursor==null||cursor.isBlank())&&q!=null&&!q.isBlank())db.update("UPDATE customer_search_log SET result_count=? WHERE user_id=? AND query=? AND result_count IS NULL ORDER BY searched_at DESC,id DESC LIMIT 1",result.total(),customer,q.strip());
        return result;
    }
    public ArticleDetail detail(long id) {
        return repository.detail(tenantId,id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"内容不存在或不可见"));
    }
}
