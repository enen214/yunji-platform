package com.sunshine.yunji.content.article.api;
import com.sunshine.yunji.content.article.application.ArticleCommands;
import com.sunshine.yunji.content.shared.*;
import jakarta.validation.Valid;
import com.sunshine.yunji.content.shared.contract.*;
import org.springframework.web.bind.annotation.*;

@RestController @org.springframework.validation.annotation.Validated @RequestMapping("/api/v1/admin/articles")
public class ArticleAdminController {
    private final ArticleCommands service;
    public ArticleAdminController(ArticleCommands service){this.service=service;}
    public record Editor(String id,String title,String summary,String contentType,String status,String richText,
        String originalText,String sourceName,String sourceUrl,String effectiveFrom,String effectiveTo,
        boolean pinned,int contentWeight,int hotWeight,String hotUntil,String lifespan,boolean urgent,
        boolean pushEnabled,int lockVersion,String relationSnapshot,String publishedAt) {}
    public record State(@jakarta.validation.constraints.Min(0) int expectedVersion,@jakarta.validation.constraints.NotBlank String action) {}
    public record Replacement(@jakarta.validation.constraints.Positive long newArticleId) {}
    public record Candidate(String id,String oldArticleId,String newArticleId,String oldTitle,String newTitle,String status,String createdAt) {}
    public record Page(java.util.List<Editor> items,long total,int page,int size) {}
    @GetMapping public ApiResponse<java.util.List<Editor>> list(){return ApiResponse.ok(ViewMapper.list(service.list(),Editor.class));}
    @GetMapping("/page") public ApiResponse<Page> page(@RequestParam(defaultValue="1") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(1000000) int page,@RequestParam(defaultValue="20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,@RequestParam(defaultValue="") @jakarta.validation.constraints.Size(max=100) String q){
        var result=service.page(page,size,q);return ApiResponse.ok(new Page(ViewMapper.list((java.util.List<java.util.Map<String,Object>>)result.get("items"),Editor.class),Db.number(result.get("total")),page,size));
    }
    @GetMapping("/{id}") public ApiResponse<Editor> detail(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(service.detail(id),Editor.class));}
    @PostMapping public ApiResponse<Editor> create(@Valid @RequestBody ArticleCommands.Draft body){return ApiResponse.ok(ViewMapper.of(service.create(body),Editor.class));}
    @PutMapping("/{id}") public ApiResponse<Editor> edit(@PathVariable long id,@Valid @RequestBody ArticleCommands.Draft body){return ApiResponse.ok(ViewMapper.of(service.edit(id,body),Editor.class));}
    @PostMapping("/{id}/state") public ApiResponse<Void> state(@PathVariable long id,@Valid @RequestBody State body){service.state(id,body.expectedVersion(),body.action());return ApiResponse.ok(null);}
    @PostMapping("/{id}/replacement") public ApiResponse<Void> replace(@PathVariable long id,@Valid @RequestBody Replacement body){service.replace(id,body.newArticleId());return ApiResponse.ok(null);}
    @GetMapping("/replacement-candidates") public ApiResponse<java.util.List<Candidate>> candidates(){return ApiResponse.ok(ViewMapper.list(service.candidates(),Candidate.class));}
    @PostMapping("/replacement-candidates/{id}/ignore") public ApiResponse<Void> ignore(@PathVariable long id){service.ignore(id);return ApiResponse.ok(null);}
}
