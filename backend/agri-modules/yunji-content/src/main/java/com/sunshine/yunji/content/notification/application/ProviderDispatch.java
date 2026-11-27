package com.sunshine.yunji.content.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunshine.yunji.content.identity.infrastructure.IdentityCrypto;
import com.sunshine.yunji.content.notification.infrastructure.*;
import com.sunshine.yunji.content.shared.Db;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Chooses the real provider when its credentials exist, otherwise the explicit local channel.
 * Never reports success without either a platform task id or a recorded local simulation.
 */
@Service
public class ProviderDispatch {
    public record Outcome(String outcome, String providerId, String code) {}

    private static final DateTimeFormatter MINUTE =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final Db db; private final ObjectMapper mapper; private final IdentityCrypto crypto;
    private final AccessTokenCache wecomToken; private final AccessTokenCache miniToken;
    private final WecomTaskTransport wecom; private final SubscribeMessageTransport subscribe;
    private final boolean local;
    private final String senderUserId, fallbackTemplate, articlePage, miniprogramState, fieldMap;
    private final boolean wecomConfigured, wechatConfigured;

    public ProviderDispatch(Db db, ObjectMapper mapper, IdentityCrypto crypto,
            @Value("${yunji.wecom.corp-id:}") String corpId,
            @Value("${yunji.wecom.secret:}") String corpSecret,
            @Value("${yunji.wecom.sender-userid:}") String senderUserId,
            @Value("${yunji.wechat.app-id:}") String wechatAppId,
            @Value("${yunji.wechat.secret:}") String wechatSecret,
            @Value("${yunji.reminder.instant-template:}") String instantTemplate,
            @Value("${yunji.reminder.subscribe-page:pages/article/index}") String subscribePage,
            @Value("${yunji.reminder.miniprogram-state:developer}") String miniprogramState,
            @Value("${yunji.reminder.subscribe-fields:}") String subscribeFields,
            @Value("${yunji.local-enabled:false}") boolean local) {
        this.db = db; this.mapper = mapper; this.crypto = crypto; this.local = local;
        this.wecomToken = AccessTokenCache.forWecom(mapper, corpId, corpSecret);
        this.miniToken = AccessTokenCache.forMiniProgram(mapper, wechatAppId, wechatSecret);
        this.wecom = new WecomTaskTransport(mapper);
        this.subscribe = new SubscribeMessageTransport(mapper);
        this.senderUserId = senderUserId == null ? "" : senderUserId.strip();
        this.fallbackTemplate = instantTemplate == null ? "" : instantTemplate.strip();
        this.articlePage = subscribePage == null ? "pages/article/index" : subscribePage.strip();
        this.miniprogramState = miniprogramState == null ? "developer" : miniprogramState.strip();
        this.fieldMap = subscribeFields == null ? "" : subscribeFields.strip();
        this.wecomConfigured = wecomToken != null && !this.senderUserId.isBlank();
        this.wechatConfigured = miniToken != null;
    }

    public boolean local() { return local; }
    public boolean wecomConfigured() { return wecomConfigured; }
    public boolean wechatConfigured() { return wechatConfigured; }
    public boolean realProviderReady() { return wecomConfigured || wechatConfigured; }

    public Outcome send(Map<String, Object> batch) {
        long id = Db.number(batch.get("id"));
        long user = Db.number(batch.get("userId"));
        String channel = Objects.toString(batch.get("channel"), "");
        boolean ready = "WECom".equals(channel) ? wecomConfigured : wechatConfigured;
        if (!ready) {
            if (local) return simulate(id, user, channel);
            return new Outcome("FAILURE", null, "PROVIDER_NOT_CONFIGURED");
        }
        try {
            return "WECom".equals(channel) ? sendWecom(user, batch) : sendSubscribe(user, batch);
        } catch (RuntimeException e) {
            return new Outcome("UNKNOWN", null, "DISPATCH_ERROR");
        }
    }

    private Outcome simulate(long id, long user, String channel) {
        String planned = db.rows("SELECT simulation_outcome FROM reminder_permission WHERE user_id=?", user)
            .stream().findFirst().map(r -> Objects.toString(r.get("simulationOutcome"), "SUCCESS")).orElse("SUCCESS");
        String outcome = switch (planned) {
            case "FAILURE", "RETRY", "UNKNOWN" -> planned;
            default -> "SUCCESS";
        };
        return new Outcome(outcome, "local-" + channel.toLowerCase(Locale.ROOT) + ":" + id, "LOCAL_SIMULATION");
    }

