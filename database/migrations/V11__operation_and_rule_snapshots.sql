CREATE TABLE activity_column (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',name VARCHAR(100) NOT NULL,category_id BIGINT NOT NULL,
 sort_order INT NOT NULL DEFAULT 0,status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',FOREIGN KEY(category_id) REFERENCES content_category(id)
);
ALTER TABLE article ADD CONSTRAINT fk_article_submitted_version FOREIGN KEY(id,submitted_version_id) REFERENCES article_version(article_id,id);
ALTER TABLE notification_rule ADD CONSTRAINT chk_notification_mode CHECK(mode IN('AT_TIME','DAILY','INSTANT'));
ALTER TABLE notification_delivery ADD rule_snapshot JSON NULL,ADD content_snapshot JSON NULL;
