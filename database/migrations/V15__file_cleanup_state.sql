ALTER TABLE file_object ADD deleted_at DATETIME NULL, ADD purged_at DATETIME NULL;
UPDATE file_object SET deleted_at=UTC_TIMESTAMP() WHERE status IN ('DELETED','PURGED');
UPDATE file_object SET purged_at=UTC_TIMESTAMP() WHERE status='PURGED';
CREATE INDEX idx_file_cleanup ON file_object(status,deleted_at,purged_at);
