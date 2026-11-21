package com.sunshine.yunji.content.catalog.api;

import com.sunshine.yunji.content.catalog.application.CatalogQueryService;
import com.sunshine.yunji.content.catalog.domain.CatalogItem;
import com.sunshine.yunji.content.shared.ApiResponse;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogQueryService service;
    public CatalogController(CatalogQueryService service) { this.service = service; }
    @GetMapping("/categories") public ApiResponse<List<CatalogItem>> categories() { return ApiResponse.ok(service.categories()); }
    @GetMapping("/organizations") public ApiResponse<List<CatalogItem>> organizations() { return ApiResponse.ok(service.organizations()); }
    @GetMapping("/keywords") public ApiResponse<List<CatalogItem>> keywords(
        @RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int limit) { return ApiResponse.ok(service.keywords(q,limit)); }
}
