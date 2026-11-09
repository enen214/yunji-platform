package com.sunshine.yunji.content.analytics.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.analytics.application.AnalyticsService;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/admin/analytics")
public class AnalyticsController {
 private final AnalyticsService s;public AnalyticsController(AnalyticsService s){this.s=s;}
 @GetMapping("/users") public ApiResponse<AnalyticsService.UserTotals> users(){return ApiResponse.ok(s.users());}
 @GetMapping public ApiResponse<Views.AnalyticsView> overview(){return ApiResponse.ok(ViewMapper.of(s.overview(),Views.AnalyticsView.class));}
 public record Day(LocalDate date){}
 @PostMapping("/aggregate") public ApiResponse<Views.AggregateView> aggregate(@RequestBody Day b){return ApiResponse.ok(ViewMapper.of(s.aggregate(b.date),Views.AggregateView.class));}
}
