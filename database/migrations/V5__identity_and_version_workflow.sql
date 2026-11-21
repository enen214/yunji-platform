ALTER TABLE article ADD lock_version INT NOT NULL DEFAULT 0, ADD submitted_version_id BIGINT NULL;
ALTER TABLE article_version ADD review_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT', ADD submitted_at DATETIME NULL, ADD decided_at DATETIME NULL;
ALTER TABLE article_version ADD CONSTRAINT chk_version_review CHECK(review_state IN('DRAFT','SUBMITTED','APPROVED','REJECTED','WITHDRAWN'));
ALTER TABLE article_review ADD request_id VARCHAR(64) NULL;
ALTER TABLE user_read_state ADD read_version_id BIGINT NULL;
ALTER TABLE user_read_state ADD CONSTRAINT fk_read_version FOREIGN KEY(article_id,read_version_id) REFERENCES article_version(article_id,id);
ALTER TABLE app_user_identity ADD key_version INT NOT NULL DEFAULT 1;
CREATE TABLE customer_session (
 token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at DATETIME NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user_id) REFERENCES app_user(id), KEY(user_id,expires_at)
);
CREATE TABLE local_login_code (
 code_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, app_id VARCHAR(100) NOT NULL, expires_at DATETIME NOT NULL,
 used_at DATETIME NULL, FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE staff_scope (
 user_id BIGINT NOT NULL, department_id BIGINT NOT NULL, PRIMARY KEY(user_id,department_id),
 FOREIGN KEY(user_id) REFERENCES sys_user(user_id), FOREIGN KEY(department_id) REFERENCES sys_dept(dept_id)
);
CREATE TABLE keyword_alias (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',normalized_alias VARCHAR(100) NOT NULL,
 keyword_id BIGINT NOT NULL, UNIQUE(tenant_id,normalized_alias),FOREIGN KEY(keyword_id) REFERENCES keyword(id)
);
CREATE TABLE keyword_blacklist (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',normalized_name VARCHAR(100) NOT NULL,
 reason VARCHAR(500) NOT NULL,UNIQUE(tenant_id,normalized_name)
);
CREATE TABLE content_page_version (
 id BIGINT PRIMARY KEY,page_id BIGINT NOT NULL,version_no INT NOT NULL,title VARCHAR(150) NOT NULL,content LONGTEXT NOT NULL,
 published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(page_id,version_no),FOREIGN KEY(page_id) REFERENCES content_page(id)
);
CREATE TABLE user_consent (
 user_id BIGINT NOT NULL,page_version_id BIGINT NOT NULL,consent_type VARCHAR(40) NOT NULL,
 accepted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(user_id,page_version_id,consent_type),
 FOREIGN KEY(user_id) REFERENCES app_user(id),FOREIGN KEY(page_version_id) REFERENCES content_page_version(id)
);
INSERT INTO content_page_version(id,page_id,version_no,title,content) SELECT id,id,version_no,title,content FROM content_page WHERE status='PUBLISHED';
CREATE TABLE subscription_box_preference (
 user_id BIGINT NOT NULL,follow_id BIGINT NOT NULL,hidden BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(user_id,follow_id),FOREIGN KEY(user_id) REFERENCES app_user(id),FOREIGN KEY(follow_id) REFERENCES user_follow(id)
);
