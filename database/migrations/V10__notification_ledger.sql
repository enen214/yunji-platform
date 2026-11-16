ALTER TABLE notification_rule DROP INDEX uk_notification_rule,
 ADD tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',ADD target_type VARCHAR(20) NOT NULL DEFAULT 'ARTICLE',ADD target_id BIGINT NOT NULL DEFAULT 0,
 ADD mode VARCHAR(20) NOT NULL DEFAULT 'INSTANT',ADD timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',ADD scheduled_at DATETIME NULL,
 ADD local_send_time TIME NULL,ADD rule_hash CHAR(64) NOT NULL,ADD lock_version INT NOT NULL DEFAULT 0,
 ADD next_run_at DATETIME NULL,ADD UNIQUE KEY uk_rule_user_hash(user_id,rule_hash),ADD KEY idx_rule_schedule(enabled,next_run_at);
ALTER TABLE notification_delivery ADD rule_id BIGINT NULL,ADD rule_version INT NOT NULL DEFAULT 0,ADD dedupe_key VARCHAR(150) NULL,
 ADD article_version_id BIGINT NULL,ADD attempts INT NOT NULL DEFAULT 0,ADD next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 ADD lease_until DATETIME NULL,ADD quota_reserved BOOLEAN NOT NULL DEFAULT FALSE,
 ADD UNIQUE KEY uk_delivery_dedupe(dedupe_key),ADD KEY idx_delivery_due(status,next_attempt_at),ADD FOREIGN KEY(rule_id) REFERENCES notification_rule(id);
CREATE TABLE notification_quota (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,app_id VARCHAR(128) NOT NULL,channel VARCHAR(30) NOT NULL,template_key VARCHAR(100) NOT NULL,
 available INT NOT NULL DEFAULT 0,reserved INT NOT NULL DEFAULT 0,consumed INT NOT NULL DEFAULT 0,lock_version INT NOT NULL DEFAULT 0,
 UNIQUE(user_id,app_id,channel,template_key),FOREIGN KEY(user_id) REFERENCES app_user(id),CHECK(available>=0 AND reserved>=0 AND consumed>=0)
);
CREATE TABLE notification_quota_ledger (
 id BIGINT PRIMARY KEY,quota_id BIGINT NOT NULL,delivery_id BIGINT NOT NULL,operation VARCHAR(20) NOT NULL,amount INT NOT NULL DEFAULT 1,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(delivery_id,operation),FOREIGN KEY(quota_id) REFERENCES notification_quota(id),
 FOREIGN KEY(delivery_id) REFERENCES notification_delivery(id),CHECK(operation IN('RESERVE','COMMIT','RELEASE'))
);
CREATE TABLE notification_attempt (
 id BIGINT PRIMARY KEY,delivery_id BIGINT NOT NULL,attempt_no INT NOT NULL,request_hash CHAR(64) NOT NULL,outcome VARCHAR(40) NOT NULL,
 provider_code VARCHAR(100) NULL,started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,finished_at DATETIME NULL,
 UNIQUE(delivery_id,attempt_no),FOREIGN KEY(delivery_id) REFERENCES notification_delivery(id)
);
CREATE TABLE notification_binding (
 id BIGINT PRIMARY KEY,user_id BIGINT NOT NULL,provider VARCHAR(30) NOT NULL,app_id VARCHAR(128) NOT NULL,
 subject_hash CHAR(64) NOT NULL,binding_state VARCHAR(20) NOT NULL,key_version INT NOT NULL DEFAULT 1,
 UNIQUE(provider,app_id,subject_hash),UNIQUE(user_id,provider,app_id),FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE notification_callback_receipt (
 provider VARCHAR(30) NOT NULL,callback_key VARCHAR(100) NOT NULL,payload_hash CHAR(64) NOT NULL,handled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(provider,callback_key)
);
CREATE TABLE local_notification_outcome (
 user_id BIGINT PRIMARY KEY,outcome VARCHAR(40) NOT NULL,FOREIGN KEY(user_id) REFERENCES app_user(id)
);
