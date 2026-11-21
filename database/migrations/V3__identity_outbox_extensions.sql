CREATE TABLE IF NOT EXISTS app_user_identity (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  provider VARCHAR(30) NOT NULL,
  provider_app_id VARCHAR(128) NOT NULL,
  subject_hash CHAR(64) NOT NULL COMMENT '带版本化密钥的 HMAC，避免裸 openid 外泄',
  encrypted_subject TEXT NULL,
  union_subject_hash CHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_identity (tenant_id, provider, provider_app_id, subject_hash),
  KEY idx_identity_user (user_id)
) ENGINE=InnoDB COMMENT='外部身份与内部用户分离';

CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  aggregate_type VARCHAR(40) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lease_until DATETIME NULL,
  processed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_outbox_pending (status, available_at, id)
) ENGINE=InnoDB COMMENT='事务事件待处理队列';

CREATE TABLE IF NOT EXISTS event_consumer_receipt (
  consumer_name VARCHAR(80) NOT NULL,
  event_id BIGINT NOT NULL,
  processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (consumer_name,event_id)
) ENGINE=InnoDB COMMENT='消费者去重收据';

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  handler_id BIGINT NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id),
  KEY idx_feedback_status(tenant_id,status,created_at)
) ENGINE=InnoDB COMMENT='用户反馈';

CREATE TABLE IF NOT EXISTS user_exposure (
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  window_key DATE NOT NULL,
  visible_count INT NOT NULL DEFAULT 0,
  last_visible_at DATETIME NOT NULL,
  PRIMARY KEY(user_id,article_id,window_key)
) ENGINE=InnoDB COMMENT='可见曝光计数；不等于已读';

CREATE TABLE IF NOT EXISTS exposure_receipt (
  event_id CHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(event_id,user_id)
) ENGINE=InnoDB COMMENT='客户端可见曝光去重';

CREATE TABLE IF NOT EXISTS idempotency_record (
  principal_key VARCHAR(150) NOT NULL,
  route_key VARCHAR(150) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  response_status SMALLINT NULL,
  response_payload JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(principal_key,route_key,idempotency_key)
) ENGINE=InnoDB COMMENT='有副作用请求的幂等记录';

CREATE TABLE IF NOT EXISTS migration_id_map (
  batch_key VARCHAR(80) NOT NULL,
  source_table VARCHAR(100) NOT NULL,
  source_id VARCHAR(128) NOT NULL,
  target_table VARCHAR(100) NOT NULL,
  target_id BIGINT NULL,
  resolution VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(1000) NULL,
  source_hash CHAR(64) NULL,
  PRIMARY KEY(batch_key,source_table,source_id,target_table)
) ENGINE=InnoDB COMMENT='迁移映射和隔离原因';
