package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** One-time subscribe message. Separate client from the WeCom customer-contact API. */
public final class SubscribeMessageTransport {
    public record Result(String state, String code, String messageId) {}
    private final ObjectMapper mapper; private final URI origin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();

    public SubscribeMessageTransport(ObjectMapper mapper) { this(mapper, URI.create("https://api.weixin.qq.com")); }
    SubscribeMessageTransport(ObjectMapper mapper, URI origin) { this.mapper = mapper; this.origin = origin; }

    public Result send(String accessToken, String openid, String templateId, Map<String, Object> data,
                       String page, String miniprogramState) {
        if (accessToken == null || accessToken.isBlank() || openid == null || openid.isBlank()
                || templateId == null || templateId.isBlank() || data == null || data.isEmpty())
            return new Result("UNKNOWN", "MISSING_PARAMETER", null);
        var payload = new LinkedHashMap<String, Object>();
        payload.put("touser", openid);
        payload.put("template_id", templateId);
        payload.put("page", page == null ? "" : page);
        payload.put("data", data);
        payload.put("miniprogram_state", miniprogramState == null || miniprogramState.isBlank() ? "developer" : miniprogramState);
        payload.put("lang", "zh_CN");
        try {
            var request = HttpRequest.newBuilder(origin.resolve("/cgi-bin/message/subscribe/send?access_token="
                    + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return new Result("UNKNOWN", "HTTP_" + response.statusCode(), null);
            var body = mapper.readTree(response.body());
            int code = body.path("errcode").asInt(-1);
            if (code == 0) return new Result("SUCCESS", "0", body.path("msgid").asText(null));
            var state = switch (code) {
                case -1, 40014, 42001, 45009 -> "RETRY";
                case 40003, 40037, 43101, 47003 -> "FAILURE";
                default -> "UNKNOWN";
            };
            return new Result(state, Integer.toString(code), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return new Result("UNKNOWN", "TRANSPORT", null);
        } catch (Exception e) { return new Result("UNKNOWN", "TRANSPORT", null); }
    }
}
