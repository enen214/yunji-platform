ALTER TABLE content_category ADD lock_version INT NOT NULL DEFAULT 0;
ALTER TABLE organization ADD lock_version INT NOT NULL DEFAULT 0;
ALTER TABLE keyword ADD lock_version INT NOT NULL DEFAULT 0;
