-- ============================================================
-- MNCO Platform - Multi-tenant Architecture Schema
-- Version: V4
-- ============================================================

-- 1. Update user roles to include TEACHER
-- Note: Check if the role was an enum or varchar. V1 used VARCHAR(20) for simplicity in some setups, 
-- but if it's used as an enum in Java, we just ensure the data is consistent.
-- Current project uses STUDENT and ADMIN.

-- 2. Refactor Labs to Lab Templates
-- Rename existing 'labs' to 'lab_templates'
ALTER TABLE labs RENAME TO lab_templates;

-- Add template status
CREATE TYPE lab_template_status AS ENUM ('ACTIVE', 'REMOVED');
ALTER TABLE lab_templates ADD COLUMN status lab_template_status NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE lab_templates ADD COLUMN last_synced_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 3. Create Lab Assignments
CREATE TABLE lab_assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID        NOT NULL REFERENCES lab_templates(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID        NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (template_id, user_id)
);

-- 4. Create Lab Instances
CREATE TYPE instance_status AS ENUM ('STOPPED', 'RUNNING', 'ERROR');

CREATE TABLE lab_instances (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id       UUID            NOT NULL REFERENCES lab_templates(id),
    user_id           UUID            NOT NULL REFERENCES users(id),
    eve_instance_path VARCHAR(512)    NOT NULL UNIQUE,
    status            instance_status NOT NULL DEFAULT 'STOPPED',
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    started_at        TIMESTAMPTZ,
    stopped_at        TIMESTAMPTZ,
    UNIQUE (template_id, user_id)
);

-- 5. Create Console Access Log
CREATE TABLE console_access_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    instance_id UUID        NOT NULL REFERENCES lab_instances(id),
    node_id     VARCHAR(64) NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. Indexes for performance
CREATE INDEX idx_assignments_user      ON lab_assignments(user_id);
CREATE INDEX idx_assignments_template  ON lab_assignments(template_id);
CREATE INDEX idx_instances_user_tpl    ON lab_instances(user_id, template_id);
CREATE INDEX idx_console_log_instance  ON console_access_log(instance_id);
CREATE INDEX idx_console_log_user      ON console_access_log(user_id);
