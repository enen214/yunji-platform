package com.sunshine.yunji.content.notification.api;

import com.sunshine.yunji.content.notification.application.*;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.sunshine.yunji.content.shared.contract.ViewMapper;

@RestController @Validated
public class ReminderController {
    private final ReminderTrackingService tracking; private final ReminderBatchService batches; private final ReminderWorker worker; private final Actors actors;
    public ReminderController(ReminderTrackingService tracking,ReminderBatchService batches,ReminderWorker worker,Actors actors){this.tracking=tracking;this.batches=batches;this.worker=worker;this.actors=actors;}
    @GetMapping("/api/v1/me/reminders/settings") public ApiResponse<ReminderViews.Settings> settings(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(tracking.settings(actors.customer(r)),ReminderViews.Settings.class));}
    @PutMapping("/api/v1/me/reminders/settings") public ApiResponse<ReminderViews.Settings> preference(HttpServletRequest r,@Valid @RequestBody ReminderTrackingService.Preference b){return ApiResponse.ok(ViewMapper.of(tracking.preference(actors.customer(r),b),ReminderViews.Settings.class));}
    @PostMapping("/api/v1/me/reminders/wecom-link") public ApiResponse<ReminderViews.Settings> wecomLink(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(tracking.wecomLink(actors.customer(r)),ReminderViews.Settings.class));}
    @PostMapping("/api/v1/me/reminders/wecom-request") public ApiResponse<ReminderViews.Settings> requestWecomContact(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(tracking.requestWecomContact(actors.customer(r)),ReminderViews.Settings.class));}
    @GetMapping("/api/v1/me/reminders/rules") public ApiResponse<ReminderViews.Rules> rules(HttpServletRequest r,@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="") String mode,@RequestParam(defaultValue="") @Pattern(regexp="|ARTICLE|KEYWORD") String targetType,@RequestParam(required=false) @Positive Long targetId,@RequestParam(defaultValue="false") boolean enabledOnly){return ApiResponse.ok(ViewMapper.of(tracking.list(actors.customer(r),page,size,mode,targetType,targetId,enabledOnly),ReminderViews.Rules.class));}
    @PostMapping("/api/v1/me/reminders/rules") public ApiResponse<ReminderViews.Tracking> create(HttpServletRequest r,@Valid @RequestBody ReminderTrackingService.Tracking b){return ApiResponse.ok(ViewMapper.of(tracking.save(actors.customer(r),null,b),ReminderViews.Tracking.class));}
    public record Daily(@Pattern(regexp="ARTICLE|KEYWORD") @NotBlank String targetType,@Positive long targetId){}
    @PostMapping("/api/v1/me/reminders/daily") public ApiResponse<ReminderViews.Tracking> daily(HttpServletRequest r,@Valid @RequestBody Daily b){return ApiResponse.ok(ViewMapper.of(tracking.daily(actors.customer(r),b.targetType(),b.targetId()),ReminderViews.Tracking.class));}
    @GetMapping("/api/v1/me/reminders/rules/{id}") public ApiResponse<ReminderViews.Tracking> detail(HttpServletRequest r,@PathVariable @Positive long id){return ApiResponse.ok(ViewMapper.of(tracking.detail(actors.customer(r),id),ReminderViews.Tracking.class));}
    @PutMapping("/api/v1/me/reminders/rules/{id}") public ApiResponse<ReminderViews.Tracking> edit(HttpServletRequest r,@PathVariable @Positive long id,@Valid @RequestBody ReminderTrackingService.Tracking b){return ApiResponse.ok(ViewMapper.of(tracking.save(actors.customer(r),id,b),ReminderViews.Tracking.class));}
    @GetMapping("/api/v1/me/reminders/keywords") public ApiResponse<java.util.List<ReminderTrackingService.KeywordSummary>> keywords(HttpServletRequest r){return ApiResponse.ok(tracking.keywords(actors.customer(r)));}
    @GetMapping("/api/v1/me/reminders/changes") public ApiResponse<ReminderViews.Changes> changes(HttpServletRequest r,@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="") @Pattern(regexp="|DAILY|INSTANT") String mode){return ApiResponse.ok(ViewMapper.of(tracking.changes(actors.customer(r),page,size,mode),ReminderViews.Changes.class));}
    @DeleteMapping("/api/v1/me/reminders/changes/{id}") public ApiResponse<Void> dismiss(HttpServletRequest r,@PathVariable @Positive long id){tracking.dismiss(actors.customer(r),id);return ApiResponse.ok(null);}
    @GetMapping("/api/v1/me/reminders/batches") public ApiResponse<ReminderViews.Batches> batches(HttpServletRequest r,@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(ViewMapper.of(batches.batches(actors.customer(r),page,size,null,null),ReminderViews.Batches.class));}
    @PostMapping("/api/local/reminders/authorization") public ApiResponse<ReminderViews.Settings> authorize(HttpServletRequest r,@RequestHeader(value="X-Local-Secret",required=false) String secret,@Valid @RequestBody ReminderTrackingService.Authorization b){return ApiResponse.ok(ViewMapper.of(tracking.authorize(actors.customer(r),b,secret),ReminderViews.Settings.class));}
    @GetMapping("/api/v1/admin/reminders/settings") public ApiResponse<ReminderViews.Control> control(){return ApiResponse.ok(ViewMapper.of(tracking.control(),ReminderViews.Control.class));}
    @PutMapping("/api/v1/admin/reminders/settings") public ApiResponse<ReminderViews.Control> configure(@Valid @RequestBody ReminderTrackingService.Control b){return ApiResponse.ok(ViewMapper.of(tracking.configure(b),ReminderViews.Control.class));}
    @GetMapping("/api/v1/admin/reminders/batches") public ApiResponse<ReminderViews.Batches> adminBatches(@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="") @Size(max=30) String state,@RequestParam(defaultValue="") @Size(max=30) String channel){return ApiResponse.ok(ViewMapper.of(batches.batches(null,page,size,state,channel),ReminderViews.Batches.class));}
    @GetMapping("/api/v1/admin/reminders/rules") public ApiResponse<ReminderViews.Rules> adminRules(@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(ViewMapper.of(batches.rules(page,size),ReminderViews.Rules.class));}
    @GetMapping("/api/v1/admin/reminders/statistics") public ApiResponse<ReminderViews.Statistics> statistics(){return ApiResponse.ok(ViewMapper.of(batches.statistics(),ReminderViews.Statistics.class));}
    @PostMapping("/api/v1/admin/reminders/tick") public ApiResponse<ReminderViews.Tick> tick(){actors.staff().require("yunji:notification:manage");return ApiResponse.ok(ViewMapper.of(worker.tick(),ReminderViews.Tick.class));}
    public record Pause(@Min(0) int expectedVersion,@NotBlank @Size(min=10,max=500) String reason){}
    @PostMapping("/api/v1/admin/reminders/rules/{id}/pause") public ApiResponse<ReminderViews.Tracking> pause(@PathVariable @Positive long id,@Valid @RequestBody Pause b){return ApiResponse.ok(ViewMapper.of(tracking.pauseByStaff(id,b.expectedVersion(),b.reason()),ReminderViews.Tracking.class));}
    public record Reconcile(@NotBlank String state,@NotBlank @Size(min=10,max=1000) String evidence){}
    public record Evidence(@NotBlank @Size(min=10,max=1000) String evidence){}
    @PostMapping("/api/v1/admin/reminders/batches/{id}/retry") public ApiResponse<Void> retry(@PathVariable @Positive long id,@Valid @RequestBody Evidence b){batches.retry(id,b.evidence());return ApiResponse.ok(null);}
    @PostMapping("/api/v1/admin/reminders/batches/{id}/reconcile") public ApiResponse<ReminderViews.Batch> reconcile(@PathVariable @Positive long id,@Valid @RequestBody Reconcile b){return ApiResponse.ok(ViewMapper.of(batches.reconcile(id,b.state(),b.evidence()),ReminderViews.Batch.class));}
}
