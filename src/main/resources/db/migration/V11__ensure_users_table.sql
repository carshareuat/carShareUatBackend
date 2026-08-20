-- Idempotent migration to ensure core `users` table exists so application bootstrap can create admin
CREATE TABLE IF NOT EXISTS users (
  id CHAR(36) PRIMARY KEY,
  role VARCHAR(20) NOT NULL,
  mobile VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ensure index on mobile exists (MySQL will ignore if already present)
CREATE INDEX IF NOT EXISTS idx_users_mobile ON users(mobile);
