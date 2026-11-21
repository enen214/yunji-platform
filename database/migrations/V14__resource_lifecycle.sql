ALTER TABLE study_subject ADD lock_version INT NOT NULL DEFAULT 0;
ALTER TABLE study_dimension ADD lock_version INT NOT NULL DEFAULT 0;
ALTER TABLE resource_package ADD lock_version INT NOT NULL DEFAULT 0, ADD current_version_no INT NOT NULL DEFAULT 1;
UPDATE resource_package p SET current_version_no=COALESCE((SELECT MAX(f.version_no) FROM resource_file f WHERE f.package_id=p.id),1);
ALTER TABLE exam_schedule ADD lock_version INT NOT NULL DEFAULT 0, ADD status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
ALTER TABLE artifact ADD lock_version INT NOT NULL DEFAULT 0;
