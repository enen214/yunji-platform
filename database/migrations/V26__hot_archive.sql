-- Soft archival only. Preserve expiry and content identity for history, audits and receipts.
ALTER TABLE article ADD hot_archived_at DATETIME NULL;
CREATE INDEX idx_hot_archive_pending ON article(status,hot_archived_at,hot_until);
