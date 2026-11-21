package com.sunshine.yunji.content.shared;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.Map;

/** Failure evidence survives the rejected business transaction; no request body or credential is stored. */
@Component
public class FailedWriteAudit {
    private final Db db;
    public FailedWriteAudit(Db db){this.db=db;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(long actor,String realm,String route,String requestHash,int status){
        db.audit(actor,realm,"HTTP_WRITE_REJECTED","REQUEST",actor,Map.of("route",route,"requestHash",requestHash,"status",status));
    }
}
