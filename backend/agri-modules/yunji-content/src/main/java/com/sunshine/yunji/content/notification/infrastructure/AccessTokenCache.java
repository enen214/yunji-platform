package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Cached platform access token (WeCom or mini program). The token never leaves this module. */
public final class AccessTokenCache {
    private final ObjectMapper mapper; private final URI origin; private final String path;
    private String token; private long renewAt;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();

    private AccessTokenCache(ObjectMapper mapper, URI origin, String path) {
        this.mapper = mapper; this.origin = origin; this.path = path;
    }

    public static AccessTokenCache forWecom(ObjectMapper mapper, String corpId, String corpSecret) {
        if (corpId == null || corpId.isBlank() || corpSecret == null || corpSecret.isBlank()) return null;
        return new AccessTokenCache(mapper, URI.create("https://qyapi.weixin.qq.com"),
            "/cgi-bin/gettoken?corpid=" + enc(corpId) + "&corpsecret=" + enc(corpSecret));
    }

    public static AccessTokenCache forMiniProgram(ObjectMapper mapper, String appId, String secret) {
        if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) return null;
        return new AccessTokenCache(mapper, URI.create("https://api.weixin.qq.com"),
            "/cgi-bin/token?grant_type=client_credential&appid=" + enc(appId) + "&secret=" + enc(secret));
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    /** Returns null when credentials are absent or the platform refuses them. Never throws. */
    public synchronized String get() {
        if (token != null && System.currentTimeMillis() < renewAt) return token;
        try {
            var request = HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(10)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return null;
            var body = mapper.readTree(response.body());
            if (body.path("errcode").asInt(0) != 0) return null;
            var value = body.path("access_token").asText("");
            if (value.isBlank()) return null;
            token = value;
            renewAt = System.currentTimeMillis() + Math.max(60, body.path("expires_in").asLong(7200) - 300) * 1000L;
            return token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return null;
        } catch (Exception e) { return null; }
    }

    public void invalidate() { token = null; renewAt = 0; }
}
