CREATE TABLE article_feed_snapshot (
    token_hash CHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(20) NOT NULL,
    query_hash CHAR(64) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    cards_json LONGTEXT NOT NULL,
    INDEX idx_feed_snapshot_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
