alter table users add column gender varchar(10);

alter table rides add column female_only boolean not null default false;

-- Backfill: set gender null for existing users (already null by default)
-- Backfill: existing rides are not female-only (default false)
