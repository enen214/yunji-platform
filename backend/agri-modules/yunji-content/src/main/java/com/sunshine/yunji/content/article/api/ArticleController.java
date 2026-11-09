package com.sunshine.yunji.content.article.api;

import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.article.application.ArticleQueryService;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import com.sunshine.yunji.content.article.domain.*;
import com.sunshine.yunji.content.shared.ApiResponse;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/v1/articles")
public class ArticleController {
    private final ArticleQueryService service;
    private final ArticleAccess access;
    public ArticleController(ArticleQueryService service,ArticleAccess access) { this.service=service;this.access=access; }
    @GetMapping("/archive") public ApiResponse<java.util.List<Views.ArchivedView>> archive(){return ApiResponse.ok(ViewMapper.list(access.archive(),Views.ArchivedView.class));}
    @GetMapping public ApiResponse<ArticlePage> page(jakarta.servlet.http.HttpServletRequest request,
        @RequestParam(defaultValue="1") @Min(1) @Max(1000) int pageNum,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize,
        @RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(required=false) @Positive Long categoryId,
        @RequestParam(defaultValue="true") boolean hotOnly,
        @RequestParam(required=false) @Size(max=80) String contentType,
        @RequestParam(required=false) @Size(max=30) java.util.List<@Positive Long> labelIds,
        @RequestParam(required=false) @Size(max=30) java.util.List<@Positive Long> keywordIds,
        @RequestParam(required=false) @Size(max=300) String cursor,
        @RequestParam(required=false) @Size(max=80) String snapshotAt) {
        return ApiResponse.ok(service.feed(request,pageNum,pageSize,q,categoryId,hotOnly,cursor,snapshotAt,
            new ArticleFilter(contentType,labelIds==null?java.util.List.of():labelIds,null,false,keywordIds==null?java.util.List.of():keywordIds)));
    }
    @GetMapping("/search") public ApiResponse<ArticlePage> search(jakarta.servlet.http.HttpServletRequest request,
        @RequestParam(defaultValue="1") @Min(1) @Max(1000) int pageNum,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize,
        @RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(required=false) @Positive Long categoryId,
        @RequestParam(required=false) @Positive Long organizationId,
        @io.swagger.v3.oas.annotations.Parameter(description="内容类型，可逗号分隔多选，类型之间为 OR")
        @RequestParam(required=false) @Size(max=80) String contentType,
        @RequestParam(required=false) @Size(max=30) java.util.List<@Positive Long> labelIds,
        @RequestParam(required=false) @Size(max=30) java.util.List<@Positive Long> categoryIds,
        @RequestParam(required=false) @Positive Long keywordId,
        @io.swagger.v3.oas.annotations.Parameter(description="关键词多选，关键词之间为 OR；与标签/板块条件取交集")
        @RequestParam(required=false) @Size(max=30) java.util.List<@Positive Long> keywordIds,
        @RequestParam(defaultValue="false") boolean includeExpired,
        @RequestParam(required=false) @Size(max=300) String cursor,
        @RequestParam(required=false) @Size(max=80) String snapshotAt) {
        var labels=labelIds==null?java.util.List.<Long>of():labelIds.stream().distinct().sorted().toList();
        var keywords=keywordIds==null?java.util.List.<Long>of():keywordIds.stream().distinct().sorted().toList();
        return ApiResponse.ok(service.search(request,pageNum,pageSize,q,categoryId,organizationId,cursor,snapshotAt,new ArticleFilter(contentType,labels,keywordId,includeExpired,keywords,categoryIds==null?java.util.List.of():categoryIds.stream().distinct().sorted().toList())));
    }
    @GetMapping("/{id}") public ApiResponse<ArticleDetail> detail(@PathVariable @Positive long id) { return ApiResponse.ok(service.detail(id)); }
    @GetMapping("/{id}/replacement") public ApiResponse<ReplacementView> replacement(@PathVariable @Positive long id){return ApiResponse.ok(access.replacement(id));}
}
