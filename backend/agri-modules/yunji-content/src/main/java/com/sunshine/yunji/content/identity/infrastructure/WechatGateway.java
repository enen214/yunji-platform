package com.sunshine.yunji.content.identity.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunshine.yunji.content.shared.BusinessException;
import com.sunshine.yunji.content.shared.Db;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Official WeChat login transport. Never logs codes, tokens, identifiers or phone numbers. */
@Component
public class WechatGateway {
    public record Identity(String appId, String openid, String phoneMasked) {
        public Identity(String appId, String openid) { this(appId, openid, null); }
        @Override public String toString() { return "WechatIdentity[redacted]"; }
    }
    private final String appId, secret;
    private final ObjectMapper mapper;
    private final URI origin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    private String token;
    private long tokenExpiresAt;

    @Autowired
    public WechatGateway(ObjectMapper mapper, @Value("${yunji.wechat.app-id:}") String appId,
                         @Value("${yunji.wechat.secret:}") String secret) {
        this(mapper, appId, secret, URI.create("https://api.weixin.qq.com"));
    }
    // Test seam is package-private; the public API cannot override the provider host.
    WechatGateway(ObjectMapper mapper, String appId, String secret, URI origin) {
        this.mapper = mapper; this.appId = appId; this.secret = secret; this.origin = origin;
    }
    public Identity exchange(String code, String suppliedApp) { return exchange(code, suppliedApp, null); }

    public Identity exchange(String code, String suppliedApp, String phoneCode) {
        BusinessException.require(!appId.isBlank() && !secret.isBlank(), 503, "微信应用凭据尚未配置");
        BusinessException.require(appId.equals(suppliedApp), 422, "微信应用ID不匹配");
        Db.text(code, 100);
        if (phoneCode != null) Db.text(phoneCode, 100);
        var identity = send("/sns/jscode2session?appid=" + encode(appId) + "&secret=" + encode(secret)
            + "&js_code=" + encode(code) + "&grant_type=authorization_code", null);
        checkError(identity, false);
        var openid = identity.path("openid");
        BusinessException.require(openid.isTextual() && !openid.asText().isBlank() && openid.asText().length() <= 128,
            503, "微信身份响应异常");
        String masked = null;
        if (phoneCode != null) {
            // No fallback and no automatic retry: both codes are single-use, phone calls may consume quota.
            var phone = send("/wxa/business/getuserphonenumber?access_token=" + encode(accessToken()), Map.of("code", phoneCode));
            checkError(phone, true);
            var info = phone.path("phone_info");
            BusinessException.require(appId.equals(info.path("watermark").path("appid").asText()), 503, "微信手机号应用校验失败");
            var number = info.path("purePhoneNumber");
            BusinessException.require(number.isTextual() && number.asText().matches("[0-9]{7,15}"), 503, "微信手机号响应异常");
            String value = number.asText();
            // Keep a constant-size mask: no plaintext phone persists or leaves this gateway.
            masked = value.length() >= 10 ? value.substring(0, 3) + "****" + value.substring(value.length() - 4)
                : value.substring(0, 1) + "****" + value.substring(value.length() - 2);
        }
        return new Identity(appId, openid.asText(), masked);
    }

    /** Internal cloud API only; callers cannot choose a provider host or arbitrary endpoint. */
    public JsonNode cloud(String operation,Map<String,Object> body) {
        BusinessException.require(java.util.Set.of("uploadfile","batchdownloadfile","batchdeletefile").contains(operation),422,"不支持的云存储操作");
        BusinessException.require(!appId.isBlank()&&!secret.isBlank(),503,"微信云存储凭据尚未配置");
        var result=send("/tcb/"+operation+"?access_token="+encode(accessToken()),body);
        checkError(result,false);return result;
    }

    private synchronized String accessToken() {
        if (token != null && System.nanoTime() < tokenExpiresAt) return token;
        // stable_token with force_refresh=false avoids invalidating an existing system's token.
        var body = send("/cgi-bin/stable_token", Map.of("grant_type", "client_credential", "appid", appId,
            "secret", secret, "force_refresh", false));
        checkError(body, false);
        var value = body.path("access_token");
        long expires = body.path("expires_in").asLong(0);
        BusinessException.require(value.isTextual() && !value.asText().isBlank() && value.asText().length() <= 4096
            && expires > 60 && expires <= 7200, 503, "微信应用凭据或接口权限不可用，请联系管理员");
        token = value.asText();
        tokenExpiresAt = System.nanoTime() + Duration.ofSeconds(expires - 60).toNanos();
        return token;
    }

    private void checkError(JsonNode body, boolean phone) {
        var error = body.get("errcode");
        BusinessException.require(error == null || error.isIntegralNumber(), 503, "微信响应格式异常");
        int code = error == null ? 0 : error.asInt(-1);
        if (code == 0) return;
        if (code == 40001 || code == 40014 || code == 42001) {
            synchronized (this) { token = null; tokenExpiresAt = 0; }
        }
        if (code == 40029 || code == 40163) throw new BusinessException(401,
            phone ? "手机号授权码无效或已过期，请重新授权" : "微信登录码无效或已过期，请重新登录");
        throw new BusinessException(503, phone ? "微信手机号服务暂不可用，请重试或使用普通微信登录"
            : "微信登录服务暂不可用，请联系管理员检查凭据、IP白名单和接口权限");
    }

    private JsonNode send(String path, Map<String, Object> body) {
        try {
            var request = HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(10));
            if (body == null) request.GET();
            else request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response = http.send(request.build(), info -> new LimitedBody());
            BusinessException.require(response.statusCode() == 200, 503, "微信登录通道暂不可用");
            var result = mapper.readTree(response.body());
            BusinessException.require(result != null && result.isObject(), 503, "微信响应格式异常");
            return result;
        } catch (BusinessException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new BusinessException(503, "微信登录请求被中断"); }
        catch (Exception e) { throw new BusinessException(503, "微信登录通道暂不可用，请重新授权后重试"); }
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    // Complete only after the bounded body arrives, so the request timeout covers body reads too.
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final java.util.concurrent.CompletableFuture<byte[]> result = new java.util.concurrent.CompletableFuture<>();
        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private java.util.concurrent.Flow.Subscription subscription;
        public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(java.util.concurrent.Flow.Subscription value) { subscription = value; value.request(1); }
        public void onNext(java.util.List<java.nio.ByteBuffer> buffers) {
            for (var buffer : buffers) {
                if (buffer.remaining() > 16384 - bytes.size()) {
                    subscription.cancel(); result.completeExceptionally(new java.io.IOException("Provider response too large")); return;
                }
                byte[] part = new byte[buffer.remaining()]; buffer.get(part); bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
