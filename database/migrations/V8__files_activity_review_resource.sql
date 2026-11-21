CREATE TABLE file_object (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',owner_realm VARCHAR(20) NOT NULL,owner_id BIGINT NOT NULL,
 object_key VARCHAR(100) NOT NULL UNIQUE,sha256 CHAR(64) NOT NULL,size_bytes BIGINT NOT NULL,media_type VARCHAR(100) NOT NULL,
 display_name VARCHAR(150) NOT NULL,privacy VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'VERIFIED',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(owner_realm,owner_id,sha256,privacy),
 CHECK(privacy IN('PUBLIC','PRIVATE')),CHECK(size_bytes BETWEEN 1 AND 10485760)
);
CREATE TABLE file_reference (
 file_id BIGINT NOT NULL,owner_type VARCHAR(30) NOT NULL,owner_id BIGINT NOT NULL,reference_version_id BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(file_id,owner_type,owner_id,reference_version_id),FOREIGN KEY(file_id) REFERENCES file_object(id)
);
CREATE TABLE download_ticket (
 token_hash CHAR(64) PRIMARY KEY,file_id BIGINT NOT NULL,principal_realm VARCHAR(20) NOT NULL,principal_id BIGINT NOT NULL,
 expires_at DATETIME NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(file_id) REFERENCES file_object(id),KEY(file_id,expires_at)
);
CREATE TABLE download_event (
 id BIGINT PRIMARY KEY,token_hash CHAR(64) NOT NULL,event_type VARCHAR(30) NOT NULL,received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(token_hash) REFERENCES download_ticket(token_hash)
);
CREATE TABLE activity (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',article_id BIGINT NULL,title VARCHAR(255) NOT NULL,
 organization_id BIGINT NOT NULL,registration_start DATETIME NOT NULL,registration_end DATETIME NOT NULL,
 event_start DATETIME NOT NULL,event_end DATETIME NOT NULL,capacity INT NOT NULL,confirmed_count INT NOT NULL DEFAULT 0,
 waitlist_enabled BOOLEAN NOT NULL DEFAULT TRUE,status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',lock_version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(article_id) REFERENCES article(id),FOREIGN KEY(organization_id) REFERENCES organization(id),
 CHECK(capacity>=0 AND confirmed_count>=0 AND confirmed_count<=capacity),CHECK(registration_end>registration_start),CHECK(event_end>event_start)
);
CREATE TABLE activity_form_version (
 id BIGINT PRIMARY KEY,activity_id BIGINT NOT NULL,version_no INT NOT NULL,schema_json JSON NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(activity_id,version_no),UNIQUE(activity_id,id),FOREIGN KEY(activity_id) REFERENCES activity(id)
);
CREATE TABLE activity_registration (
 id BIGINT PRIMARY KEY,activity_id BIGINT NOT NULL,user_id BIGINT NOT NULL,form_version_id BIGINT NOT NULL,answer_snapshot JSON NOT NULL,
 status VARCHAR(20) NOT NULL,queue_seq BIGINT NOT NULL,submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,cancelled_at DATETIME NULL,
 lock_version INT NOT NULL DEFAULT 0,UNIQUE(activity_id,user_id),FOREIGN KEY(activity_id,form_version_id) REFERENCES activity_form_version(activity_id,id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),KEY(activity_id,status,queue_seq),CHECK(status IN('CONFIRMED','WAITLISTED','CANCELLED'))
);
CREATE TABLE review_campaign (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',organization_id BIGINT NOT NULL,article_id BIGINT NULL,
 title VARCHAR(255) NOT NULL,open_at DATETIME NOT NULL,close_at DATETIME NOT NULL,rule_version INT NOT NULL DEFAULT 1,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',created_by BIGINT NOT NULL,lock_version INT NOT NULL DEFAULT 0,
 FOREIGN KEY(organization_id) REFERENCES organization(id),FOREIGN KEY(article_id) REFERENCES article(id),CHECK(close_at>open_at)
);
CREATE TABLE review_requirement (
 campaign_id BIGINT NOT NULL,rule_version INT NOT NULL,requirement_key VARCHAR(64) NOT NULL,label VARCHAR(150) NOT NULL,
 required BOOLEAN NOT NULL,file_type_allowlist JSON NOT NULL,max_size BIGINT NOT NULL,eligibility_json JSON NOT NULL,
 PRIMARY KEY(campaign_id,rule_version,requirement_key),FOREIGN KEY(campaign_id) REFERENCES review_campaign(id)
);
CREATE TABLE review_application (
 id BIGINT PRIMARY KEY,campaign_id BIGINT NOT NULL,user_id BIGINT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 current_submission_id BIGINT NULL,latest_submission_id BIGINT NULL,lock_version INT NOT NULL DEFAULT 0,
 UNIQUE(campaign_id,user_id),FOREIGN KEY(campaign_id) REFERENCES review_campaign(id),FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE review_submission (
 id BIGINT PRIMARY KEY,application_id BIGINT NOT NULL,version_no INT NOT NULL,requirement_snapshot JSON NOT NULL,eligibility_snapshot JSON NOT NULL,
 submitted_at DATETIME NULL,UNIQUE(application_id,version_no),UNIQUE(application_id,id),FOREIGN KEY(application_id) REFERENCES review_application(id)
);
ALTER TABLE review_application ADD FOREIGN KEY(id,current_submission_id) REFERENCES review_submission(application_id,id),ADD FOREIGN KEY(id,latest_submission_id) REFERENCES review_submission(application_id,id);
CREATE TABLE review_submission_file (
 submission_id BIGINT NOT NULL,requirement_key VARCHAR(64) NOT NULL,file_id BIGINT NOT NULL,
 PRIMARY KEY(submission_id,requirement_key),FOREIGN KEY(submission_id) REFERENCES review_submission(id),FOREIGN KEY(file_id) REFERENCES file_object(id)
);
CREATE TABLE review_decision (
 id BIGINT PRIMARY KEY,application_id BIGINT NOT NULL,submission_id BIGINT NOT NULL,decision VARCHAR(30) NOT NULL,
 comment VARCHAR(1000) NOT NULL,reviewer_id BIGINT NOT NULL,decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(application_id,submission_id) REFERENCES review_submission(application_id,id)
);
CREATE TABLE study_subject (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',organization_id BIGINT NOT NULL,
 code VARCHAR(64) NOT NULL,name VARCHAR(150) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
 UNIQUE(organization_id,code),FOREIGN KEY(organization_id) REFERENCES organization(id)
);
CREATE TABLE study_dimension (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',kind VARCHAR(20) NOT NULL,code VARCHAR(64) NOT NULL,name VARCHAR(150) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',UNIQUE(tenant_id,kind,code),CHECK(kind IN('GRADE','MAJOR'))
);
CREATE TABLE resource_package (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',organization_id BIGINT NOT NULL,subject_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL,resource_type VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',article_id BIGINT NULL,
 FOREIGN KEY(organization_id) REFERENCES organization(id),FOREIGN KEY(subject_id) REFERENCES study_subject(id),FOREIGN KEY(article_id) REFERENCES article(id)
);
CREATE TABLE resource_file (
 package_id BIGINT NOT NULL,file_id BIGINT NOT NULL,version_no INT NOT NULL,display_name VARCHAR(150) NOT NULL,
 sort_order INT NOT NULL DEFAULT 0,PRIMARY KEY(package_id,file_id,version_no),FOREIGN KEY(package_id) REFERENCES resource_package(id),FOREIGN KEY(file_id) REFERENCES file_object(id)
);
CREATE TABLE exam_schedule (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',organization_id BIGINT NOT NULL,subject_id BIGINT NOT NULL,
 starts_at DATETIME NOT NULL,display_timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',source_reference VARCHAR(500) NOT NULL,
 FOREIGN KEY(organization_id) REFERENCES organization(id),FOREIGN KEY(subject_id) REFERENCES study_subject(id)
);
CREATE TABLE artifact (
 id BIGINT PRIMARY KEY,tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',name VARCHAR(150) NOT NULL,platform VARCHAR(40) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',guide_article_id BIGINT NULL,FOREIGN KEY(guide_article_id) REFERENCES article(id)
);
CREATE TABLE artifact_version (
 id BIGINT PRIMARY KEY,artifact_id BIGINT NOT NULL,version_name VARCHAR(64) NOT NULL,file_id BIGINT NOT NULL,sha256 CHAR(64) NOT NULL,
 release_notes VARCHAR(2000) NOT NULL,published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
 UNIQUE(artifact_id,version_name),FOREIGN KEY(artifact_id) REFERENCES artifact(id),FOREIGN KEY(file_id) REFERENCES file_object(id)
);
