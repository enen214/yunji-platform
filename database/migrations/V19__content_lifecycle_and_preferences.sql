-- Filtered baseline: four content layers, carrier, anomaly flag and persistent replacement inbox.
ALTER TABLE article_version
 ADD interpretation LONGTEXT NULL,
 ADD original_text LONGTEXT NULL,
 ADD carrier_type VARCHAR(20) NOT NULL DEFAULT 'TEXT_IMAGE',
 ADD CONSTRAINT chk_version_carrier CHECK(carrier_type IN ('TEXT_IMAGE','LONG_IMAGE','FILE'));
ALTER TABLE article
 ADD carrier_type VARCHAR(20) NOT NULL DEFAULT 'TEXT_IMAGE',
 ADD abnormal TINYINT NOT NULL DEFAULT 0,
 ADD CONSTRAINT chk_article_carrier CHECK(carrier_type IN ('TEXT_IMAGE','LONG_IMAGE','FILE')),
 ADD CONSTRAINT chk_article_abnormal CHECK(abnormal IN (0,1));
ALTER TABLE subscription_box_preference ADD push_enabled BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TABLE user_replacement_notice (
 user_id BIGINT NOT NULL, follow_id BIGINT NOT NULL, old_article_id BIGINT NOT NULL,
 new_article_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,old_article_id), UNIQUE KEY uk_replacement_notice_follow(follow_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(old_article_id) REFERENCES article(id), FOREIGN KEY(new_article_id) REFERENCES article(id)
);
CREATE INDEX idx_behavior_content_type_user ON behavior_event(object_id,event_type,user_id);
