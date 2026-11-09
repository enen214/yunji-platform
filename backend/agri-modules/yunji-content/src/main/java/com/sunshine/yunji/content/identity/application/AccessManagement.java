package com.sunshine.yunji.content.identity.application;
import com.sunshine.yunji.content.shared.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class AccessManagement {
 private final Db db;private final Actors actors;
 public AccessManagement(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Scope(List<Long> departments,List<Long> reviewOrganizations){}
 private void manage(){BusinessException.require(actors.staff().admin(),403,"只有系统管理员可配置业务授权范围");}
 public List<Map<String,Object>> users(){manage();var users=db.rows("SELECT user_id id,user_name,nick_name,dept_id,status FROM sys_user WHERE tenant_id='000000' AND del_flag='0' ORDER BY user_id LIMIT 200");for(var u:users){u.put("departmentScope",db.rows("SELECT department_id FROM staff_scope WHERE user_id=?",u.get("id")));u.put("reviewScope",db.rows("SELECT organization_id FROM reviewer_scope WHERE user_id=?",u.get("id")));}return users;}
 @Transactional public void scope(long user,Scope b){manage();db.one("SELECT user_id FROM sys_user WHERE user_id=? AND tenant_id='000000' AND del_flag='0' FOR UPDATE",user);BusinessException.require(b.departments!=null&&b.reviewOrganizations!=null&&b.departments.size()<=100&&b.reviewOrganizations.size()<=100,422,"授权范围过大");
  for(long d:b.departments)db.one("SELECT dept_id FROM sys_dept WHERE dept_id=? AND tenant_id='000000' AND del_flag='0' AND status='0'",d);
  for(long o:b.reviewOrganizations)db.one("SELECT id FROM organization WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",o);
  db.update("DELETE FROM staff_scope WHERE user_id=?",user);db.update("DELETE FROM reviewer_scope WHERE user_id=?",user);for(long d:new HashSet<>(b.departments))db.update("INSERT INTO staff_scope(user_id,department_id) VALUES(?,?)",user,d);for(long o:new HashSet<>(b.reviewOrganizations))db.update("INSERT INTO reviewer_scope(user_id,organization_id) VALUES(?,?)",user,o);
  db.audit(actors.staff().id(),"STAFF","STAFF_SCOPE_SET","SYS_USER",user,Map.of("departments",b.departments,"reviewOrganizations",b.reviewOrganizations));
 }
}
