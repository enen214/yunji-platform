package com.sunshine.yunji.content.discovery.application;
import com.sunshine.yunji.content.shared.Db;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Bounded archival, not deletion: historical reads and search remain intact. */
@Service
public class HotArchive {
 private final Db db;public HotArchive(Db db){this.db=db;}
 @Transactional public int archive(){return db.update("UPDATE article SET hot_archived_at=UTC_TIMESTAMP() WHERE status='PUBLISHED' AND hot_archived_at IS NULL AND hot_until<=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR) ORDER BY hot_until,id LIMIT 500");}
}
