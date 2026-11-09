package com.sunshine.yunji.content.engagement.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.engagement.application.EngagementService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestController
public class EngagementController {
 private final EngagementService s;private final Actors actors;
 public EngagementController(EngagementService s,Actors actors){this.s=s;this.actors=actors;}
 public record State(boolean enabled){}public record Read(@Positive long versionId){}public record Report(@NotBlank String type,@NotBlank @Size(max=1000) String reason){}public record Feedback(@NotBlank @Size(max=2000) String description,@Pattern(regexp="GENERAL|CONTENT|REMINDER|SERVICE|OTHER") String type,@Size(max=100) String contact){}public record Event(@NotBlank String eventId,@NotBlank String type,Long objectId,Instant occurredAt,Map<String,Object> payload){}public record Decision(String status){}
 @PutMapping("/api/v1/me/favorites/{id}") public ApiResponse<Void> favorite(HttpServletRequest r,@PathVariable long id,@RequestBody State b){s.favorite(actors.customer(r),id,b.enabled);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/me/favorites") public ApiResponse<java.util.List<Views.FavoriteView>> favorites(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.favorites(actors.customer(r)),Views.FavoriteView.class));}
 @PostMapping("/api/v1/me/history/{id}") public ApiResponse<Void> browse(HttpServletRequest r,@PathVariable long id){s.browse(actors.customer(r),id);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/me/history") public ApiResponse<java.util.List<Views.HistoryView>> history(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.history(actors.customer(r)),Views.HistoryView.class));}
 @DeleteMapping("/api/v1/me/history") public ApiResponse<Void> clear(HttpServletRequest r){s.clearHistory(actors.customer(r));return ApiResponse.ok(null);}
 @DeleteMapping("/api/v1/me/history/{id}") public ApiResponse<Void> remove(HttpServletRequest r,@PathVariable long id){s.removeHistory(actors.customer(r),id);return ApiResponse.ok(null);}
 @DeleteMapping("/api/v1/me/favorites") public ApiResponse<Void> clearFavorites(HttpServletRequest r){s.clearFavorites(actors.customer(r));return ApiResponse.ok(null);}
 @PutMapping("/api/v1/me/read/{id}") public ApiResponse<Void> read(HttpServletRequest r,@PathVariable long id,@Valid @RequestBody Read b){s.read(actors.customer(r),id,b.versionId);return ApiResponse.ok(null);}
 @PostMapping("/api/v1/me/reports/{id}") public ApiResponse<Views.RequestView> report(HttpServletRequest r,@PathVariable long id,@Valid @RequestBody Report b){return ApiResponse.ok(ViewMapper.of(s.report(actors.customer(r),id,b.type,b.reason),Views.RequestView.class));}
 @PostMapping("/api/v1/me/feedback") public ApiResponse<Views.RequestView> feedback(HttpServletRequest r,@Valid @RequestBody Feedback b){return ApiResponse.ok(ViewMapper.of(s.feedback(actors.customer(r),b.description,b.type,b.contact),Views.RequestView.class));}
 @GetMapping("/api/v1/me/feedback") public ApiResponse<java.util.List<Views.FeedbackView>> feedback(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.feedback(actors.customer(r)),Views.FeedbackView.class));}
 @PostMapping("/api/v1/me/events") public ApiResponse<Void> event(HttpServletRequest r,@Valid @RequestBody Event b){s.event(actors.customer(r),b.eventId,b.type,b.objectId,b.occurredAt,b.payload);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/admin/moderation/{kind}") public ApiResponse<java.util.List<Views.ModerationView>> moderation(@PathVariable String kind){return ApiResponse.ok(ViewMapper.list(s.moderation(kind),Views.ModerationView.class));}
 @PostMapping("/api/v1/admin/moderation/{kind}/{id}") public ApiResponse<Void> handle(@PathVariable String kind,@PathVariable long id,@RequestBody Decision b){s.handle(kind,id,b.status);return ApiResponse.ok(null);}
}
