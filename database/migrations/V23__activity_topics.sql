-- Independent editorial topics. No copy of demo tables or business code.
CREATE TABLE activity_topic (
 id BIGINT NOT NULL PRIMARY KEY,
 tenant_id VARCHAR(20) NOT NULL DEFAULT '000000',
 title VARCHAR(150) NOT NULL,
 subtitle VARCHAR(300) NULL,
 image_url VARCHAR(500) NULL,
 description TEXT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 sort_order INT NOT NULL DEFAULT 0,
 lock_version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL,
 updated_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted_at DATETIME NULL,
 KEY idx_topic_public (tenant_id,status,deleted_at,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE activity_topic_article (
 topic_id BIGINT NOT NULL,
 article_id BIGINT NOT NULL,
 sort_order INT NOT NULL,
 PRIMARY KEY(topic_id,article_id),
 KEY idx_topic_article (article_id),
 CONSTRAINT fk_topic_article_topic FOREIGN KEY(topic_id) REFERENCES activity_topic(id),
 CONSTRAINT fk_topic_article_article FOREIGN KEY(article_id) REFERENCES article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
