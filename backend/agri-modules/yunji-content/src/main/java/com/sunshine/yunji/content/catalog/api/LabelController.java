package com.sunshine.yunji.content.catalog.api;

import com.sunshine.yunji.content.catalog.application.LabelService;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class LabelController {
    public record LabelView(String id,String categoryId,String parentId,String name,String status,int sortOrder,int lockVersion,long articleCount){}
    private final LabelService service;
    public LabelController(LabelService service){this.service=service;}
    @GetMapping("/api/v1/catalog/labels") public ApiResponse<List<LabelView>> list(){return ApiResponse.ok(ViewMapper.list(service.list(false),LabelView.class));}
    @GetMapping("/api/v1/admin/catalog/labels") public ApiResponse<List<LabelView>> admin(){return ApiResponse.ok(ViewMapper.list(service.list(true),LabelView.class));}
    @PostMapping("/api/v1/admin/catalog/labels") public ApiResponse<LabelView> create(@Valid @RequestBody LabelService.Input b){return ApiResponse.ok(ViewMapper.of(service.save(null,b),LabelView.class));}
    @PutMapping("/api/v1/admin/catalog/labels/{id}") public ApiResponse<LabelView> edit(@PathVariable long id,@Valid @RequestBody LabelService.Input b){return ApiResponse.ok(ViewMapper.of(service.save(id,b),LabelView.class));}
}