    private Outcome sendWecom(long user, Map<String, Object> batch) {
        var perm = db.rows("SELECT * FROM reminder_permission WHERE user_id=?", user).stream().findFirst().orElse(Map.of());
        String recipient = Objects.toString(perm.get("wecomExternalUserid"), "");
        if (recipient.isBlank()) return new Outcome("FAILURE", null, "WECOM_RECIPIENT_MISSING");
        String token = wecomToken.get();
        if (token == null) return new Outcome("UNKNOWN", null, "WECOM_TOKEN_UNAVAILABLE");
        var items = items(batch);
        if (items.isEmpty()) return new Outcome("FAILURE", null, "EMPTY_SNAPSHOT");
        String text = wecomText(items);
        if (text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 2000)
            text = text.substring(0, 600);
        var result = wecom.create(token, senderUserId, List.of(recipient), text);
        if ("AWAITING_STAFF".equals(result.state()) || "PARTIAL".equals(result.state()))
            return new Outcome("SUCCESS", result.taskId(), "0");
        if ("REJECTED".equals(result.state())) return new Outcome("FAILURE", null, result.code());
        return new Outcome("UNKNOWN", result.taskId(), result.code());
    }

    private Outcome sendSubscribe(long user, Map<String, Object> batch) {
        String openid = openid(user);
        if (openid == null) return new Outcome("FAILURE", null, "OPENID_UNAVAILABLE");
        String template = db.rows("SELECT instant_template FROM reminder_control WHERE id=1").stream().findFirst()
            .map(r -> Objects.toString(r.get("instantTemplate"), "")).orElse("");
        if (template == null || template.isBlank()) template = fallbackTemplate;
        if (template.isBlank()) return new Outcome("FAILURE", null, "TEMPLATE_MISSING");
        String token = miniToken.get();
        if (token == null) return new Outcome("UNKNOWN", null, "WECHAT_TOKEN_UNAVAILABLE");
        var items = items(batch);
        if (items.isEmpty()) return new Outcome("FAILURE", null, "EMPTY_SNAPSHOT");
        String page = articlePage + "?id=" + Objects.toString(items.get(0).get("articleId"), "");
        var result = subscribe.send(token, openid, template, subscribeData(items), page, miniprogramState);
        return switch (result.state()) {
            case "SUCCESS" -> new Outcome("SUCCESS", result.messageId(), "0");
            case "RETRY" -> new Outcome("RETRY", null, result.code());
            case "FAILURE" -> new Outcome("FAILURE", null, result.code());
            default -> new Outcome("UNKNOWN", null, result.code());
        };
    }

    private String openid(long user) {
        var rows = db.rows("SELECT encrypted_subject FROM app_user_identity WHERE user_id=? AND provider='WECHAT' ORDER BY id DESC LIMIT 1", user);
        if (rows.isEmpty()) return null;
        Object value = rows.get(0).get("encryptedSubject");
        if (value == null) return null;
        try { return crypto.decrypt(value.toString()); } catch (RuntimeException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        if (!(value instanceof Map<?, ?>)) return null;
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> batch) {
        Object snapshot = batch.get("snapshot");
        if (snapshot == null) return List.of();
        Map<String, Object> root;
        if (snapshot instanceof Map<?, ?>) root = cast(snapshot);
        else {
            try { root = mapper.readValue(snapshot.toString(), Map.class); } catch (Exception e) { return List.of(); }
        }
        if (root == null) return List.of();
        Object raw = root.get("items");
        if (!(raw instanceof List<?> list)) return List.of();
        var out = new ArrayList<Map<String, Object>>();
        for (Object item : list) { var mapped = cast(item); if (mapped != null) out.add(mapped); }
        return out;
    }

    private String wecomText(List<Map<String, Object>> items) {
        var text = new StringBuilder();
        text.append("您关注的信息有 ").append(items.size()).append(" 条更新\n");
        int shown = 0;
        for (var item : items) {
            if (++shown > 10) { text.append("…共 ").append(items.size()).append(" 条，详见小程序信息盒子\n"); break; }
            text.append(shown).append(". ").append(clip(Objects.toString(item.get("title"), "未命名"), 60)).append('\n');
        }
        return text.toString();
    }

    private Map<String, Object> subscribeData(List<Map<String, Object>> items) {
        var first = items.get(0);
        var values = new LinkedHashMap<String, String>();
        values.put("title", clip(Objects.toString(first.get("title"), ""), 20));
        values.put("summary", clip(Objects.toString(first.get("summary"), ""), 20));
        values.put("count", Integer.toString(items.size()));
        values.put("now", MINUTE.format(Instant.now()));
        // thing 类字段微信不接受空值（会返回 47003），给一句恒定文案兜底
        values.put("tip", "点击查看详情");
        String mapping = fieldMap.isBlank()
            // 与小程序「教育资讯更新提醒」模板对齐：thing1 资讯名称 / time2 更新时间 / thing3 温馨提示
            ? "{\"thing1\":\"title\",\"time2\":\"now\",\"thing3\":\"tip\"}"
            : fieldMap;
        Map<String, String> pairs;
        try {
            @SuppressWarnings("unchecked") Map<String, String> parsed = mapper.readValue(mapping, Map.class);
            pairs = parsed;
        } catch (Exception e) {
            pairs = Map.of("thing1", "title", "time2", "now", "thing3", "tip");
        }
        var data = new LinkedHashMap<String, Object>();
        for (var entry : pairs.entrySet())
            data.put(entry.getKey(), Map.of("value", values.getOrDefault(entry.getValue(), "")));
        return data;
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
