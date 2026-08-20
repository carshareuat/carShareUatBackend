ALTER TABLE device_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NULL AFTER device_type,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NULL AFTER created_at;

UPDATE device_tokens
SET created_at = COALESCE(created_at, created_date, NOW()),
    updated_at = COALESCE(updated_at, updated_date, NOW())
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE device_tokens
    DROP COLUMN IF EXISTS created_date,
    DROP COLUMN IF EXISTS updated_date;

ALTER TABLE device_tokens
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
