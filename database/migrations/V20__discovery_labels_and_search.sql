-- Board owns labels; keywords remain an independent cross-board search/follow dimension.
CREATE TABLE content_label (
 id BIGINT PRIMARY KEY, tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
 category_id BIGINT NOT NULL, parent_id BIGINT NULL, name VARCHAR(100) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ENABLED', sort_order INT NOT NULL DEFAULT 0,
 lock_version INT NOT NULL DEFAULT 0, deleted_at DATETIME NULL,
 FOREIGN KEY(category_id) REFERENCES content_category(id),
 FOREIGN KEY(parent_id) REFERENCES content_label(id),
 CONSTRAINT chk_label_status CHECK(status IN ('ENABLED','DISABLED'))
);
CREATE TABLE article_label_rel (
 article_id BIGINT NOT NULL, label_id BIGINT NOT NULL,
 PRIMARY KEY(article_id,label_id),
 FOREIGN KEY(article_id) REFERENCES article(id), FOREIGN KEY(label_id) REFERENCES content_label(id)
);
ALTER TABLE user_follow DROP CHECK chk_follow_type;
ALTER TABLE user_follow ADD CONSTRAINT chk_follow_type CHECK(target_type IN ('KEYWORD','ARTICLE','CATEGORY','LABEL'));
CREATE TABLE customer_search_history (
 user_id BIGINT NOT NULL, query VARCHAR(100) NOT NULL, searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,query), FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE activity_favorite (
 user_id BIGINT NOT NULL, activity_id BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,activity_id), FOREIGN KEY(user_id) REFERENCES app_user(id), FOREIGN KEY(activity_id) REFERENCES activity(id)
);
-- Retire the unsupported channel, preserving ledger and identity evidence.
UPDATE notification_rule SET enabled=0,next_run_at=NULL,lock_version=lock_version+1 WHERE channel='WECHAT_OFFICIAL';
