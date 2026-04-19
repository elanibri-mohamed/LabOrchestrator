-- ============================================================
-- MNCO Platform - Audit Log & Refresh Token Schema
-- Version: V3
-- ============================================================

-- Ensure audit_logs has the JPA-mapped columns required by the current entity.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'event_type'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'actor_id'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN actor_id UUID REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'actor_username'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN actor_username VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'lab_id'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN lab_id UUID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'lab_name'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN lab_name VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'result'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'error_code'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN error_code VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'user_agent'
    ) THEN
        ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(255);
    END IF;
END;
$$;

-- Enforce immutability at the database level.
CREATE OR REPLACE RULE audit_log_no_update AS
    ON UPDATE TO audit_logs DO INSTEAD NOTHING;

CREATE OR REPLACE RULE audit_log_no_delete AS
    ON DELETE TO audit_logs DO INSTEAD NOTHING;

CREATE INDEX IF NOT EXISTS idx_audit_actor_id   ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_lab_id     ON audit_logs(lab_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action     ON audit_logs(event_type);

-- Ensure refresh_tokens index coverage and cleanup helper.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token      ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM refresh_tokens WHERE expires_at < NOW() AND revoked = true;
END;
$$ LANGUAGE plpgsql;
