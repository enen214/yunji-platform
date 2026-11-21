package com.sunshine.yunji.content.identity.api;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class AdminNavigationController {
 private final Db db;private final Actors actors;
 public AdminNavigationController(Db db,Actors actors){this.db=db;this.actors=actors;}
 public record Group(String title,List<List<String>> items) {}
 @GetMapping("/api/v1/admin/navigation") public ApiResponse<List<Group>> navigation(){
  actors.staff();var grouped=new LinkedHashMap<String,List<List<String>>>();
  for(var row:db.rows("SELECT * FROM admin_navigation WHERE enabled=1 ORDER BY group_order,sort_order,route"))grouped.computeIfAbsent(row.get("groupName").toString(),key->new ArrayList<>()).add(List.of(row.get("route").toString(),row.get("label").toString()));
  return ApiResponse.ok(grouped.entrySet().stream().map(e->new Group(e.getKey(),e.getValue())).toList());
 }
}
