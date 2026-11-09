package com.sunshine.yunji.content.catalog.api;
import com.sunshine.yunji.content.catalog.application.DiscoveryService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @Validated
public class DiscoveryController {
    public record Board(String id,String name,String description,long articleCount){}
    public record Keyword(String id,String name,Long searchers){}
    public record History(String query,String searchedAt){}
    public record Query(@NotBlank @Size(max=100) String query){}
    public record State(boolean enabled){}
    public record Summary(long unreadCount,long sentToday,String timezone){}
    public record Activity(String id,String title,String eventStart,String eventEnd){}
    public record Customer(String id,String nickname,String status,String createdAt,String lastLoginAt,String lastActiveAt,long activeDays30,Double averageIntervalDays){}
    public record Customers(long total,List<Customer> items){}
    private final DiscoveryService s; private final Actors actors;
    public DiscoveryController(DiscoveryService s,Actors actors){this.s=s;this.actors=actors;}
    @GetMapping("/api/v1/catalog/boards") public ApiResponse<List<Board>> boards(){return ApiResponse.ok(ViewMapper.list(s.boards(),Board.class));}
    @GetMapping("/api/v1/catalog/labels/{id}/keywords") public ApiResponse<List<Keyword>> keywords(@PathVariable @Positive long id){return ApiResponse.ok(ViewMapper.list(s.keywords(id),Keyword.class));}
    @GetMapping("/api/v1/search/hot") public ApiResponse<List<Keyword>> hot(){return ApiResponse.ok(ViewMapper.list(s.hot(),Keyword.class));}
    public record Popular(String id,String title,long clicks) {}
    @GetMapping("/api/v1/search/popular") public ApiResponse<List<Popular>> popular(){return ApiResponse.ok(ViewMapper.list(s.popular(),Popular.class));}
    @DeleteMapping("/api/v1/me/search-history/item") public ApiResponse<Void> remove(HttpServletRequest r,@RequestParam String query){s.removeHistory(actors.customer(r),query);return ApiResponse.ok(null);}
    @GetMapping("/api/v1/me/search-history") public ApiResponse<List<History>> history(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.history(actors.customer(r)),History.class));}
    @PostMapping("/api/v1/me/search-history") public ApiResponse<Void> remember(HttpServletRequest r,@Valid @RequestBody Query b){s.remember(actors.customer(r),b.query());return ApiResponse.ok(null);}
    @DeleteMapping("/api/v1/me/search-history") public ApiResponse<Void> clear(HttpServletRequest r){s.clear(actors.customer(r));return ApiResponse.ok(null);}
    @GetMapping("/api/v1/me/discovery-summary") public ApiResponse<Summary> summary(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(s.summary(actors.customer(r)),Summary.class));}
    @PutMapping("/api/v1/me/activity-favorites/{id}") public ApiResponse<Void> favorite(HttpServletRequest r,@PathVariable @Positive long id,@RequestBody State b){s.favorite(actors.customer(r),id,b.enabled());return ApiResponse.ok(null);}
    @GetMapping("/api/v1/me/activity-favorites") public ApiResponse<List<Activity>> favorites(HttpServletRequest r){return ApiResponse.ok(ViewMapper.list(s.favorites(actors.customer(r)),Activity.class));}
    @GetMapping("/api/v1/admin/customers") public ApiResponse<Customers> customers(@RequestParam(defaultValue="1") @Min(1) @Max(10000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="") @Size(max=100) String q){return ApiResponse.ok(ViewMapper.of(s.customers(page,size,q),Customers.class));}
}
