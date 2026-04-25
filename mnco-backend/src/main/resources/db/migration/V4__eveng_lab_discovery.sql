-- ============================================================
-- MNCO Platform - EVE-NG Lab Discovery Support
-- Version: V4
-- Adds support for syncing labs from EVE-NG server (read-only mode)
-- ============================================================

-- Add columns to labs table to support EVE-NG lab discovery
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'labs'
          AND column_name = 'synced_from_eveng'
    ) THEN
        ALTER TABLE labs ADD COLUMN synced_from_eveng BOOLEAN NOT NULL DEFAULT false;
        CREATE INDEX idx_labs_synced_from_eveng ON labs(synced_from_eveng);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'labs'
          AND column_name = 'external_metadata'
    ) THEN
        ALTER TABLE labs ADD COLUMN external_metadata TEXT;
    END IF;
END;
$$;

-- Ensure eveng_lab_id index is unique for discovery lookups (optional: enforce uniqueness)
-- Commented out to allow multiple labs to reference same path in edge cases
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_labs_eveng_lab_id_unique ON labs(eveng_lab_id) WHERE eveng_lab_id IS NOT NULL;

-- Create index for efficient lookups by synced_from_eveng + status (discovery queries)
CREATE INDEX IF NOT EXISTS idx_labs_synced_status ON labs(synced_from_eveng, status);

COMMENT ON COLUMN labs.synced_from_eveng IS 'Flag indicating if lab was discovered from EVE-NG server (true) or created locally (false)';
COMMENT ON COLUMN labs.external_metadata IS 'JSON metadata from EVE-NG (e.g., nodeCount, discoveredAt timestamp)';
