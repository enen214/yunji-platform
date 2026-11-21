-- Nullable legacy columns keep old audit/submission snapshots readable.
ALTER TABLE activity MODIFY organization_id BIGINT NULL;
ALTER TABLE review_campaign MODIFY organization_id BIGINT NULL;
ALTER TABLE sys_user ADD yunji_lock_version INT NOT NULL DEFAULT 0;
CREATE TABLE global_file (
 id BIGINT PRIMARY KEY, name VARCHAR(150) NOT NULL, description VARCHAR(2000) NOT NULL DEFAULT '',
 enabled BOOLEAN NOT NULL DEFAULT TRUE, deleted_at DATETIME NULL, current_version INT NOT NULL DEFAULT 0,
 lock_version INT NOT NULL DEFAULT 0, created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE global_file_version (
 file_id BIGINT NOT NULL, version_no INT NOT NULL, object_id BIGINT NOT NULL,
 provider VARCHAR(20) NOT NULL, cloud_file_id VARCHAR(1000) NULL, note VARCHAR(500) NOT NULL DEFAULT '',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(file_id,version_no), FOREIGN KEY(file_id) REFERENCES global_file(id), FOREIGN KEY(object_id) REFERENCES file_object(id),
 CHECK(provider IN('LOCAL','WECHAT_CLOUD'))
);
CREATE TABLE activity_file (
 activity_id BIGINT NOT NULL, file_id BIGINT NOT NULL, show_history BOOLEAN NOT NULL DEFAULT FALSE, sort_order INT NOT NULL DEFAULT 0,
 PRIMARY KEY(activity_id,file_id), FOREIGN KEY(activity_id) REFERENCES activity(id), FOREIGN KEY(file_id) REFERENCES global_file(id)
);
CREATE TABLE activity_review (
 activity_id BIGINT NOT NULL, campaign_id BIGINT NOT NULL,
 PRIMARY KEY(activity_id,campaign_id), FOREIGN KEY(activity_id) REFERENCES activity(id), FOREIGN KEY(campaign_id) REFERENCES review_campaign(id)
);
CREATE TABLE activity_download (
 user_id BIGINT NOT NULL, file_id BIGINT NOT NULL, activity_id BIGINT NOT NULL, day_key DATE NOT NULL,
 version_no INT NOT NULL, issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,file_id,activity_id,day_key), FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(file_id) REFERENCES global_file(id), FOREIGN KEY(activity_id) REFERENCES activity(id)
);
ALTER TABLE download_ticket ADD library_activity_id BIGINT NULL, ADD library_file_id BIGINT NULL, ADD library_version INT NULL;
UPDATE admin_navigation SET route='file-library' WHERE route='files';
