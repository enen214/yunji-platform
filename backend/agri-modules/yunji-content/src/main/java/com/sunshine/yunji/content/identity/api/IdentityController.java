package com.sunshine.yunji.content.identity.api;

import com.sunshine.yunji.content.identity.application.*;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.sunshine.yunji.content.identity.infrastructure.WechatGateway;
import com.sunshine.yunji.content.shared.contract.*;

@RestController
public class IdentityController {
    private final IdentityService service;private final Actors actors;private final WechatGateway wechat;
    public IdentityController(IdentityService service,Actors actors,WechatGateway wechat){this.service=service;this.actors=actors;this.wechat=wechat;}
    public record CodeRequest(@Positive long userId,@NotBlank String appId){}
    public record LoginRequest(@NotBlank @Size(max=100) String code,@NotBlank @Size(max=100) String appId){}
    public record NativeWechatLoginRequest(@NotBlank @Size(max=100) String code,@NotBlank @Size(max=100) String appId,@Size(min=1,max=100) String phoneCode){}
    public record ProfileRequest(@NotBlank @Size(max=100) String nickname){}
    @PostMapping("/api/local/login-codes") public ApiResponse<Views.CodeView> code(@RequestHeader(value="X-Local-Secret",required=false) String secret,@Valid @RequestBody CodeRequest b){return ApiResponse.ok(ViewMapper.of(service.issueCode(secret,b.userId,b.appId),Views.CodeView.class));}
    @PostMapping("/api/v1/auth/customer/local") public ApiResponse<Views.CustomerSessionView> login(@Valid @RequestBody LoginRequest b){return ApiResponse.ok(ViewMapper.of(service.localLogin(b.code,b.appId),Views.CustomerSessionView.class));}
    @PostMapping("/api/v1/auth/customer/wechat") public ApiResponse<Views.CustomerSessionView> wechat(@Valid @RequestBody LoginRequest b){return ApiResponse.ok(ViewMapper.of(service.isLocal()?service.localWechat(b.code,b.appId):service.providerLogin(wechat.exchange(b.code,b.appId)),Views.CustomerSessionView.class));}
    /** Always real, including in a local workbench. Synthetic codes never enter the local-login branch here. */
    @PostMapping("/api/v1/auth/customer/wechat/native") public ApiResponse<Views.CustomerSessionView> nativeWechat(@Valid @RequestBody NativeWechatLoginRequest b){return ApiResponse.ok(ViewMapper.of(service.providerLogin(wechat.exchange(b.code,b.appId,b.phoneCode)),Views.CustomerSessionView.class));}
    @PostMapping("/api/v1/auth/customer/logout") public ApiResponse<Void> logout(HttpServletRequest r){actors.customer(r);service.logout(r.getHeader("Authorization").substring(7));return ApiResponse.ok(null);}
    @GetMapping("/api/v1/me/profile") public ApiResponse<Views.ProfileView> profile(HttpServletRequest r){return ApiResponse.ok(ViewMapper.of(service.profile(actors.customer(r)),Views.ProfileView.class));}
    @PutMapping("/api/v1/me/profile") public ApiResponse<Views.ProfileView> profile(HttpServletRequest r,@Valid @RequestBody ProfileRequest b){return ApiResponse.ok(ViewMapper.of(service.profile(actors.customer(r),b.nickname()),Views.ProfileView.class));}
    @PostMapping(value="/api/v1/me/avatar",consumes="multipart/form-data") public ApiResponse<Views.ProfileView> avatar(HttpServletRequest r,@RequestPart org.springframework.web.multipart.MultipartFile file){return ApiResponse.ok(ViewMapper.of(service.avatar(actors.customer(r),file),Views.ProfileView.class));}
    @GetMapping("/api/v1/me/avatar") public org.springframework.http.ResponseEntity<byte[]> avatar(HttpServletRequest r){var d=service.avatar(actors.customer(r));return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(d.mediaType())).header("Cache-Control","private,max-age=300").body(d.bytes());}
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class) public org.springframework.http.ResponseEntity<ApiResponse<Void>> missingPart(){return org.springframework.http.ResponseEntity.unprocessableEntity().body(new ApiResponse<>(422,"缺少上传文件",null));}
    @GetMapping("/api/v1/admin/session") public ApiResponse<Views.StaffSessionView> staff(){var a=actors.staff();return ApiResponse.ok(new Views.StaffSessionView(Long.toString(a.id()),Long.toString(a.department()),"STAFF",a.admin(),a.permissions().stream().sorted().toList()));}
}
