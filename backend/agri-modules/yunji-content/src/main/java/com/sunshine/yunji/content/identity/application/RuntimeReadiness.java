package com.sunshine.yunji.content.identity.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RuntimeReadiness {
    private static final Logger LOG=LoggerFactory.getLogger(RuntimeReadiness.class);
    @EventListener(ApplicationReadyEvent.class)
    public void ready(){LOG.info("YUNJI_APPLICATION_READY");}
}
