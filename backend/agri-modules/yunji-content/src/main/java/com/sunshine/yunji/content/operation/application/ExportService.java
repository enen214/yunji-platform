package com.sunshine.yunji.content.operation.application;

import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** Bounded server exports use the same domain grant as the page, plus a separate export grant. */
@Service
public class ExportService {
 private final Db db; private final Actors actors;
 public ExportService(Db db, Actors actors){this.db=db;this.actors=actors;}
 public record ExportView(String filename,String contentType,String csv,int rowCount,long totalCount,boolean truncated){}
 public ExportView export(String dataset,Long activityId){
  var actor=actors.staff();actor.require("yunji:export");
  String sql,count;List<String> columns;Object[] params;
  switch(dataset){
   case "activity-registrations" -> {
    actor.require("yunji:activity:manage");BusinessException.require(activityId!=null&&activityId>0,422,"需要活动ID");
    db.one("SELECT id FROM activity WHERE id=? AND tenant_id='000000'",activityId);
    sql="SELECT id,user_id,status,submitted_at,answer_snapshot FROM activity_registration WHERE activity_id=? ORDER BY queue_seq,id LIMIT 5000";
    count="SELECT COUNT(*) FROM activity_registration WHERE activity_id=?";params=new Object[]{activityId};
    columns=List.of("id","userId","status","submittedAt","answerSnapshot");
   }
   case "audit" -> {
    actor.require("yunji:audit:read");
    sql="SELECT id,actor_id,actor_client,action,object_type,object_id,created_at FROM audit_event WHERE tenant_id='000000' ORDER BY created_at DESC,id DESC LIMIT 5000";
    count="SELECT COUNT(*) FROM audit_event WHERE tenant_id='000000'";params=new Object[0];
    columns=List.of("id","actorId","actorClient","action","objectType","objectId","createdAt");
   }
   case "analytics" -> {
    actor.require("yunji:analytics:read");
    sql="SELECT metric_date,metric_key,value FROM analytics_daily WHERE tenant_id='000000' ORDER BY metric_date DESC,metric_key LIMIT 5000";
    count="SELECT COUNT(*) FROM analytics_daily WHERE tenant_id='000000'";params=new Object[0];
    columns=List.of("metricDate","metricKey","value");
   }
   default -> throw new BusinessException(422,"不支持的导出数据集");
  }
  var rows=db.rows(sql,params);long total=db.count(count,params);
  var csv=new StringBuilder("\uFEFF");csv.append(String.join(",",columns)).append("\r\n");
  for(var row:rows){var cells=new ArrayList<String>();for(var column:columns)cells.add(cell(row.get(column)));csv.append(String.join(",",cells)).append("\r\n");}
  db.audit(actor.id(),"STAFF","DATA_EXPORT",dataset,activityId==null?0:activityId,Map.of("rows",rows.size(),"total",total,"truncated",total>rows.size()));
  return new ExportView(dataset+".csv","text/csv;charset=utf-8",csv.toString(),rows.size(),total,total>rows.size());
 }
 static String cell(Object raw){String text=Objects.toString(raw,"");if(text.matches("(?s)^[\\s]*[=+@-].*"))text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
}
