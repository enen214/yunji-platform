-- Serialize replacement graph mutations within the single public tenant.
CREATE TABLE article_replacement_guard (
  tenant_id VARCHAR(20) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO article_replacement_guard(tenant_id) VALUES('000000');
