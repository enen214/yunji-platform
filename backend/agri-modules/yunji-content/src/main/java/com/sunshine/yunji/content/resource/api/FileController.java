package com.sunshine.yunji.content.resource.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.resource.application.FileService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController
public class FileController {
 private final FileService s;private final Actors actors;
 public FileController(FileService s,Actors actors){this.s=s;this.actors=actors;}
 @PostMapping(value="/api/v1/me/files",consumes="multipart/form-data") public ApiResponse<Views.FileView> upload(HttpServletRequest r,@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="PRIVATE") String privacy){return ApiResponse.ok(ViewMapper.of(s.upload(new FileService.Principal("CUSTOMER",actors.customer(r)),file,privacy),Views.FileView.class));}
 @PostMapping(value="/api/v1/admin/files",consumes="multipart/form-data") public ApiResponse<Views.FileView> adminUpload(@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="PRIVATE") String privacy){return ApiResponse.ok(ViewMapper.of(s.upload(s.staff(),file,privacy),Views.FileView.class));}
 @GetMapping("/api/v1/admin/files") public ApiResponse<java.util.List<Views.FileView>> list(){return ApiResponse.ok(ViewMapper.list(s.list(),Views.FileView.class));}
 @PostMapping("/api/v1/admin/files/cleanup") public ApiResponse<Views.CleanupView> cleanup(@RequestParam(defaultValue="false") boolean dryRun,@RequestParam(defaultValue="100") int limit){return ApiResponse.ok(ViewMapper.of(s.cleanup(dryRun,limit),Views.CleanupView.class));}
 @DeleteMapping("/api/v1/admin/files/{id}") public ApiResponse<Void> delete(@PathVariable long id){s.delete(id);return ApiResponse.ok(null);}
 @PostMapping("/api/v1/me/files/{id}/tickets") public ApiResponse<Views.TicketView> ticket(HttpServletRequest r,@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.ticket(new FileService.Principal("CUSTOMER",actors.customer(r)),id),Views.TicketView.class));}
 @PostMapping("/api/v1/admin/files/{id}/tickets") public ApiResponse<Views.TicketView> adminTicket(@PathVariable long id){var a=actors.staff();return ApiResponse.ok(ViewMapper.of(s.ticket(new FileService.Principal("STAFF",a.id()),id),Views.TicketView.class));}
 @GetMapping("/api/v1/files/download/{token}") public ResponseEntity<byte[]> download(HttpServletRequest r,@PathVariable String token){
  FileService.Principal p;
  try{p=new FileService.Principal("CUSTOMER",actors.customer(r));}catch(BusinessException e){p=new FileService.Principal("STAFF",actors.staff().id());}
  var f=s.download(p,token);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store").header("Content-Disposition",ContentDisposition.attachment().filename(f.name(),StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(f.mediaType())).body(f.bytes());
 }
}
