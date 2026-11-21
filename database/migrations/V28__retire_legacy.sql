-- V28 退役旧资源、协议同意、组织维度（规格 10-08 / 2-09 / 12-02）
-- 1) 旧资源接口已下线，考试安排 / 资源包 / 产物不再有写入方
DROP TABLE IF EXISTS artifact_version;
DROP TABLE IF EXISTS artifact;
DROP TABLE IF EXISTS resource_file;
DROP TABLE IF EXISTS resource_package;
DROP TABLE IF EXISTS exam_schedule;

-- 2) 协议同意接口已在代码层移除
DROP TABLE IF EXISTS user_consent;

-- 3) 版本字段仅保留兼容，不再追加版本（L2）
ALTER TABLE article_version MODIFY COLUMN review_state varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1';

-- 4) 组织维度：2-09 已停用，仅作外键兼容保留
--    activity / app_user / review_campaign / reviewer_scope / study_subject 仍持有
--    organization 外键，代码侧既不写入也不返回 organization_id，表仅作外键兼容保留，
--    但外键列仍在，故不删表、不删外键，只加注释标注停用。
ALTER TABLE organization COMMENT='云迹 2-09 已停用组织维度，仅为外键兼容保留，不做业务写入';
ALTER TABLE article_organization_rel COMMENT='云迹 2-09 已停用组织维度，仅为外键兼容保留，不做业务写入';

-- 5) article 版本字段同样是兼容残留（L2 补全）
ALTER TABLE article MODIFY COLUMN latest_version_id bigint DEFAULT NULL COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1';
ALTER TABLE article MODIFY COLUMN submitted_version_id bigint DEFAULT NULL COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1';

-- 6) L10 死字段 freshness_weight 删除：排序只用 content_weight 与 hot_weight(hotDays)
ALTER TABLE article DROP CHECK chk_article_weights;
ALTER TABLE article DROP COLUMN freshness_weight;
ALTER TABLE article ADD CONSTRAINT chk_article_weights CHECK (pinned IN (0,1) AND content_weight BETWEEN 1 AND 5 AND hot_weight BETWEEN 0 AND 30);

ALTER TABLE article_version DROP CHECK chk_article_version_weights;
ALTER TABLE article_version DROP COLUMN freshness_weight;
ALTER TABLE article_version ADD CONSTRAINT chk_article_version_weights CHECK (pinned IN (0,1) AND content_weight BETWEEN 1 AND 5 AND hot_weight BETWEEN 0 AND 30);

-- 7) S1 时区一致：next_attempt_at 默认值原为 CURRENT_TIMESTAMP（服务器本地时间），
--    而 claim() 用 next_attempt_at <= UTC_TIMESTAMP() 比较；服务器时区非 UTC 时新批次永远领不到。
ALTER TABLE reminder_batch MODIFY COLUMN next_attempt_at datetime NOT NULL DEFAULT (UTC_TIMESTAMP());
