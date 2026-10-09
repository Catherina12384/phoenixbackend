-- Assumes a fresh database (no users without a phone number).

-- ---------- users: STAFF role, account state, mandatory unique phone ----------
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'));

ALTER TABLE users
    ADD COLUMN active               BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN token_version        INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users ALTER COLUMN phone SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_phone_format CHECK (phone ~ '^\+91[6-9][0-9]{9}$');
CREATE UNIQUE INDEX uq_users_phone       ON users (phone);
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- ---------- OTP challenges (one live OTP per purpose + phone; only an HMAC hash is stored) ----------
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

-- ---------- Signups waiting for OTP verification (password already BCrypt-hashed) ----------
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

-- ---------- Rate limiting counters (fixed window per key) ----------
CREATE TABLE rate_limits (
    limit_key    VARCHAR(300) PRIMARY KEY,
    window_start TIMESTAMPTZ  NOT NULL,
    hits         INTEGER      NOT NULL
);
CREATE INDEX idx_rate_limits_window ON rate_limits (window_start);

-- ---------- Audit trail ----------
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
ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE r text;
BEGIN
    FOREACH r IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA public FROM %I', r);
            EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM %I', r);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM %I', r);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM %I', r);
        END IF;
    END LOOP;
END $$;
