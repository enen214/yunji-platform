package com.sunshine.yunji.content.discovery.api;
import com.sunshine.yunji.content.discovery.application.HotArchive;
import com.sunshine.yunji.content.shared.ApiResponse;
import org.springframework.web.bind.annotation.*;
@RestController
public class HotArchiveController {
 private final HotArchive archive;public HotArchiveController(HotArchive archive){this.archive=archive;}
 public record Result(int archived){}
 @PostMapping("/api/v1/admin/articles/archive-hot") public ApiResponse<Result> archive(){return ApiResponse.ok(new Result(archive.archive()));}
}
