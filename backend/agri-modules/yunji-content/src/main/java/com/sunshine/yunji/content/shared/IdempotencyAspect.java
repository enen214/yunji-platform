package com.sunshine.yunji.content.shared;

import com.sunshine.yunji.content.identity.application.Actors;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** The receipt and domain write commit together. Concurrent identical keys serialize on the primary key. */
@Aspect @Component
public class IdempotencyAspect {
 private final Db db;private final Actors actors;private final HttpServletRequest request;private final TransactionTemplate tx;private final FailedWriteAudit failures;
 public IdempotencyAspect(Db db,Actors actors,HttpServletRequest request,TransactionTemplate tx,FailedWriteAudit failures){this.db=db;this.actors=actors;this.request=request;this.tx=tx;this.failures=failures;}
 @Around("within(com.sunshine.yunji.content..api..*) && execution(public com.sunshine.yunji.content.shared.ApiResponse *(..))")
 public Object around(ProceedingJoinPoint join) throws Throwable {
  String path=request.getRequestURI(),method=request.getMethod();
  if(Set.of("GET","HEAD","OPTIONS").contains(method)||path.startsWith("/api/v1/auth/")||path.startsWith("/api/local/"))return join.proceed();
  String key=request.getHeader("Idempotency-Key");BusinessException.require(key!=null&&key.matches("[A-Za-z0-9_-]{8,128}"),422,"写请求需要8至128位Idempotency-Key");
  String principal=path.startsWith("/api/v1/admin/")?"STAFF:"+actors.staff().id():"CUSTOMER:"+actors.customer(request);
  // Imports own durable batch receipts and independent row transactions. Wrapping the
  // whole controller in this transaction would undo partial-success semantics.
  if(path.equals("/api/v1/admin/articles/import")||path.equals("/api/v1/admin/articles/import-text"))return join.proceed();
  String route=method+" "+path;List<Object> args=new ArrayList<>();
  for(Object arg:join.getArgs()){
   if(arg instanceof HttpServletRequest)continue;
   if(arg instanceof org.springframework.web.multipart.MultipartFile f)args.add(Map.of("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(f.getBytes())),"name",Objects.toString(f.getOriginalFilename(),""),"size",f.getSize()));
   else args.add(arg);
  }
  String hash=Db.hash(db.json(args));
  try{return tx.execute(status->{
   db.update("INSERT IGNORE INTO idempotency_record(principal_key,route_key,idempotency_key,request_hash,expires_at) VALUES(?,?,?,?,UTC_TIMESTAMP()+INTERVAL 1 DAY)",principal,route,key,hash);
   var receipt=db.one("SELECT * FROM idempotency_record WHERE principal_key=? AND route_key=? AND idempotency_key=? FOR UPDATE",principal,route,key);
   BusinessException.require(hash.equals(receipt.get("requestHash")),409,"幂等键已用于不同请求");
   if("COMPLETED".equals(receipt.get("status"))){var old=db.object(receipt.get("responsePayload").toString());return new ApiResponse<>(200,Objects.toString(old.get("msg")),old.get("data"));}
   try{Object result=join.proceed();String[] actor=principal.split(":");db.audit(Long.parseLong(actor[1]),actor[0],"HTTP_WRITE","REQUEST",Long.parseLong(actor[1]),Map.of("route",route,"requestHash",hash));db.update("UPDATE idempotency_record SET status='COMPLETED',response_status=200,response_payload=? WHERE principal_key=? AND route_key=? AND idempotency_key=?",db.json(result),principal,route,key);return result;}
   catch(RuntimeException e){throw e;}catch(Throwable e){throw new IllegalStateException(e);}
  });}catch(RuntimeException e){String[] actor=principal.split(":");try{failures.record(Long.parseLong(actor[1]),actor[0],route,hash,e instanceof BusinessException b?b.status:500);}catch(Exception auditFailure){org.slf4j.LoggerFactory.getLogger(getClass()).error("YUNJI_WRITE_AUDIT_FAILED {}",auditFailure.getClass().getSimpleName());}throw e;}
 }
}
