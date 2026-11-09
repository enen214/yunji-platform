package com.sunshine.yunji.content.subscription.api;
import com.sunshine.yunji.content.subscription.application.SubscriptionService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import com.sunshine.yunji.content.shared.contract.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me")
public class SubscriptionController {
 private final SubscriptionService s;private final Actors actors;
 public SubscriptionController(SubscriptionService s,Actors actors){this.s=s;this.actors=actors;}
 public record State(boolean enabled,Boolean stopReminders){}public record Hidden(boolean hidden){}
 public record Replacement(String targetId){}
 @PostMapping("/follows/undo") public ApiResponse<Void> undo(HttpServletRequest r,@RequestBody SubscriptionService.Undo b){s.undo(actors.customer(r),b);return ApiResponse.ok(null);}
 public record Labels(@jakarta.validation.constraints.NotEmpty @jakarta.validation.constraints.Size(max=100) java.util.List<@jakarta.validation.constraints.Positive Long> labelIds,boolean enabled){}
 @PutMapping("/follows/labels") public ApiResponse<Void> labels(HttpServletRequest r,@jakarta.validation.Valid @RequestBody Labels b){s.followLabels(actors.customer(r),b.labelIds(),b.enabled());return ApiResponse.ok(null);}
 @PostMapping("/follows/articles/{id}/replacement") public ApiResponse<Void> replacement(HttpServletRequest r,@PathVariable long id,@RequestBody Replacement b){s.acceptReplacement(actors.customer(r),id,Db.number(b.targetId));return ApiResponse.ok(null);}
 @PutMapping("/follows/{type}/{id}") public ApiResponse<Void> follow(HttpServletRequest r,@PathVariable String type,@PathVariable long id,@RequestBody State b){s.follow(actors.customer(r),type,id,b.enabled,Boolean.TRUE.equals(b.stopReminders));return ApiResponse.ok(null);}
 @GetMapping("/follows") public ApiResponse<java.util.List<Views.FollowView>> list(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.follows(actors.customer(r)),Views.FollowView.class));}
 @GetMapping("/subscription-boxes") public ApiResponse<java.util.List<Views.BoxView>> boxes(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.boxes(actors.customer(r)),Views.BoxView.class));}
}
