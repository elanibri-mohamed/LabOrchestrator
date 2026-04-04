-- ============================================================
-- MNCO Platform - Audit Log & Refresh Token Schema
-- Version: V3
-- ============================================================

-- Append-only audit log (FR-AA-07, FR-LM-10)
-- Note: The DB-level rule below prevents UPDATE/DELETE.
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(30) NOT NULL,
    actor_id        UUID        REFERENCES users(id) ON DELETE SET NULL,
    actor_username  VARCHAR(100),
    lab_id          UUID,
    lab_name        VARCHAR(100),
    result          VARCHAR(10) NOT NULL CHECK (result IN ('SUCCESS', 'FAILURE')),
    error_code      VARCHAR(100),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enforce immutability at the database level
CREATE OR REPLACE RULE audit_log_no_update AS
    ON UPDATE TO audit_logs DO INSTEAD NOTHING;

CREATE OR REPLACE RULE audit_log_no_delete AS
    ON DELETE TO audit_logs DO INSTEAD NOTHING;

CREATE INDEX IF NOT EXISTS idx_audit_actor_id   ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_lab_id     ON audit_logs(lab_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type);

-- Refresh tokens (FR-AA-04)
-- Already defined in V1, but ensure it exists cleanly
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_val ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_user_id   ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires_at ON refresh_tokens(expires_at);

-- Auto-cleanup function for expired refresh tokens
CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM refresh_tokens WHERE expires_at < NOW() AND revoked = true;
END;
$$ LANGUAGE plpgsql;
