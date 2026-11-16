package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.shared.Db;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Orchestration is intentionally not transactional. Every claim is dispatched through ProviderDispatch. */
@Service
public class ReminderWorker {
    private final ReminderBatchService batches; private final ProviderDispatch dispatch;
    public ReminderWorker(ReminderBatchService batches, ProviderDispatch dispatch) { this.batches = batches; this.dispatch = dispatch; }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Map<String, Object> tick() {
        int planned = batches.plan(), processed = 0;
        for (int i = 0; i < 100; i++) {
            var claim = batches.claim();
            if (claim.isEmpty()) break;
            long id = Db.number(claim.get("id"));
            var outcome = dispatch.send(claim);
            batches.finish(id, claim.get("leaseToken").toString(), outcome.outcome(), outcome.providerId(), outcome.code());
            processed++;
        }
        return Map.of("planned", planned, "processed", processed,
            "localSimulation", dispatch.local(), "realProviderReady", dispatch.realProviderReady());
    }
}
