-- New change-driven reminders; legacy notification_rule semantics are not rewritten.
CREATE TABLE reminder_control (
 id INT PRIMARY KEY, daily_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 instant_enabled BOOLEAN NOT NULL DEFAULT TRUE, timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
 slots JSON NOT NULL, daily_template VARCHAR(128) NOT NULL DEFAULT '', instant_template VARCHAR(128) NOT NULL DEFAULT '',
 lock_version INT NOT NULL DEFAULT 0
);
INSERT INTO reminder_control(id,slots) VALUES(1,JSON_ARRAY('08:00','13:00','17:00'));
CREATE TABLE reminder_preference (
 user_id BIGINT PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT TRUE, slot TIME NOT NULL DEFAULT '08:00',
 last_batch_date DATE NULL, lock_version INT NOT NULL DEFAULT 0,
 FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE reminder_tracking (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, target_type VARCHAR(20) NOT NULL, target_id BIGINT NOT NULL,
 mode VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, lock_version INT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE(user_id,target_type,target_id), FOREIGN KEY(user_id) REFERENCES app_user(id),
 CHECK(target_type IN('ARTICLE','KEYWORD')), CHECK(mode IN('DAILY','INSTANT'))
);
CREATE TABLE reminder_change (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, article_id BIGINT NOT NULL, version_id BIGINT NOT NULL,
 event_type VARCHAR(30) NOT NULL, state VARCHAR(30) NOT NULL DEFAULT 'PENDING',
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE(user_id,article_id,version_id), KEY idx_change_user_state(user_id,state),
 FOREIGN KEY(user_id) REFERENCES app_user(id), FOREIGN KEY(article_id) REFERENCES article(id), FOREIGN KEY(version_id) REFERENCES article_version(id),
 CHECK(state IN('PENDING','BATCHED','SENT','READ','DISMISSED','SUPERSEDED','CANCELLED'))
);
CREATE TABLE reminder_match (
 change_id BIGINT NOT NULL, tracking_id BIGINT NOT NULL, tracking_version INT NOT NULL,
 PRIMARY KEY(change_id,tracking_id), FOREIGN KEY(change_id) REFERENCES reminder_change(id), FOREIGN KEY(tracking_id) REFERENCES reminder_tracking(id)
);
CREATE TABLE reminder_batch (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, channel VARCHAR(30) NOT NULL,
 state VARCHAR(30) NOT NULL DEFAULT 'PENDING', snapshot JSON NOT NULL, attempts INT NOT NULL DEFAULT 0,
 provider_task_id VARCHAR(150) NULL, error_code VARCHAR(100) NULL,
 lease_token VARCHAR(64) NULL, lease_until DATETIME NULL, next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 quota_reserved BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), completed_at DATETIME NULL,
 FOREIGN KEY(user_id) REFERENCES app_user(id), KEY idx_batch_state(state,next_attempt_at),
 CHECK(channel IN('WECom','WECHAT_SUBSCRIBE')),
 CHECK(state IN('PENDING','WAITING_AUTH','SENDING','AWAITING_STAFF','PARTIAL','ACCEPTED','SENT','FAILED','UNKNOWN','CANCELLED'))
);
CREATE TABLE reminder_batch_item (
 batch_id BIGINT NOT NULL, change_id BIGINT NOT NULL,
 PRIMARY KEY(batch_id,change_id), UNIQUE(change_id),
 FOREIGN KEY(batch_id) REFERENCES reminder_batch(id), FOREIGN KEY(change_id) REFERENCES reminder_change(id)
);
CREATE TABLE reminder_permission (
 user_id BIGINT PRIMARY KEY, wecom_bound BOOLEAN NOT NULL DEFAULT FALSE,
 available INT NOT NULL DEFAULT 0, reserved INT NOT NULL DEFAULT 0, consumed INT NOT NULL DEFAULT 0,
 simulation_outcome VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
 FOREIGN KEY(user_id) REFERENCES app_user(id), CHECK(available>=0 AND reserved>=0 AND consumed>=0)
);
CREATE TABLE reminder_local_receipt (
 receipt_key VARCHAR(100) PRIMARY KEY, payload_hash CHAR(64) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE reminder_attempt (
 id BIGINT PRIMARY KEY, batch_id BIGINT NOT NULL, attempt_no INT NOT NULL,
 outcome VARCHAR(30) NOT NULL, evidence VARCHAR(1000) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(batch_id,attempt_no), FOREIGN KEY(batch_id) REFERENCES reminder_batch(id)
);
CREATE TABLE label_keyword_curation (
 label_id BIGINT NOT NULL, keyword_id BIGINT NOT NULL, sort_order INT NOT NULL DEFAULT 0,
 PRIMARY KEY(label_id,keyword_id), FOREIGN KEY(label_id) REFERENCES content_label(id), FOREIGN KEY(keyword_id) REFERENCES keyword(id)
);
