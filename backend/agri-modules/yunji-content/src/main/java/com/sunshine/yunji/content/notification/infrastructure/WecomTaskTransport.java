package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Customer-contact task API, deliberately separate from internal application message/send. */
public final class WecomTaskTransport {
    public record Result(String state,String code,String taskId,List<String> rejectedRecipients) {}
    private final ObjectMapper mapper;private final URI origin;private final HttpClient http;
    public WecomTaskTransport(ObjectMapper mapper){this(mapper,URI.create("https://qyapi.weixin.qq.com"));}
    WecomTaskTransport(ObjectMapper mapper,URI origin){this.mapper=mapper;this.origin=origin;http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();}
    public Result create(String accessToken,String sender,List<String> recipients,String text) {
        if(sender==null||sender.isBlank()||recipients==null||recipients.isEmpty()||recipients.size()>100||new HashSet<>(recipients).size()!=recipients.size()||recipients.stream().anyMatch(x->x==null||x.isBlank())||text==null||text.isBlank()||text.getBytes(StandardCharsets.UTF_8).length>2048)throw new IllegalArgumentException("Invalid frozen task payload");
        return parseCreate(post("/cgi-bin/externalcontact/add_msg_template",accessToken,Map.of("chat_type","single","sender",sender,"external_userid",recipients,"text",Map.of("content",text))));
    }
    public Map<String,Object> queryTask(String accessToken,String taskId,String cursor) {
        return post("/cgi-bin/externalcontact/get_groupmsg_task",accessToken,Map.of("msgid",required(taskId),"limit",100,"cursor",Objects.toString(cursor,"")));
    }
    public Map<String,Object> queryRecipients(String accessToken,String taskId,String sender,String cursor) {
        return post("/cgi-bin/externalcontact/get_groupmsg_send_result",accessToken,Map.of("msgid",required(taskId),"userid",required(sender),"limit",100,"cursor",Objects.toString(cursor,"")));
    }
    private String required(String value){if(value==null||value.isBlank()||value.length()>1024)throw new IllegalArgumentException("Missing platform identifier");return value;}
    @SuppressWarnings("unchecked") private Map<String,Object> post(String path,String token,Object payload) {
        required(token);
        try {
            var request=HttpRequest.newBuilder(origin.resolve(path+"?access_token="+URLEncoder.encode(token,StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var stream=response.body()) {
                byte[] bytes=stream.readNBytes(65537);if(response.statusCode()!=200||bytes.length>65536)return Map.of("transportUnknown",true);
                return mapper.readValue(bytes,Map.class);
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();return Map.of("transportUnknown",true);}
        catch(Exception e){return Map.of("transportUnknown",true);}
    }
    static Result parseCreate(Map<String,Object> body) {
        Object code=body.get("errcode");if(!(code instanceof Integer))return new Result("UNKNOWN","MALFORMED_REPLY",null,List.of());
        if((int)code!=0)return new Result("REJECTED",code.toString(),null,List.of());
        Object id=body.get("msgid");if(!(id instanceof String task)||task.isBlank())return new Result("UNKNOWN","MISSING_TASK_ID",null,List.of());
        var rejected=new ArrayList<String>();Object fail=body.get("fail_list");
        if(fail!=null){if(!(fail instanceof List<?> list)||list.stream().anyMatch(x->!(x instanceof String)))return new Result("UNKNOWN","INVALID_FAIL_LIST",task,List.of());for(Object x:list)rejected.add(x.toString());}
        return new Result(rejected.isEmpty()?"AWAITING_STAFF":"PARTIAL","0",task,List.copyOf(rejected));
    }
}
