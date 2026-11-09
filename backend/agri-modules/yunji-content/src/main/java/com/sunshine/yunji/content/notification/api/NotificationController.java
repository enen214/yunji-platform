package com.sunshine.yunji.content.notification.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.notification.application.NotificationService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
public class NotificationController {
 private final NotificationService s;private final Actors actors;
 public NotificationController(NotificationService s,Actors actors){this.s=s;this.actors=actors;}
 @GetMapping("/api/v1/me/notifications/rules") public ApiResponse<java.util.List<Views.RuleView>> rules(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.rules(actors.customer(r)),Views.RuleView.class));}
 @PostMapping("/api/v1/me/notifications/rules") public ApiResponse<Views.RuleView> create(HttpServletRequest r,@RequestBody NotificationService.Rule b){return ApiResponse.ok(ViewMapper.of(s.rule(actors.customer(r),null,b),Views.RuleView.class));}
 @PutMapping("/api/v1/me/notifications/rules/{id}") public ApiResponse<Views.RuleView> edit(HttpServletRequest r,@PathVariable long id,@RequestBody NotificationService.Rule b){return ApiResponse.ok(ViewMapper.of(s.rule(actors.customer(r),id,b),Views.RuleView.class));}
 @DeleteMapping("/api/v1/me/notifications/rules/{id}") public ApiResponse<Void> cancel(HttpServletRequest r,@PathVariable long id){s.cancel(actors.customer(r),id);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/me/notifications/status") public ApiResponse<Views.NotificationStatusView> status(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(s.status(actors.customer(r)),Views.NotificationStatusView.class));}
 @GetMapping("/api/v1/me/notifications/deliveries") public ApiResponse<java.util.List<Views.NotificationDeliverySummaryView>> deliveries(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.deliveries(actors.customer(r)),Views.NotificationDeliverySummaryView.class));}
 @PostMapping("/api/local/notifications/authorization") public ApiResponse<Views.NotificationStatusView> authorize(HttpServletRequest r,@RequestHeader(value="X-Local-Secret",required=false) String secret,@RequestBody NotificationService.Authorization b){return ApiResponse.ok(ViewMapper.of(s.authorizeLocal(actors.customer(r),b,secret),Views.NotificationStatusView.class));}
 @PostMapping("/api/v1/admin/notifications/tick") public ApiResponse<Views.TickView> tick(){return ApiResponse.ok(ViewMapper.of(s.tick(),Views.TickView.class));}
 public record Reconcile(String outcome,String evidence){}
 @PostMapping("/api/v1/admin/notifications/{id}/reconcile") public ApiResponse<Views.DeliveryView> reconcile(@PathVariable long id,@RequestBody Reconcile b){return ApiResponse.ok(ViewMapper.of(s.reconcile(id,b.outcome,b.evidence),Views.DeliveryView.class));}
 @GetMapping("/api/v1/admin/notifications/ledger") public ApiResponse<java.util.List<Views.DeliveryView>> ledger(){return ApiResponse.ok(ViewMapper.list(s.ledger(),Views.DeliveryView.class));}
 public record Capabilities(boolean localSimulation,boolean realProviderReady){}
 @GetMapping("/api/v1/admin/notifications/rules") public ApiResponse<java.util.List<Views.RuleView>> adminRules(){return ApiResponse.ok(ViewMapper.list(s.adminRules(),Views.RuleView.class));}
 @GetMapping("/api/v1/admin/notifications/capabilities") public ApiResponse<Capabilities> capabilities(){return ApiResponse.ok(ViewMapper.of(s.capabilities(),Capabilities.class));}
}
