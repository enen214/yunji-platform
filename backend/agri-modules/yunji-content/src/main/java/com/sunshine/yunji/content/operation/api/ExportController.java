package com.sunshine.yunji.content.operation.api;
import com.sunshine.yunji.content.operation.application.ExportService;
import com.sunshine.yunji.content.shared.ApiResponse;
import com.sunshine.yunji.content.shared.BusinessException;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExportController {
 private final ExportService service;
 public ExportController(ExportService service){this.service=service;}
 public record Request(String dataset,Long activityId){}
 @PostMapping("/api/v1/admin/exports") public ApiResponse<ExportService.ExportView> export(@RequestBody Request body){BusinessException.require(body!=null,422,"导出参数不能为空");return ApiResponse.ok(service.export(body.dataset(),body.activityId()));}
 @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) public org.springframework.http.ResponseEntity<ApiResponse<Void>> unreadable(){return org.springframework.http.ResponseEntity.unprocessableEntity().body(new ApiResponse<>(422,"请求正文不符合约束",null));}
}
