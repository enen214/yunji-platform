package com.sunshine.yunji.content.review.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.review.application.ReviewService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
public class ReviewController {
 private final ReviewService s;private final Actors actors;
 public ReviewController(ReviewService s,Actors actors){this.s=s;this.actors=actors;}
 @GetMapping("/api/v1/review-campaigns") public ApiResponse<java.util.List<Views.PublicCampaignView>> campaigns(){return ApiResponse.ok(ViewMapper.list(s.campaigns(),Views.PublicCampaignView.class));}
 @GetMapping("/api/v1/review-campaigns/{id}") public ApiResponse<Views.PublicCampaignView> campaign(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.campaign(id,true),Views.PublicCampaignView.class));}
 @GetMapping("/api/v1/me/review-campaigns/{id}/eligibility") public ApiResponse<Views.EligibilityView> eligibility(HttpServletRequest r,@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.eligibility(actors.customer(r),id),Views.EligibilityView.class));}
 @PostMapping("/api/v1/me/review-campaigns/{id}/draft") public ApiResponse<Views.ApplicationView> draft(HttpServletRequest r,@PathVariable long id,@RequestBody ReviewService.Materials b){return ApiResponse.ok(ViewMapper.of(s.draft(actors.customer(r),id,b),Views.ApplicationView.class));}
 @GetMapping("/api/v1/me/review-applications") public ApiResponse<java.util.List<Views.ApplicationView>> mine(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.mine(actors.customer(r)),Views.ApplicationView.class));}
 @GetMapping("/api/v1/me/review-applications/{id}") public ApiResponse<Views.ApplicationView> detail(HttpServletRequest r,@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.mineDetail(actors.customer(r),id),Views.ApplicationView.class));}
 @PostMapping("/api/v1/me/review-applications/{id}/actions") public ApiResponse<Views.ApplicationView> action(HttpServletRequest r,@PathVariable long id,@Valid @RequestBody ReviewService.Action b){return ApiResponse.ok(ViewMapper.of(s.action(actors.customer(r),id,b),Views.ApplicationView.class));}
 @PostMapping("/api/v1/admin/review-campaigns") public ApiResponse<Views.CampaignView> create(@Valid @RequestBody ReviewService.Campaign b){return ApiResponse.ok(ViewMapper.of(s.create(b),Views.CampaignView.class));}
 @GetMapping("/api/v1/admin/review-campaigns") public ApiResponse<java.util.List<Views.CampaignView>> adminList(){return ApiResponse.ok(ViewMapper.list(s.adminCampaigns(),Views.CampaignView.class));}
 @GetMapping("/api/v1/admin/review-campaigns/{id}") public ApiResponse<Views.CampaignView> adminCampaign(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.adminCampaign(id),Views.CampaignView.class));}
 @PutMapping("/api/v1/admin/review-campaigns/{id}") public ApiResponse<Views.CampaignView> editCampaign(@PathVariable long id,@Valid @RequestBody ReviewService.CampaignUpdate b){return ApiResponse.ok(ViewMapper.of(s.updateCampaign(id,b),Views.CampaignView.class));}
 @GetMapping("/api/v1/admin/review-applications") public ApiResponse<java.util.List<Views.ApplicationView>> queue(){return ApiResponse.ok(ViewMapper.list(s.queue(),Views.ApplicationView.class));}
 @GetMapping("/api/v1/admin/review-applications/{id}") public ApiResponse<Views.ApplicationView> staffDetail(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.staffDetail(id),Views.ApplicationView.class));}
 @PostMapping("/api/v1/admin/review-applications/{id}/decisions") public ApiResponse<Views.ApplicationView> decide(@PathVariable long id,@Valid @RequestBody ReviewService.Action b){return ApiResponse.ok(ViewMapper.of(s.decide(id,b),Views.ApplicationView.class));}
}
