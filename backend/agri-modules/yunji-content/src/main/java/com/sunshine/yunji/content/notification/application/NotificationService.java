package com.sunshine.yunji.content.notification.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Legacy rule hashing plus capability reporting. Quota, ledger and delivery settlement are deliberately absent. */
@Service
public class NotificationService {
 private final Actors actors;private final boolean local;private final ProviderDispatch dispatch;
 public NotificationService(Actors actors,@Value("${yunji.local-enabled:false}") boolean local,ProviderDispatch dispatch){this.actors=actors;this.local=local;this.dispatch=dispatch;}
 public static String ruleHash(Db db,String targetType,long targetId,String mode,String channel,String timezone,String scheduledAt,String localSendTime){
  String at=scheduledAt.isBlank()?"":Instant.parse(scheduledAt).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
  return Db.hash(db.json(new TreeMap<>(Map.of("targetType",targetType,"targetId",Long.toString(targetId),"mode",mode,"channel",channel,"zone",timezone,"at",at,"time",localSendTime))));
 }
}
