package com.sunshine.yunji.content.notification.application;

import com.sunshine.yunji.content.shared.Db;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Orchestration is intentionally not transactional. Production dispatch remains fail-closed. */
@Service
public class ReminderWorker {
    private final ReminderBatchService batches; private final boolean local;
    public ReminderWorker(ReminderBatchService batches,@Value("${yunji.local-enabled:false}") boolean local){this.batches=batches;this.local=local;}
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Map<String,Object> tick() {
        int planned=batches.plan(),processed=0;
        if(local)for(int i=0;i<100;i++) {
            var claim=batches.claim();if(claim.isEmpty())break;
            long id=Db.number(claim.get("id"));
            batches.finish(id,claim.get("leaseToken").toString(),claim.get("simulationOutcome").toString(),"local-task:"+id,"LOCAL_SIMULATION");processed++;
        }
        return Map.of("planned",planned,"processed",processed,"localSimulation",local,"realProviderReady",false);
    }
}
