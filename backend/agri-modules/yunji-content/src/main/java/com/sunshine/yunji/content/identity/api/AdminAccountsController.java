package com.sunshine.yunji.content.identity.api;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** One operational role. Foundation roles are not granted by this API. */
@RestController @RequestMapping("/api/v1/admin/accounts")
public class AdminAccountsController {
 private final Db db;private final Actors actors;
 public AdminAccountsController(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Account(String userId,String userName,String nickName,String status,int yunjiLockVersion){}
 public record Change(String userName,String nickName,String password,boolean enabled,int expectedVersion){}
 @GetMapping public ApiResponse<List<Account>> list(){actors.staff();return ApiResponse.ok(ViewMapper.list(db.rows("SELECT user_id,user_name,nick_name,status,yunji_lock_version FROM sys_user WHERE tenant_id='000000' AND user_type='sys_user' AND del_flag='0' ORDER BY user_id LIMIT 500"),Account.class));}
 @PostMapping @Transactional public ApiResponse<String> create(@RequestBody Change b){var a=actors.staff();validate(b,true);BusinessException.require(db.count("SELECT COUNT(*) FROM sys_user WHERE tenant_id='000000' AND user_name=?",b.userName())==0,409,"账号已存在");long id=db.id();db.update("INSERT INTO sys_user(user_id,tenant_id,user_name,nick_name,user_type,password,status,create_by,create_time) VALUES(?,'000000',?,?,'sys_user',?,?,?,UTC_TIMESTAMP())",id,b.userName(),b.nickName(),BCrypt.hashpw(b.password()),b.enabled()?"0":"1",a.id());db.audit(a.id(),"STAFF","ACCOUNT_CREATE","STAFF",id,Map.of("username",b.userName()));return ApiResponse.ok(Long.toString(id));}
 @PutMapping("/{id}") @Transactional public ApiResponse<Void> update(@PathVariable long id,@RequestBody Change b){var a=actors.staff();validate(b,false);db.rows("SELECT user_id FROM sys_user WHERE tenant_id='000000' AND del_flag='0' ORDER BY user_id FOR UPDATE");var old=db.one("SELECT * FROM sys_user WHERE user_id=? AND tenant_id='000000' AND user_type='sys_user' AND del_flag='0'",id);BusinessException.require(Db.number(old.get("yunjiLockVersion"))==b.expectedVersion(),409,"账号已变化，请刷新");BusinessException.require(b.enabled()||a.id()!=id,409,"不能停用自己");BusinessException.require(b.enabled()||db.count("SELECT COUNT(*) FROM sys_user WHERE tenant_id='000000' AND user_type='sys_user' AND status='0' AND del_flag='0' AND user_id<>?",id)>0,409,"至少保留一个管理员");BusinessException.require(db.count("SELECT COUNT(*) FROM sys_user WHERE tenant_id='000000' AND user_name=? AND user_id<>?",b.userName(),id)==0,409,"账号已存在");boolean reset=b.password()!=null&&!b.password().isBlank();db.update("UPDATE sys_user SET user_name=?,nick_name=?,status=?,password=?,yunji_lock_version=yunji_lock_version+1,update_by=?,update_time=UTC_TIMESTAMP() WHERE user_id=?",b.userName(),b.nickName(),b.enabled()?"0":"1",reset?BCrypt.hashpw(b.password()):old.get("password"),a.id(),id);if(!b.enabled()||reset)StpUtil.logout(id);db.audit(a.id(),"STAFF","ACCOUNT_UPDATE","STAFF",id,Map.of("enabled",b.enabled(),"passwordReset",reset));return ApiResponse.ok(null);}
 private void validate(Change b,boolean create){BusinessException.require(b.userName()!=null&&b.userName().matches("[A-Za-z][A-Za-z0-9_.-]{2,29}"),422,"账号需3至30位字母数字或_.-");Db.text(b.nickName(),30);if(create||b.password()!=null&&!b.password().isBlank())BusinessException.require(b.password()!=null&&b.password().length()>=12&&b.password().length()<=64&&b.password().matches("(?s).*[A-Za-z].*")&&b.password().matches("(?s).*\\d.*"),422,"密码需12至64位且包含字母和数字");}
}
