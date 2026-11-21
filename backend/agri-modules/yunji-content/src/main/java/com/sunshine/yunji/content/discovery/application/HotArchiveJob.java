package com.sunshine.yunji.content.discovery.application;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @EnableScheduling
@ConditionalOnProperty(name="yunji.hot-archive.scheduler-enabled",havingValue="true")
public class HotArchiveJob {
 private final HotArchive archive;public HotArchiveJob(HotArchive archive){this.archive=archive;}
 @Scheduled(fixedDelayString="${yunji.hot-archive.poll-ms:60000}",initialDelay=30000)
 public void run(){try{archive.archive();}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).error("HOT_ARCHIVE_FAILED type={}",e.getClass().getSimpleName());}}
}
