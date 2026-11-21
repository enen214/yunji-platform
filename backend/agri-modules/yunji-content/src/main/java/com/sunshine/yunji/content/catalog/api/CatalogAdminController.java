package com.sunshine.yunji.content.catalog.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.catalog.application.CatalogCommands;
import com.sunshine.yunji.content.shared.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/catalog")
public class CatalogAdminController {
 private final CatalogCommands service;
 public CatalogAdminController(CatalogCommands service){this.service=service;}
 @GetMapping("/{kind}") public ApiResponse<java.util.List<Views.CatalogEntryView>> list(@PathVariable String kind){return ApiResponse.ok(ViewMapper.list(service.list(kind),Views.CatalogEntryView.class));}
 @PostMapping("/{kind}") public ApiResponse<Views.CatalogEntryView> create(@PathVariable String kind,@Valid @RequestBody CatalogCommands.Item b){return ApiResponse.ok(ViewMapper.of(service.save(kind,null,b),Views.CatalogEntryView.class));}
 @PutMapping("/{kind}/{id}") public ApiResponse<Views.CatalogEntryView> edit(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody CatalogCommands.Item b){return ApiResponse.ok(ViewMapper.of(service.save(kind,id,b),Views.CatalogEntryView.class));}
 @DeleteMapping("/{kind}/{id}") public ApiResponse<Void> delete(@PathVariable String kind,@PathVariable long id){service.delete(kind,id);return ApiResponse.ok(null);}
 public record Alias(String alias){}public record Blacklist(String keyword,String reason){}
 @PostMapping("/keywords/{id}/aliases") public ApiResponse<Views.AliasView> alias(@PathVariable long id,@RequestBody Alias b){return ApiResponse.ok(ViewMapper.of(service.alias(id,b.alias),Views.AliasView.class));}
 @PostMapping("/blacklist") public ApiResponse<Void> blacklist(@RequestBody Blacklist b){service.blacklist(b.keyword,b.reason);return ApiResponse.ok(null);}
}
