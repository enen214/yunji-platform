package com.sunshine.yunji.content.operation.api;
import com.sunshine.yunji.content.operation.application.ExportService;
import com.sunshine.yunji.content.shared.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExportController {
 private final ExportService service;
 public ExportController(ExportService service){this.service=service;}
 public record Request(String dataset,Long activityId){}
 @PostMapping("/api/v1/admin/exports") public ApiResponse<ExportService.ExportView> export(@RequestBody Request body){return ApiResponse.ok(service.export(body.dataset(),body.activityId()));}
}
