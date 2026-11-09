package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.shared.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

/** Read-only incident detection. Never turn an uncertain provider outcome into a retry. */
@Component
public class ReminderMonitor {
 private final Db db;private final int overdueMinutes;private long lastLog;private String previous="";
 public ReminderMonitor(Db db,@Value("${yunji.reminder.overdue-minutes:15}") int minutes){this.db=db;this.overdueMinutes=Math.max(5,minutes);}
 public record Incident(String id,String channel,String state,String createdAt,long ageMinutes,String errorCode){}
 public record Health(long incidents,int thresholdMinutes,List<Incident> items,boolean realProviderReady){}
 public Health health(){
  String where="state IN('UNKNOWN','FAILED') OR (state IN('PENDING','WAITING_AUTH','SENDING','AWAITING_STAFF','PARTIAL') AND created_at<TIMESTAMPADD(MINUTE,-?,UTC_TIMESTAMP()))";
  return new Health(db.count("SELECT COUNT(*) FROM reminder_batch WHERE "+where,overdueMinutes),overdueMinutes,com.sunshine.yunji.content.shared.contract.ViewMapper.list(db.rows("SELECT id,channel,state,created_at,TIMESTAMPDIFF(MINUTE,created_at,UTC_TIMESTAMP()) age_minutes,error_code FROM reminder_batch WHERE "+where+" ORDER BY created_at,id LIMIT 100",overdueMinutes),Incident.class),false);
 }
 public synchronized void logIncidents(){var h=health();String signature=h.items().toString();long now=System.currentTimeMillis();if(h.incidents()>0&&(!signature.equals(previous)||now-lastLog>=300000)){LoggerFactory.getLogger(getClass()).error("REMINDER_INCIDENT count={} thresholdMinutes={} realProviderReady=false; inspect administrator monitor before any replay",h.incidents(),overdueMinutes);lastLog=now;}previous=signature;}
}
