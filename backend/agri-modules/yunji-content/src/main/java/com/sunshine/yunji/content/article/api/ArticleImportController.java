package com.sunshine.yunji.content.article.api;
import com.sunshine.yunji.content.article.application.ArticleImportService;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController @RequestMapping("/api/v1/admin/articles")
public class ArticleImportController {
    private final ArticleImportService service;
    public ArticleImportController(ArticleImportService service){this.service=service;}
    public record Text(String content) {}
    @PostMapping("/import-text") public ApiResponse<ArticleImportService.Result> text(@RequestHeader("Idempotency-Key") String key,@RequestBody Text body){return ApiResponse.ok(service.run(key,body.content()));}
    @PostMapping(value="/import",consumes="multipart/form-data") public ApiResponse<ArticleImportService.Result> file(@RequestHeader("Idempotency-Key") String key,@RequestPart("file") MultipartFile file){
        BusinessException.require(file.getOriginalFilename()!=null&&file.getOriginalFilename().toLowerCase(java.util.Locale.ROOT).endsWith(".json"),422,"仅支持.json文件");
        BusinessException.require(file.getSize()<=ArticleImportService.MAX_BYTES,413,"文件超过5MiB");
        try{return ApiResponse.ok(service.run(key,StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(file.getBytes())).toString()));}catch(java.io.IOException e){throw new BusinessException(422,"文件需为UTF-8文本");}
    }
    @GetMapping("/import-batches/{id}") public ApiResponse<ArticleImportService.Result> status(@PathVariable long id){return ApiResponse.ok(service.result(id));}
}
