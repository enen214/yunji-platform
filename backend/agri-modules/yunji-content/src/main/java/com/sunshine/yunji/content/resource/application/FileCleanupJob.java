package com.sunshine.yunji.content.resource.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="yunji.file-cleanup.enabled",havingValue="true")
public class FileCleanupJob {
    private static final Logger LOG=LoggerFactory.getLogger(FileCleanupJob.class);
    private final FileService files;
    public FileCleanupJob(FileService files){this.files=files;}
    @Scheduled(cron="${yunji.file-cleanup.cron:0 30 2 * * *}",zone="UTC")
    public void execute(){try{LOG.info("YUNJI_FILE_CLEANUP {}",files.scheduledCleanup(100));}catch(Exception e){LOG.error("YUNJI_FILE_CLEANUP_FAILED",e);}}
}
