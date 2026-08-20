ALTER TABLE device_tokens
    ADD COLUMN IF NOT EXISTS token VARCHAR(2048) NULL AFTER user_id;

UPDATE device_tokens
SET token = COALESCE(token, fcm_token)
WHERE token IS NULL AND fcm_token IS NOT NULL;

ALTER TABLE device_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NULL AFTER device_type,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NULL AFTER created_at;

UPDATE device_tokens
SET created_at = COALESCE(created_at, created_date, NOW()),
    updated_at = COALESCE(updated_at, updated_date, NOW())
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE device_tokens
    MODIFY token VARCHAR(2048) NOT NULL,
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE device_tokens
    DROP COLUMN IF EXISTS created_date,
    DROP COLUMN IF EXISTS updated_date,
    DROP COLUMN IF EXISTS fcm_token;

DROP INDEX IF EXISTS uk_device_token_user_token ON device_tokens;
ALTER TABLE device_tokens
    ADD CONSTRAINT uk_device_token_user_token UNIQUE (user_id, token);
