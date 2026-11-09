package com.sunshine.yunji.content.resource.application;

import com.sunshine.yunji.content.identity.infrastructure.WechatGateway;
import com.sunshine.yunji.content.shared.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Server-side WeChat cloud adapter. Local emulation is explicit and forbidden outside local mode. */
@Component
public class CloudFiles {
 private final WechatGateway gateway;private final String environment;private final boolean local;
 public CloudFiles(WechatGateway gateway,@Value("${yunji.cloud.environment:}") String environment,@Value("${yunji.local-enabled:false}") boolean local){this.gateway=gateway;this.environment=environment;this.local=local;}
 public boolean local(){return local;}
 private void configured(){BusinessException.require(!environment.isBlank(),503,"微信云存储环境未配置，不能使用本地模拟替代生产上传");}
 public String upload(byte[] bytes,String name){configured();String path="yunji/library/"+UUID.randomUUID()+"/"+name.replaceAll("[^A-Za-z0-9._-]","_");var p=gateway.cloud("uploadfile",Map.of("env",environment,"path",path));URI url=trusted(p.path("url").asText());String boundary="Yunji"+UUID.randomUUID().toString().replace("-","");
  var head=new java.io.ByteArrayOutputStream();for(var entry:Map.of("key",path,"Signature",p.path("authorization").asText(),"x-cos-security-token",p.path("token").asText(),"x-cos-meta-fileid",p.path("cos_file_id").asText()).entrySet())head.writeBytes(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));head.writeBytes(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"upload.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));head.writeBytes(bytes);head.writeBytes(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
  try{var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build().send(HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(30)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(head.toByteArray())).build(),HttpResponse.BodyHandlers.discarding());BusinessException.require(response.statusCode()>=200&&response.statusCode()<300,503,"微信云文件上传失败");String id=p.path("file_id").asText();BusinessException.require(id.startsWith("cloud://")&&id.length()<=1000,503,"微信云文件标识异常");return id;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new BusinessException(503,"云上传被中断");}catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(503,"云上传暂不可用");}
 }
 public String download(String fileId){configured();var r=gateway.cloud("batchdownloadfile",Map.of("env",environment,"file_list",List.of(Map.of("fileid",fileId,"max_age",120))));var f=r.path("file_list").path(0);BusinessException.require(f.path("status").asInt(-1)==0&&fileId.equals(f.path("fileid").asText()),503,"云文件暂不可下载");return trusted(f.path("download_url").asText()).toString();}
 static URI trusted(String value){try{var u=URI.create(value);String host=Objects.toString(u.getHost(),"").toLowerCase(Locale.ROOT);BusinessException.require("https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&(host.endsWith(".myqcloud.com")||host.endsWith(".tcb.qcloud.la")||host.endsWith(".tcloudbaseapp.com")),503,"云存储返回了非信任地址");return u;}catch(IllegalArgumentException e){throw new BusinessException(503,"云存储地址异常");}}
}
