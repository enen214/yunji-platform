package com.sunshine.yunji.content.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

@Component
@org.springframework.scheduling.annotation.EnableScheduling
@ConditionalOnProperty(name="yunji.reminder.scheduler-enabled",havingValue="true")
public class ReminderJob {
    private final ReminderWorker worker;private final ReminderMonitor monitor;
    public ReminderJob(ReminderWorker worker,ReminderMonitor monitor){this.worker=worker;this.monitor=monitor;}
    @Scheduled(fixedDelayString="${yunji.reminder.poll-ms:30000}",initialDelay=30000)
    public void run(){try{worker.tick();}catch(Exception e){LoggerFactory.getLogger(getClass()).error("Reminder worker failed: {}",e.getClass().getSimpleName());}finally{try{monitor.logIncidents();}catch(Exception e){LoggerFactory.getLogger(getClass()).error("Reminder monitor failed: {}",e.getClass().getSimpleName());}}}
}
