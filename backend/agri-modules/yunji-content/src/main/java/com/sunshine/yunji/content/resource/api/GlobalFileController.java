package com.sunshine.yunji.content.resource.api;
import com.sunshine.yunji.content.resource.application.GlobalFileLibrary;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
public class GlobalFileController {
 private final GlobalFileLibrary s;private final Actors actors;
 public GlobalFileController(GlobalFileLibrary s,Actors actors){this.s=s;this.actors=actors;}
 public record Version(int versionNo,String note,String createdAt,String displayName,long sizeBytes,String mediaType,String sha256){}
 public record Activity(String id,String title){}
 public record File(String id,String name,String description,boolean enabled,int currentVersion,int lockVersion,Integer activityCount,Integer downloadCount,List<Version> versions,List<Activity> activities){}
 public record Link(String url,String path,Integer expiresIn,String ticket){}
 public record Available(String id,String name,String description,int currentVersion,boolean showHistory,int downloadCount){}
 public record Reference(String activityId,String fileId,boolean showHistory,int sortOrder){}
 public record References(List<Reference> files,List<String> reviewIds){}
 @GetMapping("/api/v1/admin/file-library") public ApiResponse<List<File>> list(){return ApiResponse.ok(ViewMapper.list(s.list(),File.class));}
 @GetMapping("/api/v1/admin/file-library/{id}") public ApiResponse<File> detail(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.detail(id),File.class));}
 @PostMapping(value="/api/v1/admin/file-library",consumes="multipart/form-data") public ApiResponse<File> upload(@RequestPart MultipartFile file,@RequestParam String name,@RequestParam(defaultValue="") String description,@RequestParam(defaultValue="") String note){return ApiResponse.ok(ViewMapper.of(s.upload(null,file,name,description,note,0),File.class));}
 @PostMapping(value="/api/v1/admin/file-library/{id}/versions",consumes="multipart/form-data") public ApiResponse<File> version(@PathVariable long id,@RequestPart MultipartFile file,@RequestParam String name,@RequestParam(defaultValue="") String description,@RequestParam(defaultValue="") String note,@RequestParam int expectedVersion){return ApiResponse.ok(ViewMapper.of(s.upload(id,file,name,description,note,expectedVersion),File.class));}
 @PutMapping("/api/v1/admin/file-library/{id}") public ApiResponse<Void> edit(@PathVariable long id,@RequestBody GlobalFileLibrary.Edit b){s.edit(id,b);return ApiResponse.ok(null);}
 @DeleteMapping("/api/v1/admin/file-library/{id}") public ApiResponse<Void> delete(@PathVariable long id,@RequestParam int expectedVersion){s.delete(id,expectedVersion);return ApiResponse.ok(null);}
 @GetMapping("/api/v1/admin/activities/{id}/functions") public ApiResponse<References> refs(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.references(id),References.class));}
 @PutMapping("/api/v1/admin/activities/{id}/functions") public ApiResponse<Void> refs(@PathVariable long id,@RequestBody GlobalFileLibrary.References b){s.references(id,b);return ApiResponse.ok(null);}
 public record Complete(String ticket){}
 @PostMapping("/api/v1/me/library-downloads/complete") public ApiResponse<Void> complete(HttpServletRequest request,@RequestBody Complete b){s.completed(actors.customer(request),b.ticket());return ApiResponse.ok(null);}
 @GetMapping("/api/v1/activities/{id}/downloads") public ApiResponse<List<Available>> available(@PathVariable long id){return ApiResponse.ok(ViewMapper.list(s.available(id),Available.class));}
 @GetMapping("/api/v1/activities/{id}/downloads/{file}/versions") public ApiResponse<List<Version>> versions(@PathVariable long id,@PathVariable long file){return ApiResponse.ok(ViewMapper.list(s.history(id,file),Version.class));}
 @PostMapping("/api/v1/activities/{id}/downloads/{file}") public ApiResponse<Link> download(HttpServletRequest request,@PathVariable long id,@PathVariable long file,@RequestParam(required=false) Integer version){return ApiResponse.ok(ViewMapper.of(s.download(actors.customer(request),id,file,version),Link.class));}
}
