-- Add plan_id column to subscriptions if it doesn't exist
-- (Hibernate ddl-auto may have already added it)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'subscriptions'
      AND COLUMN_NAME = 'plan_id');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE subscriptions ADD COLUMN plan_id char(36) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove subscriptions with invalid/empty plan_id that cannot reference a valid plan
DELETE FROM subscriptions WHERE plan_id IS NULL OR TRIM(plan_id) = '';

-- Ensure column is NOT NULL
ALTER TABLE subscriptions MODIFY COLUMN plan_id char(36) NOT NULL;

-- Add FK constraint if not already present
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'subscriptions'
      AND CONSTRAINT_NAME = 'fk_subscription_plan');

SET @sql2 = IF(@fk_exists = 0,
    'ALTER TABLE subscriptions ADD CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;