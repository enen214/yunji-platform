package com.sunshine.yunji.content.identity.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.identity.application.AccessManagement;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/access/users")
public class AccessController {
 private final AccessManagement s;public AccessController(AccessManagement s){this.s=s;}
 @GetMapping public ApiResponse<java.util.List<Views.AccessUserView>> list(){return ApiResponse.ok(ViewMapper.list(s.users(),Views.AccessUserView.class));}
 @PutMapping("/{id}/scope") public ApiResponse<Void> scope(@PathVariable long id,@RequestBody AccessManagement.Scope b){s.scope(id,b);return ApiResponse.ok(null);}
}
