-- Product baseline 2026-10-27. Historical migrations remain immutable.
-- Stable content identity is retained for old ledger FK references; new edits overwrite
-- the current content row and never append a business version or enter approval.
ALTER TABLE article DROP CHECK chk_article_weights;
UPDATE article SET content_weight=GREATEST(content_weight,1);
ALTER TABLE article ADD CONSTRAINT chk_article_weights CHECK(pinned IN(0,1) AND content_weight BETWEEN 1 AND 5 AND hot_weight BETWEEN 0 AND 30 AND freshness_weight BETWEEN 0 AND 5);
ALTER TABLE article_version DROP CHECK chk_article_version_weights;
UPDATE article_version SET content_weight=GREATEST(content_weight,1);
ALTER TABLE article_version ADD CONSTRAINT chk_article_version_weights CHECK(pinned IN(0,1) AND content_weight BETWEEN 1 AND 5 AND hot_weight BETWEEN 0 AND 30 AND freshness_weight BETWEEN 0 AND 5);
ALTER TABLE article ADD hot_until DATE NULL, ADD urgent BOOLEAN NOT NULL DEFAULT FALSE,
 ADD lifespan VARCHAR(20) NOT NULL DEFAULT 'LONG', ADD push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 ADD taxonomy_hash CHAR(64) NULL, ADD import_batch_id BIGINT NULL;
CREATE INDEX idx_article_hot_active ON article(tenant_id,status,hot_until,effective_to);
CREATE INDEX idx_article_taxonomy ON article(tenant_id,taxonomy_hash,status,effective_to);
CREATE TABLE content_type_definition (
 code VARCHAR(20) PRIMARY KEY, name VARCHAR(100) NOT NULL, renderer VARCHAR(20) NOT NULL,
 require_original BOOLEAN NOT NULL, require_body BOOLEAN NOT NULL, require_summary BOOLEAN NOT NULL,
 require_image BOOLEAN NOT NULL, require_dates BOOLEAN NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, lock_version INT NOT NULL DEFAULT 0,
 CHECK(renderer IN('TEXT','NOTICE','LONG_IMAGE'))
);
INSERT INTO content_type_definition VALUES
 ('BASIC','普通信息','TEXT',TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,0),
 ('NOTICE','通知','NOTICE',FALSE,TRUE,FALSE,FALSE,TRUE,TRUE,0),
 ('GUIDE','攻略','LONG_IMAGE',FALSE,FALSE,FALSE,TRUE,FALSE,TRUE,0);
ALTER TABLE keyword ADD import_batch_id BIGINT NULL;
CREATE TABLE article_replacement_candidate (
 id BIGINT PRIMARY KEY, old_article_id BIGINT NOT NULL, new_article_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 resolved_by BIGINT NULL, resolved_at DATETIME NULL,
 UNIQUE(old_article_id,new_article_id), FOREIGN KEY(old_article_id) REFERENCES article(id),
 FOREIGN KEY(new_article_id) REFERENCES article(id), CHECK(status IN('PENDING','CONFIRMED','IGNORED','OBSOLETE'))
);
CREATE TABLE article_import_batch (
 id BIGINT PRIMARY KEY, staff_id BIGINT NOT NULL, request_key VARCHAR(128) NOT NULL,
 payload_hash CHAR(64) NOT NULL, payload JSON NOT NULL, total INT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING', lease_token VARCHAR(64) NULL, lease_until DATETIME NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), completed_at DATETIME NULL,
 UNIQUE(staff_id,request_key), CHECK(status IN('PROCESSING','PARTIAL','COMPLETED'))
);
CREATE TABLE article_import_item (
 batch_id BIGINT NOT NULL, row_index INT NOT NULL, title VARCHAR(255) NULL,
 article_id BIGINT NULL, error_message VARCHAR(1000) NULL,
 PRIMARY KEY(batch_id,row_index), FOREIGN KEY(batch_id) REFERENCES article_import_batch(id),
 FOREIGN KEY(article_id) REFERENCES article(id)
);
CREATE TABLE user_exposure_total (
 user_id BIGINT NOT NULL, article_id BIGINT NOT NULL, visible_count INT NOT NULL DEFAULT 0,
 day_key DATE NOT NULL, day_count INT NOT NULL DEFAULT 0, last_visible_at DATETIME(6) NULL,
 PRIMARY KEY(user_id,article_id), FOREIGN KEY(user_id) REFERENCES app_user(id), FOREIGN KEY(article_id) REFERENCES article(id)
);
CREATE TABLE customer_search_log (
 id BIGINT PRIMARY KEY, user_id BIGINT NULL, query VARCHAR(100) NOT NULL,
 searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, result_count INT NULL,
 KEY idx_search_query(query,searched_at), KEY idx_search_user(user_id,searched_at)
);
-- Upgrade makes existing content discoverable according to its former hot setting.
UPDATE article SET content_weight=GREATEST(content_weight,1), hot_until=IF(hot_weight>0,DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)+INTERVAL hot_weight DAY,NULL);
-- Legacy organization/follow/permission storage is retained for old audit references,
-- not exposed as current product capabilities.
UPDATE user_follow SET deleted_at=UTC_TIMESTAMP() WHERE target_type='CATEGORY' AND deleted_at IS NULL;

CREATE TABLE admin_navigation (route VARCHAR(100) PRIMARY KEY,group_name VARCHAR(100) NOT NULL,label VARCHAR(100) NOT NULL,group_order INT NOT NULL,sort_order INT NOT NULL,enabled BOOLEAN NOT NULL DEFAULT TRUE);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('articles','内容','资讯管理',0,0);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('content/replacements','内容','更替判断列表',0,1);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('categories','内容','板块管理',0,2);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('labels','内容','标签管理',0,3);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('keywords','内容','关键词管理',0,4);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('catalog-tree','内容','分类树',0,5);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('content-types','内容','信息类型',0,6);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('recommendations','内容','首页强加',0,7);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('banners','内容','轮播管理',0,8);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('pages','内容','协议内容',0,9);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('topics','活动与功能','活动页面专题',1,0);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('activities','活动与功能','活动与报名配置',1,1);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('files','活动与功能','文件管理',1,2);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('review-campaigns','活动与功能','评审项目',1,3);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('review-applications','活动与功能','材料评审',1,4);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/rules','提醒与推送','强提醒规则',2,0);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/settings','提醒与推送','日常提醒设置',2,1);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/records','提醒与推送','投递记录',2,2);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/wecom-operations','提醒与推送','企微任务处理',2,3);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/wecom-config','提醒与推送','企微配置',2,4);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reminders/monitor','提醒与推送','监控与告警',2,5);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('customers','用户与互动','小程序用户',3,0);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('feedback','用户与互动','反馈处理',3,1);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('reports','用户与互动','举报处理',3,2);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('analytics/dashboard','数据','数据看板',4,0);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('analytics/content','数据','内容统计',4,1);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('analytics/behavior','数据','行为统计',4,2);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('analytics/profile','数据','用户概况',4,3);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('audit','数据','操作审计',4,4);
INSERT INTO admin_navigation(route,group_name,label,group_order,sort_order) VALUES('system','系统','管理员账号',5,0);
CREATE UNIQUE INDEX uk_staff_username ON sys_user(tenant_id,user_name);
