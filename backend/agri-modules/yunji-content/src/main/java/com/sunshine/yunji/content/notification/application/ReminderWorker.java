package com.sunshine.yunji.content.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunshine.yunji.content.notification.infrastructure.ProviderHttpTransport;
import com.sunshine.yunji.content.notification.infrastructure.WecomTaskTransport;
import com.sunshine.yunji.content.shared.Db;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Orchestration is intentionally not transactional. Production dispatch remains fail-closed. */
@Service
public class ReminderWorker {
    private final ReminderBatchService batches;
    private final ProviderHttpTransport provider;
    private final WecomTaskTransport wecomTasks;
    private final Db db;
    private final boolean local;
    private final String wecomCorpId, wecomCorpSecret, wecomSender;

    public ReminderWorker(ReminderBatchService batches, Db db, ObjectMapper mapper,
            @Value("${yunji.local-enabled:false}") boolean local,
            @Value("${yunji.wecom.corp-id:}") String wecomCorpId,
            @Value("${yunji.wecom.corp-secret:}") String wecomCorpSecret,
            @Value("${yunji.wecom.sender:}") String wecomSender) {
        this.batches = batches;
        this.db = db;
        this.provider = new ProviderHttpTransport(mapper);
        this.wecomTasks = new WecomTaskTransport(mapper);
        this.local = local;
        this.wecomCorpId = wecomCorpId;
        this.wecomCorpSecret = wecomCorpSecret;
        this.wecomSender = wecomSender;
    }

    private boolean wecomReady() {
        return !wecomCorpId.isBlank() && !wecomCorpSecret.isBlank() && !wecomSender.isBlank();
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Map<String,Object> tick() {
        int planned = batches.plan(), processed = 0;
        for (int i = 0; i < 100; i++) {
            var claim = batches.claim();
            if (claim.isEmpty()) break;
            long id = Db.number(claim.get("id"));
            String lease = claim.get("leaseToken").toString();
            if (local) {
                batches.finish(id, lease, claim.get("simulationOutcome").toString(), "local-task:" + id, "LOCAL_SIMULATION");
            } else if ("WECom".equals(claim.get("channel"))) {
                dispatchWecom(claim, id, lease);
            } else {
                // 订阅消息通道本期不接，结果置 UNKNOWN 交给人工核实，不反复重试、不误发。
                batches.finish(id, lease, "CHANNEL_NOT_WIRED", "", "CHANNEL_NOT_WIRED");
            }
            processed++;
        }
        return Map.of("planned", planned, "processed", processed, "localSimulation", local, "realProviderReady", wecomReady());
    }

    private void dispatchWecom(Map<String,Object> claim, long id, String lease) {
        if (!wecomReady()) { batches.finish(id, lease, "RETRY", "", "PROVIDER_NOT_CONFIGURED"); return; }
        long user = Db.number(claim.get("userId"));
        String extId;
        try {
            extId = Objects.toString(db.one("SELECT wecom_external_userid FROM reminder_permission WHERE user_id=?", user).get("wecomExternalUserid"), "");
        } catch (Exception e) {
            batches.finish(id, lease, "RETRY", "", "MISSING_EXTERNAL_USERID");
            return;
        }
        if (extId.isBlank()) { batches.finish(id, lease, "RETRY", "", "MISSING_EXTERNAL_USERID"); return; }
        String text = buildText(claim.get("snapshot").toString());
        try {
            String token = provider.accessToken(ProviderHttpTransport.Channel.WECOM, wecomCorpId, wecomCorpSecret);
            var result = wecomTasks.create(token, wecomSender, List.of(extId), text);
            String outcome = switch (result.state()) {
                case "AWAITING_STAFF", "PARTIAL" -> "SUCCESS";
                case "REJECTED" -> "FAILURE";
                default -> "UNKNOWN";
            };
            batches.finish(id, lease, outcome, result.taskId(), result.code());
        } catch (Exception e) {
            batches.finish(id, lease, "RETRY", "", e.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private String buildText(String snapshotJson) {
        var snap = db.object(snapshotJson);
        var items = (List<?>) snap.get("items");
        if (items == null || items.isEmpty()) return "【云迹提醒】你有新信息，请在信息盒子查看。";
        var sb = new StringBuilder("【云迹提醒】你有 ").append(items.size()).append(" 条新信息：\n");
        for (var it : items) {
            String line = "· " + ((Map<String,Object>) it).get("title") + "\n";
            if (sb.length() + line.length() > 600) { sb.append("…（更多请在信息盒子查看）"); break; }
            sb.append(line);
        }
        String s = sb.toString();
        if (s.getBytes(StandardCharsets.UTF_8).length > 2048) s = s.substring(0, 600) + "…";
        return s;
    }
}
