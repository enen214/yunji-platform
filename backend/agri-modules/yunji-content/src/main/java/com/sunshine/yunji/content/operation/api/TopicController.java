package com.sunshine.yunji.content.operation.api;
import com.sunshine.yunji.content.operation.application.TopicService;
import com.sunshine.yunji.content.shared.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @Validated
public class TopicController {
 private final TopicService s;public TopicController(TopicService s){this.s=s;}
 @GetMapping("/api/v1/topics") public ApiResponse<List<TopicService.Topic>> topics(){return ApiResponse.ok(s.list(false));}
 @GetMapping("/api/v1/topics/{id}") public ApiResponse<TopicService.Topic> topic(@PathVariable @Positive long id){return ApiResponse.ok(s.detail(id,false));}
 @GetMapping("/api/v1/admin/operation/topics") public ApiResponse<List<TopicService.Topic>> admin(){return ApiResponse.ok(s.list(true));}
 @GetMapping("/api/v1/admin/operation/topics/{id}") public ApiResponse<TopicService.Topic> detail(@PathVariable @Positive long id){return ApiResponse.ok(s.detail(id,true));}
 @PostMapping("/api/v1/admin/operation/topics") public ApiResponse<TopicService.Topic> create(@Valid @RequestBody TopicService.Draft b){return ApiResponse.ok(s.save(null,b));}
 @PutMapping("/api/v1/admin/operation/topics/{id}") public ApiResponse<TopicService.Topic> edit(@PathVariable @Positive long id,@Valid @RequestBody TopicService.Draft b){return ApiResponse.ok(s.save(id,b));}
 @DeleteMapping("/api/v1/admin/operation/topics/{id}") public ApiResponse<Void> delete(@PathVariable @Positive long id,@RequestParam @Min(0) int expectedVersion){s.delete(id,expectedVersion);return ApiResponse.ok(null);}
}
