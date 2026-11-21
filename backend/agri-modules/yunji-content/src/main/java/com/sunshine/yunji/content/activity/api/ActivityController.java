package com.sunshine.yunji.content.activity.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.activity.application.ActivityService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
public class ActivityController {
 private final ActivityService s;private final Actors actors;
 public ActivityController(ActivityService s,Actors actors){this.s=s;this.actors=actors;}
 @GetMapping("/api/v1/activities") public ApiResponse<java.util.List<Views.PublicActivityView>> list(){return ApiResponse.ok(ViewMapper.list(s.list(),Views.PublicActivityView.class));}
 @GetMapping("/api/v1/activities/{id}") public ApiResponse<Views.PublicActivityView> detail(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.detail(id),Views.PublicActivityView.class));}
 @PostMapping("/api/v1/me/activities/{id}/registration") public ApiResponse<Views.RegistrationView> register(HttpServletRequest r,@PathVariable long id,@RequestBody ActivityService.Registration b){return ApiResponse.ok(ViewMapper.of(s.register(actors.customer(r),id,b),Views.RegistrationView.class));}
 @DeleteMapping("/api/v1/me/activities/{id}/registration") public ApiResponse<Void> cancel(HttpServletRequest r,@PathVariable long id){s.cancel(actors.customer(r),id);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/me/registrations") public ApiResponse<java.util.List<Views.RegistrationView>> mine(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.mine(actors.customer(r)),Views.RegistrationView.class));}
 @PostMapping("/api/v1/admin/activities") public ApiResponse<Views.ActivityView> create(@Valid @RequestBody ActivityService.Config b){return ApiResponse.ok(ViewMapper.of(s.save(null,b),Views.ActivityView.class));}
 @PutMapping("/api/v1/admin/activities/{id}") public ApiResponse<Views.ActivityView> edit(@PathVariable long id,@Valid @RequestBody ActivityService.Config b){return ApiResponse.ok(ViewMapper.of(s.save(id,b),Views.ActivityView.class));}
 @GetMapping("/api/v1/admin/activities") public ApiResponse<java.util.List<Views.ActivityView>> adminList(){return ApiResponse.ok(ViewMapper.list(s.adminList(),Views.ActivityView.class));}
 @GetMapping("/api/v1/admin/activities/{id}") public ApiResponse<Views.ActivityView> adminDetail(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.adminDetail(id),Views.ActivityView.class));}
}
