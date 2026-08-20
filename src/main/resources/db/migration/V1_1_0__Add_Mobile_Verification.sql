-- =====================================================================
-- MOBILE NUMBER VERIFICATION - DATABASE MIGRATION
-- =====================================================================
-- Date: 2026-08-17
-- Purpose: Add mobile verification columns to users table
-- Database: MySQL 8.0+
-- =====================================================================

-- Step 1: Add new columns to users table only if they are missing.
-- This migration is intentionally idempotent because the table may already
-- contain the mobile verification columns from a previous failed attempt.
-- =====================================================================
SELECT COUNT(*) INTO @mobile_verified_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND column_name = 'mobile_verified';

SET @mobile_verified_sql = IF(@mobile_verified_exists = 0,
  'ALTER TABLE users ADD COLUMN mobile_verified BOOLEAN DEFAULT FALSE COMMENT ''Has mobile number been verified via OTP?''',
  'SELECT 1');

PREPARE mobile_verified_stmt FROM @mobile_verified_sql;
EXECUTE mobile_verified_stmt;
DEALLOCATE PREPARE mobile_verified_stmt;

SELECT COUNT(*) INTO @verified_on_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND column_name = 'verified_on';

SET @verified_on_sql = IF(@verified_on_exists = 0,
  'ALTER TABLE users ADD COLUMN verified_on TIMESTAMP NULL COMMENT ''When was mobile verification completed?''',
  'SELECT 1');

PREPARE verified_on_stmt FROM @verified_on_sql;
EXECUTE verified_on_stmt;
DEALLOCATE PREPARE verified_on_stmt;

SELECT COUNT(*) INTO @firebase_uid_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND column_name = 'firebase_uid';

SET @firebase_uid_sql = IF(@firebase_uid_exists = 0,
  'ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(255) NULL COMMENT ''Firebase UID for phone authentication''',
  'SELECT 1');

PREPARE firebase_uid_stmt FROM @firebase_uid_sql;
EXECUTE firebase_uid_stmt;
DEALLOCATE PREPARE firebase_uid_stmt;

SELECT COUNT(*) INTO @firebase_uid_index_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND index_name = 'uk_users_firebase_uid';

SET @firebase_uid_index_sql = IF(@firebase_uid_index_exists = 0,
  'ALTER TABLE users ADD UNIQUE INDEX uk_users_firebase_uid (firebase_uid)',
  'SELECT 1');

PREPARE firebase_uid_index_stmt FROM @firebase_uid_index_sql;
EXECUTE firebase_uid_index_stmt;
DEALLOCATE PREPARE firebase_uid_index_stmt;

-- Step 2: Create indexes for performance
-- =====================================================================
SELECT COUNT(*) INTO @firebase_uid_idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND index_name = 'idx_firebase_uid';

SET @firebase_uid_idx_sql = IF(@firebase_uid_idx_exists = 0,
  'CREATE INDEX idx_firebase_uid ON users(firebase_uid)',
  'SELECT 1');

PREPARE firebase_uid_idx_stmt FROM @firebase_uid_idx_sql;
EXECUTE firebase_uid_idx_stmt;
DEALLOCATE PREPARE firebase_uid_idx_stmt;

SELECT COUNT(*) INTO @mobile_verified_idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND index_name = 'idx_mobile_verified';

SET @mobile_verified_idx_sql = IF(@mobile_verified_idx_exists = 0,
  'CREATE INDEX idx_mobile_verified ON users(mobile_verified)',
  'SELECT 1');

PREPARE mobile_verified_idx_stmt FROM @mobile_verified_idx_sql;
EXECUTE mobile_verified_idx_stmt;
DEALLOCATE PREPARE mobile_verified_idx_stmt;

SELECT COUNT(*) INTO @verified_on_idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND index_name = 'idx_verified_on';

SET @verified_on_idx_sql = IF(@verified_on_idx_exists = 0,
  'CREATE INDEX idx_verified_on ON users(verified_on)',
  'SELECT 1');

PREPARE verified_on_idx_stmt FROM @verified_on_idx_sql;
EXECUTE verified_on_idx_stmt;
DEALLOCATE PREPARE verified_on_idx_stmt;

-- Step 3: Update existing users (optional - backfill for existing users)
-- =====================================================================
-- Option 1: Mark all existing users as verified (if you trust them)
-- UPDATE users SET mobile_verified = TRUE, verified_on = NOW() WHERE active = TRUE;

-- Option 2: Keep them unverified (recommended for security)
-- No action needed - defaults to FALSE

-- Step 4: Verify schema changes
-- =====================================================================
-- Run this to verify the columns were added correctly:
-- DESC users;
-- SHOW COLUMNS FROM users;

-- Step 5: Verify indexes
-- =====================================================================
-- Run this to verify indexes:
-- SHOW INDEX FROM users;

-- =====================================================================
-- ROLLBACK SCRIPT (if needed)
-- =====================================================================
-- ALTER TABLE users DROP COLUMN mobile_verified;
-- ALTER TABLE users DROP COLUMN verified_on;
-- ALTER TABLE users DROP COLUMN firebase_uid;
-- DROP INDEX idx_firebase_uid ON users;
-- DROP INDEX idx_mobile_verified ON users;
-- DROP INDEX idx_verified_on ON users;
