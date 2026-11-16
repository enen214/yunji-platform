ALTER TABLE behavior_event ADD client_event_id VARCHAR(64) NULL, ADD ingested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE behavior_event ADD UNIQUE KEY uk_event_principal(user_id,client_event_id);
ALTER TABLE exposure_receipt ADD request_hash CHAR(64) NULL;
CREATE TABLE analytics_daily (
 tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',metric_date DATE NOT NULL,metric_key VARCHAR(80) NOT NULL,
 dimension_hash CHAR(64) NOT NULL,value BIGINT NOT NULL,PRIMARY KEY(tenant_id,metric_date,metric_key,dimension_hash)
);
