package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.shared.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

/** Incident detection plus bounded fallback: overdue unconfirmed batches are failed, never replayed. */
@Component
public class ReminderMonitor {
 private final Db db;private final int overdueMinutes;private long lastLog;private String previous="";
 public ReminderMonitor(Db db,@Value("${yunji.reminder.overdue-minutes:5}") int minutes){this.db=db;this.overdueMinutes=Math.max(5,minutes);}
 public record Incident(String id,String channel,String state,String createdAt,long ageMinutes,String errorCode){}
 public record Health(long incidents,int thresholdMinutes,List<Incident> items,boolean realProviderReady){}
 public Health health(){
  String where="state IN('PENDING','SENDING','AWAITING_STAFF','PARTIAL') AND created_at<TIMESTAMPADD(MINUTE,-?,UTC_TIMESTAMP())";
  return new Health(db.count("SELECT COUNT(*) FROM reminder_batch WHERE "+where,overdueMinutes),overdueMinutes,com.sunshine.yunji.content.shared.contract.ViewMapper.list(db.rows("SELECT id,channel,state,created_at,TIMESTAMPDIFF(MINUTE,created_at,UTC_TIMESTAMP()) age_minutes,error_code FROM reminder_batch WHERE "+where+" ORDER BY created_at,id LIMIT 100",overdueMinutes),Incident.class),false);
 }
 public synchronized void logIncidents(){
  int expired=failOverdue();
  if(expired>0)LoggerFactory.getLogger(getClass()).warn("REMINDER_EXPIRED count={} thresholdMinutes={}; overdue batches failed and audited",expired,overdueMinutes);
  var h=health();String signature=h.items().toString();long now=System.currentTimeMillis();
  if(h.incidents()>0&&(!signature.equals(previous)||now-lastLog>=300000)){LoggerFactory.getLogger(getClass()).error("REMINDER_INCIDENT count={} thresholdMinutes={} realProviderReady=false; inspect administrator monitor before any replay",h.incidents(),overdueMinutes);lastLog=now;}
  previous=signature;
 }
 /** Unconfirmed results must not linger: overdue AWAITING_STAFF/UNKNOWN batches become FAILED and are audited. */
 private int failOverdue(){
  return expire("state IN('AWAITING_STAFF','UNKNOWN','PARTIAL')","OVERDUE_UNCONFIRMED")+expire("state IN('PENDING','SENDING')","OVERDUE_NOT_DISPATCHED");
 }
 /** Terminal, audited failure for anything past the threshold. */
 private int expire(String states,String code){
  var rows=db.rows("SELECT id,channel,state FROM reminder_batch WHERE "+states+" AND created_at<TIMESTAMPADD(MINUTE,-?,UTC_TIMESTAMP())",overdueMinutes);
  int affected=0;
  for(var r:rows) {
   long id=Db.number(r.get("id"));
   affected+=db.update("UPDATE reminder_batch SET state='FAILED',error_code=?,completed_at=UTC_TIMESTAMP() WHERE id=? AND state=?",code,id,r.get("state"));
   db.update("UPDATE reminder_change c JOIN reminder_batch_item i ON i.change_id=c.id SET c.state='CANCELLED' WHERE i.batch_id=? AND c.state='BATCHED'",id);
   db.audit(0,"SYSTEM",code,"BATCH",id,Map.of("previousState",r.get("state"),"channel",r.get("channel"),"overdueMinutes",overdueMinutes));
  }
  return affected;
 }
}
