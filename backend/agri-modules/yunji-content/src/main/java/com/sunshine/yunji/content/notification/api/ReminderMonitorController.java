package com.sunshine.yunji.content.notification.api;
import com.sunshine.yunji.content.notification.application.ReminderMonitor;
import com.sunshine.yunji.content.shared.ApiResponse;
import org.springframework.web.bind.annotation.*;
@RestController
public class ReminderMonitorController {
 private final ReminderMonitor monitor;
 public ReminderMonitorController(ReminderMonitor monitor){this.monitor=monitor;}
 @GetMapping("/api/v1/admin/reminders/health") public ApiResponse<ReminderMonitor.Health> health(){return ApiResponse.ok(monitor.health());}
}
