-- ============================================================
-- MNCO Platform - Multi-Tenant Lab Assignment & Instance Schema
-- Version: V4
-- Purpose: Implement reference architecture for EVE-NG template/assignment/instance isolation
--          using separate tables to coexist with existing single-tenant labs.
-- ============================================================

-- ── ENUM TYPES ───────────────────────────────────────────────────────────────

CREATE TYPE lab_template_status AS ENUM ('ACTIVE', 'REMOVED');
CREATE TYPE instance_status     AS ENUM ('STOPPED', 'RUNNING', 'ERROR');
-- Note: Using existing UserRole enum (ADMIN, INSTRUCTOR, STUDENT, RESEARCHER)
-- INSTRUCTOR is equivalent to TEACHER in the reference architecture

-- ── EVENG_TEMPLATES TABLE ────────────────────────────────────────────────────
-- Replicates the reference `labs` table (templates synced from EVE-NG GUI).
-- This is a NEW table; existing `labs` table remains for legacy single-tenant labs.
-- Each row represents one .unl file in /opt/unetlab/labs/templates/.

CREATE TABLE IF NOT EXISTS eveng_templates (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(128) NOT NULL,
    eveng_template_path    VARCHAR(512) NOT NULL UNIQUE,
    template_status        lab_template_status NOT NULL DEFAULT 'ACTIVE',
    last_synced_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    description            TEXT,
    CONSTRAINT uq_template_name UNIQUE (name)
);

-- ── LAB_ASSIGNMENTS TABLE ────────────────────────────────────────────────────
-- Grants a user access to an EVE-NG template. One row per (template, user).

CREATE TABLE IF NOT EXISTS lab_assignments (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id            UUID NOT NULL REFERENCES eveng_templates(id) ON DELETE CASCADE,
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by            UUID NOT NULL REFERENCES users(id),
    assigned_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_template_user UNIQUE (template_id, user_id)
);

-- ── LAB_INSTANCES TABLE ──────────────────────────────────────────────────────
-- One private copy of a template per user, created lazily on first start.
-- Each instance corresponds to a .unl file in /opt/unetlab/labs/instances/{userId}/{templateId}.unl

CREATE TABLE IF NOT EXISTS lab_instances (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id            UUID NOT NULL REFERENCES eveng_templates(id) ON DELETE CASCADE,
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    eveng_instance_path    VARCHAR(512) NOT NULL UNIQUE,
    status                 instance_status NOT NULL DEFAULT 'STOPPED',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at             TIMESTAMPTZ,
    stopped_at             TIMESTAMPTZ,
    CONSTRAINT uq_user_template UNIQUE (template_id, user_id)
);

-- ── CONSOLE_ACCESS_LOG TABLE ─────────────────────────────────────────────────
-- Tracks every console connection attempt for progress monitoring.

CREATE TABLE IF NOT EXISTS console_access_log (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    instance_id            UUID NOT NULL REFERENCES lab_instances(id) ON DELETE CASCADE,
    node_id                VARCHAR(64) NOT NULL,
    accessed_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── INDEXES FOR PERFORMANCE ──────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_assignments_user      ON lab_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_assignments_template  ON lab_assignments(template_id);
CREATE INDEX IF NOT EXISTS idx_instances_user        ON lab_instances(user_id);
CREATE INDEX IF NOT EXISTS idx_instances_template    ON lab_instances(template_id);
CREATE INDEX IF NOT EXISTS idx_console_log_instance  ON console_access_log(instance_id);
CREATE INDEX IF NOT EXISTS idx_console_log_user      ON console_access_log(user_id);
CREATE INDEX IF NOT EXISTS idx_console_log_accessed  ON console_access_log(accessed_at DESC);

-- ── TRIGGERS ─────────────────────────────────────────────────────────────────

-- Auto-update updated_at on templates when modified via application logic
-- (Application layer will handle; DB trigger provided for integrity if direct SQL used)
CREATE OR REPLACE FUNCTION update_eveng_template_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_synced_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to eveng_templates for any update to refresh last_synced_at
DROP TRIGGER IF EXISTS trigger_eveng_template_updated ON eveng_templates;
CREATE TRIGGER trigger_eveng_template_updated
    BEFORE UPDATE ON eveng_templates
    FOR EACH ROW EXECUTE FUNCTION update_eveng_template_updated_at();

-- ── COMMENTS ─────────────────────────────────────────────────────────────────

COMMENT ON COLUMN eveng_templates.template_status IS 'ACTIVE = available for assignment, REMOVED = deleted from EVE-NG GUI but assignments preserved';
COMMENT ON COLUMN eveng_templates.eveng_template_path IS 'Full path to .unl file in EVE-NG templates directory (e.g., /opt/unetlab/labs/templates/lab.unl)';
COMMENT ON COLUMN lab_assignments.assigned_by IS 'User ID who granted this assignment (ADMIN or INSTRUCTOR/TEACHER)';
COMMENT ON COLUMN lab_instances.eveng_instance_path IS 'Full path to instance .unl file in EVE-NG instances directory (e.g., /opt/unetlab/labs/instances/{userId}/{templateId}.unl)';
COMMENT ON COLUMN lab_instances.status IS 'STOPPED = powered off, RUNNING = active, ERROR = EVE-NG failure';
COMMENT ON TABLE eveng_templates IS 'EVE-NG lab templates synced from the EVE-NG GUI (read-only source of truth for topology)';
COMMENT ON TABLE lab_assignments IS 'Multi-tenant access control: one row per (template, user) grant; tenant boundary';
COMMENT ON TABLE lab_instances IS 'Per-user private copy of a template; created lazily on first start; isolated via separate .unl file';

-- ── REFERENCE MAPPING NOTE ───────────────────────────────────────────────────
-- Reference table   → Our table
-- ------------------------------
-- labs              → eveng_templates
-- lab_assignments   → lab_assignments (same name, but FK to eveng_templates)
-- lab_instances     → lab_instances (same name, FK to eveng_templates)
-- console_access_log → console_access_log (same)
--
-- The existing `labs` table (single-tenant) remains untouched and may be deprecated.
