-- 云迹重制版核心 schema
-- MySQL 8.0 / utf8mb4 / 仅业务表；框架表由 cis4j 基座提供
-- 执行前请创建独立数据库，禁止在现网库直接执行

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  nickname VARCHAR(100) NULL,
  avatar_url VARCHAR(500) NULL,
  phone_masked VARCHAR(32) NULL,
  organization_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_app_user_org (organization_id),
  KEY idx_app_user_status (tenant_id, status)
) ENGINE=InnoDB COMMENT='C端用户';

CREATE TABLE IF NOT EXISTS content_category (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  parent_id BIGINT NOT NULL DEFAULT 0,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_code (tenant_id, code),
  UNIQUE KEY uk_category_parent_name (tenant_id, parent_id, name),
  KEY idx_category_parent (tenant_id, parent_id, sort_order)
) ENGINE=InnoDB COMMENT='内容分类树';

CREATE TABLE IF NOT EXISTS organization (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  parent_id BIGINT NOT NULL DEFAULT 0,
  type VARCHAR(20) NOT NULL COMMENT 'SCHOOL/COLLEGE/DEPARTMENT/PLATFORM',
  code VARCHAR(64) NULL,
  name VARCHAR(150) NOT NULL,
  short_name VARCHAR(80) NULL,
  logo_url VARCHAR(500) NULL,
  description VARCHAR(1000) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_code (tenant_id, code),
  UNIQUE KEY uk_org_parent_name (tenant_id, parent_id, name),
  KEY idx_org_parent_type (tenant_id, parent_id, type)
) ENGINE=InnoDB COMMENT='学校/学院/部门组织树';

CREATE TABLE IF NOT EXISTS keyword (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  display_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_keyword_normalized (tenant_id, normalized_name),
  KEY idx_keyword_name (tenant_id, display_name)
) ENGINE=InnoDB COMMENT='可订阅关键词';

CREATE TABLE IF NOT EXISTS article (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  title VARCHAR(255) NOT NULL,
  summary VARCHAR(1000) NULL,
  cover_url VARCHAR(500) NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'PLATFORM',
  source_name VARCHAR(150) NULL,
  source_url VARCHAR(500) NULL,
  content_type VARCHAR(20) NOT NULL DEFAULT 'BASIC',
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
  current_version_id BIGINT NULL COMMENT '当前发布版本',
  latest_version_id BIGINT NULL COMMENT '最新编辑版本',
  effective_from DATETIME NULL,
  effective_to DATETIME NULL,
  pinned TINYINT NOT NULL DEFAULT 0,
  content_weight TINYINT NOT NULL DEFAULT 1,
  hot_weight TINYINT NOT NULL DEFAULT 0,
  freshness_weight TINYINT NOT NULL DEFAULT 1,
  published_at DATETIME NULL,
  offline_at DATETIME NULL,
  created_by BIGINT NULL,
  created_dept_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version_no INT NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_article_feed (tenant_id, status, published_at, id),
  KEY idx_article_effective (tenant_id, effective_from, effective_to),
  KEY idx_article_creator (tenant_id, created_by, created_dept_id),
  FULLTEXT KEY ft_article_search (title, summary, source_name),
  CONSTRAINT chk_article_weights CHECK (pinned IN (0,1) AND content_weight BETWEEN 0 AND 5 AND hot_weight BETWEEN 0 AND 10 AND freshness_weight BETWEEN 0 AND 5),
  CONSTRAINT chk_article_status CHECK (status IN ('DRAFT','PENDING_REVIEW','REJECTED','PUBLISHED','OFFLINE')),
  CONSTRAINT chk_article_effective CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)
) ENGINE=InnoDB COMMENT='资讯主表';

CREATE TABLE IF NOT EXISTS article_version (
  id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  summary VARCHAR(1000) NULL,
  cover_url VARCHAR(500) NULL,
  source_name VARCHAR(150) NULL,
  source_url VARCHAR(500) NULL,
  content_type VARCHAR(20) NOT NULL DEFAULT 'BASIC',
  effective_from DATETIME NULL,
  effective_to DATETIME NULL,
  rich_text LONGTEXT NULL,
  relation_snapshot JSON NOT NULL COMMENT '该版本分类/组织/关键词快照',
  change_summary VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_version (article_id, version_no),
  KEY idx_article_version_article (article_id, created_at)
) ENGINE=InnoDB COMMENT='不可变版本，发布后投影到主表和关联索引';

CREATE TABLE IF NOT EXISTS article_category_rel (
  article_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (article_id, category_id),
  KEY idx_article_category_category (category_id, article_id)
) ENGINE=InnoDB COMMENT='资讯-分类关系';

CREATE TABLE IF NOT EXISTS article_organization_rel (
  article_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (article_id, organization_id),
  KEY idx_article_org_org (organization_id, article_id)
) ENGINE=InnoDB COMMENT='资讯-组织关系';

