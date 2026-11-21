package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;

/** Explicit transport only; no controller, scheduler, authorization or quota side effects. */
public final class ProviderHttpTransport {
    public enum Channel { WECHAT_SUBSCRIBE, WECHAT_OFFICIAL, WECOM }
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI wechat, wecom;
    private final Map<String,CachedToken> tokens = new java.util.HashMap<>();
    private record CachedToken(String value, Instant until) {}

    public ProviderHttpTransport(ObjectMapper mapper) {
        this(mapper, URI.create("https://api.weixin.qq.com"), URI.create("https://qyapi.weixin.qq.com"));
    }
    // Package boundary permits an HTTP fixture, never a URL supplied by an application user.
    ProviderHttpTransport(ObjectMapper mapper, URI wechat, URI wecom) {
        this.mapper=mapper;this.wechat=wechat;this.wecom=wecom;
        http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private static String encoded(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String read(HttpResponse<java.io.InputStream> response) throws java.io.IOException {
        try(var body=response.body()) {
            byte[] bytes=body.readNBytes(65537);
            if(bytes.length>65536)throw new java.io.IOException("Provider reply too large");
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }
    public synchronized String accessToken(Channel channel,String appId,String secret) {
        if(appId==null||appId.isBlank()||secret==null||secret.isBlank())throw new IllegalStateException("Provider credentials unavailable");
        // Secret hash makes rotation invalidate the cache without retaining it as a map key.
        String key=channel+":"+appId+":"+com.sunshine.yunji.content.shared.Db.hash(secret);
        var cached=tokens.get(key);if(cached!=null&&cached.until.isAfter(Instant.now()))return cached.value;
        boolean enterprise=channel==Channel.WECOM;
        String query=enterprise?"corpid="+encoded(appId)+"&corpsecret="+encoded(secret):"grant_type=client_credential&appid="+encoded(appId)+"&secret="+encoded(secret);
        URI uri=(enterprise?wecom:wechat).resolve((enterprise?"/cgi-bin/gettoken?":"/cgi-bin/token?")+query);
        try {
            var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
            String body=read(response);if(response.statusCode()!=200)throw new IllegalStateException();
            var json=mapper.readTree(body);var token=json.path("access_token");var expiry=json.path("expires_in");
            if(!token.isTextual()||token.asText().isBlank()||!expiry.isIntegralNumber()||expiry.asLong()<120||expiry.asLong()>86400
                    ||(json.has("errcode")&&(!json.get("errcode").isIntegralNumber()||json.get("errcode").asLong()!=0)))throw new IllegalStateException();
            tokens.put(key,new CachedToken(token.asText(),Instant.now().plusSeconds(expiry.asLong()-60)));
            // Only a few configured applications are expected; no unbounded cache.
            if(tokens.size()>16)tokens.entrySet().removeIf(e->!e.getKey().equals(key));
            return token.asText();
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Provider token request interrupted");}
        catch(Exception e){throw new IllegalStateException("Provider token unavailable");}
    }
    public synchronized void invalidateToken(Channel channel,String appId,String secret) {
        tokens.remove(channel+":"+appId+":"+com.sunshine.yunji.content.shared.Db.hash(secret));
    }
    public ProviderProtocol.Reply send(Channel channel,String token,Object payload) {
        if(token==null||token.isBlank())throw new IllegalArgumentException("Provider access token required");
        String path=switch(channel){case WECHAT_SUBSCRIBE->"/cgi-bin/message/subscribe/send";case WECHAT_OFFICIAL->"/cgi-bin/message/template/send";case WECOM->"/cgi-bin/message/send";};
        URI uri=(channel==Channel.WECOM?wecom:wechat).resolve(path+"?access_token="+encoded(token));
        try {
            String body=mapper.writeValueAsString(payload);
            if(body.length()>65536)throw new IllegalArgumentException("Provider payload too large");
            var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofInputStream());
            return ProviderProtocol.sendReply(response.statusCode(),read(response),mapper,channel==Channel.WECOM);
        } catch(InterruptedException e){Thread.currentThread().interrupt();return new ProviderProtocol.Reply(ProviderProtocol.Outcome.UNKNOWN,"INTERRUPTED",null);}
        catch(Exception e){return new ProviderProtocol.Reply(ProviderProtocol.Outcome.UNKNOWN,"TRANSPORT_FAILURE",null);}
        // Never retry an HTTP send here: the outbox owner must decide using its durable attempt.
    }
}
