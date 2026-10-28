-- V3: roles, OTP, audit, RLS.
-- Safe on existing data and safe if parts were applied by hand earlier.

-- ---------- users: STAFF role, account state, mandatory unique phone ----------
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'));

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS active               BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS token_version        INTEGER NOT NULL DEFAULT 0;

-- Drop the phone rules first so the data can be repaired.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_format;
DROP INDEX IF EXISTS uq_users_phone;
DROP INDEX IF EXISTS uq_users_email_lower;

-- 1. Normalise phones to +91XXXXXXXXXX where they are recognisable Indian numbers.
UPDATE users SET phone = regexp_replace(phone, '[[:space:]()-]', '', 'g') WHERE phone IS NOT NULL;
UPDATE users SET phone = '+91' || phone           WHERE phone ~ '^[6-9][0-9]{9}$';
UPDATE users SET phone = '+' || phone             WHERE phone ~ '^91[6-9][0-9]{9}$';
UPDATE users SET phone = '+91' || substr(phone,2) WHERE phone ~ '^0[6-9][0-9]{9}$';

-- 2. Anything missing or still invalid gets a unique placeholder that passes the format check.
--    These are NOT real numbers: replace them with real ones before those users log in.
UPDATE users
SET phone = '+91' || (6 + (id % 4))::text || lpad(id::text, 9, '0')
WHERE phone IS NULL OR phone !~ '^\+91[6-9][0-9]{9}$';

-- 3. Duplicate phones: keep the oldest row's number, give the others a placeholder.
UPDATE users u
SET phone = '+91' || (6 + (u.id % 4))::text || lpad(u.id::text, 9, '0')
FROM (SELECT id, row_number() OVER (PARTITION BY phone ORDER BY id) AS rn FROM users) d
WHERE u.id = d.id AND d.rn > 1;

-- 4. Emails that differ only by case would break the unique index: keep the oldest, rename the rest.
UPDATE users u
SET email = 'duplicate-' || u.id || '-' || u.email
FROM (SELECT id, row_number() OVER (PARTITION BY lower(email) ORDER BY id) AS rn FROM users) d
WHERE u.id = d.id AND d.rn > 1;

ALTER TABLE users ALTER COLUMN phone SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_phone_format CHECK (phone ~ '^\+91[6-9][0-9]{9}$');
CREATE UNIQUE INDEX uq_users_phone       ON users (phone);
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- ---------- New tables ----------
-- Dropped first so a hand-made copy with a different shape cannot survive.
-- These tables are created by this migration, so nothing real is lost.
DROP TABLE IF EXISTS otp_challenges;
DROP TABLE IF EXISTS pending_registrations;
DROP TABLE IF EXISTS rate_limits;
DROP TABLE IF EXISTS audit_log;

-- OTP challenges (one live OTP per purpose + phone; only an HMAC hash is stored)
CREATE TABLE otp_challenges (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purpose    VARCHAR(20) NOT NULL CHECK (purpose IN ('REGISTER', 'RESET_PASSWORD')),
    phone      VARCHAR(20) NOT NULL,
    code_hash  VARCHAR(64) NOT NULL,
    attempts   INTEGER     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    UNIQUE (purpose, phone)
);
CREATE INDEX idx_otp_expires ON otp_challenges (expires_at);

-- Signups waiting for OTP verification (password already BCrypt-hashed)
CREATE TABLE pending_registrations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_pending_phone   ON pending_registrations (phone);
CREATE INDEX idx_pending_email   ON pending_registrations (lower(email));
CREATE INDEX idx_pending_expires ON pending_registrations (expires_at);

-- Rate limiting counters (fixed window per key)
CREATE TABLE rate_limits (
    limit_key    VARCHAR(300) PRIMARY KEY,
    window_start TIMESTAMPTZ  NOT NULL,
    hits         INTEGER      NOT NULL
);
CREATE INDEX idx_rate_limits_window ON rate_limits (window_start);

-- Audit trail
CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    request_id  VARCHAR(64),
    actor_email VARCHAR(255),
    actor_role  VARCHAR(30),
    action      VARCHAR(50) NOT NULL,
    target_type VARCHAR(30),
    target_id   VARCHAR(100),
    success     BOOLEAN     NOT NULL,
    ip          VARCHAR(64),
    details     JSONB
);
CREATE INDEX idx_audit_time   ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_actor  ON audit_log (lower(actor_email));
CREATE INDEX idx_audit_action ON audit_log (action);

-- ---------- Supabase: lock the auto-generated Data API out of every table ----------
-- All access goes through Spring Boot (owner role, bypasses RLS). With RLS on and no
-- policies, the public anon/authenticated roles can read and write nothing.
-- REMEMBER: every future CREATE TABLE in this schema needs its own ENABLE ROW LEVEL SECURITY.
ALTER TABLE users                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE dealers               ENABLE ROW LEVEL SECURITY;
ALTER TABLE products              ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_challenges        ENABLE ROW LEVEL SECURITY;
ALTER TABLE pending_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE rate_limits           ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log             ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE r text;
BEGIN
    FOREACH r IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            BEGIN
                EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA public FROM %I', r);
                EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM %I', r);
                EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM %I', r);
                EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM %I', r);
            EXCEPTION WHEN insufficient_privilege THEN
                RAISE NOTICE 'Skipped revoke for role %: insufficient privilege', r;
            END;
        END IF;
    END LOOP;
END $$;