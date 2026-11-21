-- 手机号作为账号唯一标识：只存不可逆 HMAC（同一号码在任何 appid 下同值），不存明文。
ALTER TABLE app_user ADD COLUMN phone_hash CHAR(64) NULL AFTER phone_masked;
-- NULL 不参与唯一性，历史上没有手机号的账号不受影响；注销时把 phone_hash 置 NULL 释放号码。
CREATE UNIQUE INDEX uk_user_phone_hash ON app_user(tenant_id,phone_hash);

-- 头像：内容存本地对象库，app_user 只记引用，头像地址统一为 /api/v1/me/avatar?v=<对象ID>。
ALTER TABLE app_user ADD COLUMN avatar_object_id BIGINT NULL AFTER avatar_url;
