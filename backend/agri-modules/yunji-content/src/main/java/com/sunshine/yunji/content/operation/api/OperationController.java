package com.sunshine.yunji.content.operation.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.operation.application.OperationService;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class OperationController {
 private final OperationService s;public OperationController(OperationService s){this.s=s;}
 @GetMapping("/api/v1/home") public ApiResponse<Views.HomeView> home(){return ApiResponse.ok(ViewMapper.of(s.home(),Views.HomeView.class));}
 @GetMapping("/api/v1/pages/{key}") public ApiResponse<Views.PageView> page(@PathVariable String key){return ApiResponse.ok(ViewMapper.of(s.page(key),Views.PageView.class));}
 @GetMapping("/api/v1/admin/operation/banners") public ApiResponse<java.util.List<Views.BannerView>> banners(){return ApiResponse.ok(ViewMapper.list(s.banners(),Views.BannerView.class));}
 @PostMapping("/api/v1/admin/operation/banners") public ApiResponse<Views.BannerView> banner(@RequestBody OperationService.Banner b){return ApiResponse.ok(ViewMapper.of(s.banner(null,b),Views.BannerView.class));}
 @PutMapping("/api/v1/admin/operation/banners/{id}") public ApiResponse<Views.BannerView> edit(@PathVariable long id,@RequestBody OperationService.Banner b){return ApiResponse.ok(ViewMapper.of(s.banner(id,b),Views.BannerView.class));}
 @DeleteMapping("/api/v1/admin/operation/banners/{id}") public ApiResponse<Void> delete(@PathVariable long id){s.deleteBanner(id);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/admin/operation/recommendations") public ApiResponse<java.util.List<Views.RecommendationView>> recs(){return ApiResponse.ok(ViewMapper.list(s.recommendations(),Views.RecommendationView.class));}
 @PostMapping("/api/v1/admin/operation/recommendations") public ApiResponse<Views.RecommendationView> rec(@RequestBody OperationService.Recommend b){return ApiResponse.ok(ViewMapper.of(s.recommend(b),Views.RecommendationView.class));}
 @DeleteMapping("/api/v1/admin/operation/recommendations/{id}") public ApiResponse<Void> deleteRec(@PathVariable long id){s.deleteRecommendation(id);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/admin/operation/pages") public ApiResponse<java.util.List<Views.AdminPageSummaryView>> pages(){return ApiResponse.ok(ViewMapper.list(s.pages(),Views.AdminPageSummaryView.class));}
 @PostMapping("/api/v1/admin/operation/pages") public ApiResponse<Views.PageView> publish(@RequestBody OperationService.Page b){return ApiResponse.ok(ViewMapper.of(s.page(b),Views.PageView.class));}
 @PostMapping("/api/v1/admin/operation/activity-columns") public ApiResponse<Views.ColumnView> column(@RequestBody OperationService.Column b){return ApiResponse.ok(ViewMapper.of(s.column(b),Views.ColumnView.class));}
 @GetMapping("/api/v1/admin/audit") public ApiResponse<java.util.List<Views.AuditView>> audit(){return ApiResponse.ok(ViewMapper.list(s.audit(),Views.AuditView.class));}
}
