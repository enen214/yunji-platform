-- 7-08 没有额度概念：删掉额度列与 CHECK；7-06 真实派发需要企微接收人。
ALTER TABLE reminder_permission DROP CHECK reminder_permission_chk_1;
ALTER TABLE reminder_permission ADD wecom_external_userid VARCHAR(64) NULL;
ALTER TABLE reminder_permission DROP COLUMN reserved, DROP COLUMN consumed;
ALTER TABLE reminder_batch DROP COLUMN quota_reserved;
