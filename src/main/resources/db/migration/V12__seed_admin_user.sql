-- Idempotent seed for admin user; leaves password_hash NULL if not provided so application bootstrap will set it
INSERT INTO users (id, role, mobile, password_hash, active, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000001', 'ADMIN', '6381110664', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE mobile = '6381110664');
