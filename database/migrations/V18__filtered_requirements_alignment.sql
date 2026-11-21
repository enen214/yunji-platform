-- 纯净需求基线：版本快照记录排序属性，订阅支持板块。
-- article 主表已有同名排序字段；本迁移只扩展 article_version，避免重复定义。
ALTER TABLE article_version
  ADD pinned TINYINT NOT NULL DEFAULT 0,
  ADD content_weight TINYINT NOT NULL DEFAULT 1,
  ADD hot_weight TINYINT NOT NULL DEFAULT 0,
  ADD freshness_weight TINYINT NOT NULL DEFAULT 1,
  ADD CONSTRAINT chk_article_version_weights CHECK (pinned IN (0,1) AND content_weight BETWEEN 0 AND 5 AND hot_weight BETWEEN 0 AND 10 AND freshness_weight BETWEEN 0 AND 5);

ALTER TABLE user_follow DROP CHECK chk_follow_type;
ALTER TABLE user_follow ADD CONSTRAINT chk_follow_type CHECK(target_type IN ('KEYWORD','ARTICLE','CATEGORY'));
