-- ============================================================
-- MNCO Platform - EVE-NG Lab Discovery Support
-- Version: V4_1
-- Adds support for syncing labs from EVE-NG server (read-only mode)
-- ============================================================

-- Add columns to lab_templates table to support EVE-NG lab discovery
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lab_templates'
          AND column_name = 'synced_from_eveng'
    ) THEN
        ALTER TABLE lab_templates ADD COLUMN synced_from_eveng BOOLEAN NOT NULL DEFAULT false;
        CREATE INDEX idx_lab_templates_synced_from_eveng ON lab_templates(synced_from_eveng);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lab_templates'
          AND column_name = 'external_metadata'
    ) THEN
        ALTER TABLE lab_templates ADD COLUMN external_metadata TEXT;
    END IF;
END;
$$;

-- Ensure eveng_lab_id index is unique for discovery lookups (optional: enforce uniqueness)
-- Commented out to allow multiple labs to reference same path in edge cases
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_lab_templates_eveng_lab_id_unique ON lab_templates(eveng_lab_id) WHERE eveng_lab_id IS NOT NULL;

-- Create index for efficient lookups by synced_from_eveng + status (discovery queries)
CREATE INDEX IF NOT EXISTS idx_lab_templates_synced_status ON lab_templates(synced_from_eveng, status);

COMMENT ON COLUMN lab_templates.synced_from_eveng IS 'Flag indicating if lab was discovered from EVE-NG server (true) or created locally (false)';
COMMENT ON COLUMN lab_templates.external_metadata IS 'JSON metadata from EVE-NG (e.g., nodeCount, discoveredAt timestamp)';
