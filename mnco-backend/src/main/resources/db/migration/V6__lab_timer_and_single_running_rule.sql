-- ============================================================
-- MNCO Platform - Lab Timer and Single Running Lab Guard
-- Version: V6
-- ============================================================

-- Add expiration timestamp for 1-hour lab timer.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lab_instances'
          AND column_name = 'expires_at'
    ) THEN
        ALTER TABLE lab_instances ADD COLUMN expires_at TIMESTAMPTZ;
    END IF;
END;
$$;

-- Query performance for timer scheduler and single-running checks.
CREATE INDEX IF NOT EXISTS idx_lab_instances_user_status
    ON lab_instances(user_id, status);

CREATE INDEX IF NOT EXISTS idx_lab_instances_status_expires_at
    ON lab_instances(status, expires_at);
