package com.sunshine.yunji.content.identity.application;

import com.sunshine.common.satoken.utils.LoginHelper;
import cn.dev33.satoken.stp.StpUtil;
import com.sunshine.yunji.content.shared.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class Actors {
    private final Db db;
    public Actors(Db db){this.db=db;}
    public record Staff(long id,long department,boolean admin,Set<String> permissions) {
        public void require(String permission){BusinessException.require(admin||permissions.contains(permission),403,"没有操作权限");}
    }
    public Staff staff(){
        try {
            StpUtil.checkLogin();var user=LoginHelper.getLoginUser();
            BusinessException.require(user!=null&&"000000".equals(user.getTenantId()),403,"租户不可访问");
            BusinessException.require("sys_user".equals(user.getUserType()),403,"账号主体不可访问后台");
            BusinessException.require(db.count("SELECT COUNT(*) FROM sys_user WHERE user_id=? AND status='0' AND del_flag='0'",user.getUserId())==1,401,"账号已停用");
            var live=db.one("SELECT dept_id FROM sys_user WHERE user_id=? AND tenant_id='000000' AND status='0' AND del_flag='0'",user.getUserId());
            Set<String> permissions=new HashSet<>();
            for(var row:db.rows("SELECT DISTINCT m.perms FROM sys_user_role ur JOIN sys_role r ON r.role_id=ur.role_id JOIN sys_role_menu rm ON rm.role_id=r.role_id JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE ur.user_id=? AND r.tenant_id='000000' AND r.status='0' AND r.del_flag='0' AND m.status='0' AND m.perms IS NOT NULL",user.getUserId())){
                for(String permission:row.get("perms").toString().split(","))if(!permission.isBlank())permissions.add(permission.strip());
            }
            return new Staff(user.getUserId(),live.get("deptId")==null?0:Db.number(live.get("deptId")),true,permissions);
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(401,"请先登录后台");}
    }
    public long customer(HttpServletRequest request){
        String header=request.getHeader("Authorization");BusinessException.require(header!=null&&header.startsWith("Bearer "),401,"请先登录");
        var rows=db.rows("SELECT s.user_id FROM customer_session s JOIN app_user u ON u.id=s.user_id WHERE token_hash=? AND s.expires_at>UTC_TIMESTAMP() AND u.status='ACTIVE' AND u.deleted_at IS NULL AND u.tenant_id='000000'",Db.hash(header.substring(7)));
        BusinessException.require(!rows.isEmpty(),401,"会话已过期或账号已注销");var row=rows.get(0);
        return Db.number(row.get("userId"));
    }
    public Long optionalCustomer(HttpServletRequest request){
        String header=request.getHeader("Authorization");
        return header==null||header.isBlank()?null:customer(request);
    }
    public void articleScope(Staff actor,Map<String,Object> article){
        if(actor.admin())return;
        BusinessException.require(Objects.equals(String.valueOf(article.get("createdBy")),Long.toString(actor.id()))||db.count("SELECT COUNT(*) FROM staff_scope WHERE user_id=? AND department_id=?",actor.id(),article.get("createdDeptId"))>0,404,"对象不存在或不可访问");
    }
}