CREATE TABLE IF NOT EXISTS article_keyword_rel (
  article_id BIGINT NOT NULL,
  keyword_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (article_id, keyword_id),
  KEY idx_article_keyword_keyword (keyword_id, article_id)
) ENGINE=InnoDB COMMENT='资讯-关键词关系';

CREATE TABLE IF NOT EXISTS article_review (
  id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/PUBLISH/OFFLINE',
  comment VARCHAR(1000) NULL,
  operator_id BIGINT NOT NULL,
  operator_dept_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_review_article (article_id, created_at),
  KEY idx_review_operator (operator_id, created_at)
) ENGINE=InnoDB COMMENT='审核与发布记录';

CREATE TABLE IF NOT EXISTS article_replacement (
  id BIGINT NOT NULL,
  old_article_id BIGINT NOT NULL,
  new_article_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  relation_snapshot JSON NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_by BIGINT NULL,
  resolved_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_replacement (old_article_id, new_article_id),
  KEY idx_replacement_status (status, created_at)
) ENGINE=InnoDB COMMENT='资讯替换关系';

CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL COMMENT 'KEYWORD/ARTICLE',
  target_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_follow (user_id, target_type, target_id),
  KEY idx_follow_target (target_type, target_id, user_id)
) ENGINE=InnoDB COMMENT='用户订阅';

CREATE TABLE IF NOT EXISTS user_favorite (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_favorite (user_id, article_id),
  KEY idx_favorite_article (article_id, created_at)
) ENGINE=InnoDB COMMENT='用户收藏';

CREATE TABLE IF NOT EXISTS user_browse_history (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  last_browsed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  browse_count INT NOT NULL DEFAULT 1,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_browse (user_id, article_id),
  KEY idx_browse_user (user_id, last_browsed_at)
) ENGINE=InnoDB COMMENT='用户浏览历史';

CREATE TABLE IF NOT EXISTS user_read_state (
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  source VARCHAR(30) NULL,
  PRIMARY KEY (user_id, article_id),
  KEY idx_read_article (article_id, read_at)
) ENGINE=InnoDB COMMENT='用户已读状态';

CREATE TABLE IF NOT EXISTS user_report (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  report_type VARCHAR(30) NOT NULL,
  reason VARCHAR(1000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  handled_by BIGINT NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_report_status (status, created_at),
  KEY idx_report_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='用户举报';

CREATE TABLE IF NOT EXISTS behavior_event (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  user_id BIGINT NULL,
  session_id VARCHAR(64) NULL,
  event_type VARCHAR(40) NOT NULL,
  object_type VARCHAR(40) NULL,
  object_id BIGINT NULL,
  payload JSON NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_behavior_user_time (user_id, occurred_at),
  KEY idx_behavior_object_time (object_type, object_id, occurred_at),
  KEY idx_behavior_type_time (event_type, occurred_at)
) ENGINE=InnoDB COMMENT='统一行为事件';

CREATE TABLE IF NOT EXISTS banner (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  title VARCHAR(150) NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  link_type VARCHAR(20) NOT NULL DEFAULT 'ARTICLE',
  link_value VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  effective_from DATETIME NULL,
  effective_to DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_banner_feed (tenant_id, status, sort_order)
) ENGINE=InnoDB COMMENT='首页轮播';

CREATE TABLE IF NOT EXISTS home_recommendation (
  id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  cover_url VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  effective_from DATETIME NULL,
  effective_to DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_home_recommendation (status, sort_order, effective_from, effective_to)
) ENGINE=InnoDB COMMENT='首页推荐位';

CREATE TABLE IF NOT EXISTS content_page (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  page_key VARCHAR(40) NOT NULL,
  title VARCHAR(150) NOT NULL,
  content LONGTEXT NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_content_page_key (tenant_id, page_key)
) ENGINE=InnoDB COMMENT='协议和静态内容';

CREATE TABLE IF NOT EXISTS audit_event (
  id BIGINT NOT NULL,
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
  actor_id BIGINT NULL,
  actor_client VARCHAR(40) NULL,
  action VARCHAR(50) NOT NULL,
  object_type VARCHAR(50) NOT NULL,
  object_id BIGINT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_object (object_type, object_id, created_at),
  KEY idx_audit_actor (actor_id, created_at)
) ENGINE=InnoDB COMMENT='审计事件';

CREATE TABLE IF NOT EXISTS notification_rule (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  channel VARCHAR(20) NOT NULL COMMENT 'WECHAT_SUBSCRIBE/WECom',
  enabled TINYINT NOT NULL DEFAULT 1,
  quiet_start TIME NULL,
  quiet_end TIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_rule (user_id, channel)
) ENGINE=InnoDB COMMENT='提醒规则扩展表';

CREATE TABLE IF NOT EXISTS notification_delivery (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  article_id BIGINT NULL,
  channel VARCHAR(20) NOT NULL,
  template_key VARCHAR(100) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  provider_message_id VARCHAR(150) NULL,
  error_code VARCHAR(80) NULL,
  sent_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_delivery_user_time (user_id, created_at),
  KEY idx_delivery_status (status, created_at)
) ENGINE=InnoDB COMMENT='提醒发送记录';

