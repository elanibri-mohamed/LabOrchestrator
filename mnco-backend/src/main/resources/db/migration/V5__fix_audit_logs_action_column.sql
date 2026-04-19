-- ============================================================
-- MNCO Platform - Fix Audit Logs Action Column
-- Version: V5
-- ============================================================
-- The audit_logs table had an 'action' column that's NOT NULL
-- but the AuditLogJpaEntity doesn't map to it.
-- Solution: Drop or set default for the 'action' column

-- Check if action column exists and drop it (or set default)
DO $$
BEGIN
    -- If the action column exists and is causing constraint violations,
    -- we have two options:
    -- Option 1: Drop it (if it's duplicate of event_type)
    -- Option 2: Set a default and make it nullable
    
    -- First, update any existing NULL values to 'SYSTEM_ACTION' 
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
          AND column_name = 'action'
    ) THEN
        -- Update NULL values with a default
        UPDATE audit_logs SET action = 'SYSTEM_ACTION' WHERE action IS NULL;
        
        -- Now alter the column to allow NULL (less restrictive)
        ALTER TABLE audit_logs ALTER COLUMN action DROP NOT NULL;
    END IF;
END;
$$;

-- Ensure event_type is properly populated for consistency
UPDATE audit_logs SET event_type = 'UNKNOWN' WHERE event_type IS NULL;
