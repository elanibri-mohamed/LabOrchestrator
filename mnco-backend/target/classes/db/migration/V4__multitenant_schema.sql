-- ============================================================
-- MNCO Platform - Multi-tenant Architecture Schema
-- Version: V4
-- ============================================================

-- 1. Update user roles to include TEACHER
-- Note: Check if the role was an enum or varchar. V1 used VARCHAR(20) for simplicity in some setups, 
-- but if it's used as an enum in Java, we just ensure the data is consistent.
-- Current project uses STUDENT and ADMIN.

-- 2. Refactor Labs to Lab Templates
-- Add columns to labs before renaming (if columns don't exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'labs' AND table_schema = 'public') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'labs' AND column_name = 'cpu_allocated') THEN
            ALTER TABLE labs ADD COLUMN cpu_allocated INTEGER NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'labs' AND column_name = 'ram_allocated') THEN
            ALTER TABLE labs ADD COLUMN ram_allocated INTEGER NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'labs' AND column_name = 'storage_allocated') THEN
            ALTER TABLE labs ADD COLUMN storage_allocated INTEGER NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'labs' AND column_name = 'eve_template_path') THEN
            ALTER TABLE labs ADD COLUMN eve_template_path VARCHAR(512) NOT NULL DEFAULT '';
        END IF;
    END IF;
END $$;

-- Only rename if labs exists and lab_templates doesn't
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'labs' AND table_schema = 'public')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'lab_templates' AND table_schema = 'public') THEN
        ALTER TABLE labs RENAME TO lab_templates;
    END IF;
END $$;

-- Add template status (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lab_template_status') THEN
        CREATE TYPE lab_template_status AS ENUM ('ACTIVE', 'REMOVED');
    END IF;
END $$;

-- Add columns to lab_templates if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'status') THEN
        ALTER TABLE lab_templates ADD COLUMN status lab_template_status NOT NULL DEFAULT 'ACTIVE';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'last_synced_at') THEN
        ALTER TABLE lab_templates ADD COLUMN last_synced_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'cpu_allocated') THEN
        ALTER TABLE lab_templates ADD COLUMN cpu_allocated INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'ram_allocated') THEN
        ALTER TABLE lab_templates ADD COLUMN ram_allocated INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'storage_allocated') THEN
        ALTER TABLE lab_templates ADD COLUMN storage_allocated INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_templates' AND column_name = 'eve_template_path') THEN
        ALTER TABLE lab_templates ADD COLUMN eve_template_path VARCHAR(512) NOT NULL DEFAULT '';
    END IF;
END $$;

-- 3. Create Lab Assignments
CREATE TABLE IF NOT EXISTS lab_assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID        NOT NULL REFERENCES lab_templates(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID        NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (template_id, user_id)
);

-- 4. Create Lab Instances
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'instance_status') THEN
        CREATE TYPE instance_status AS ENUM ('STOPPED', 'RUNNING', 'ERROR');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS lab_instances (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id       UUID            NOT NULL REFERENCES lab_templates(id),
    user_id           UUID            NOT NULL REFERENCES users(id),
    eve_instance_path VARCHAR(512)    NOT NULL UNIQUE,
    status            instance_status NOT NULL DEFAULT 'STOPPED',
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    started_at        TIMESTAMPTZ,
    stopped_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    UNIQUE (template_id, user_id)
);

-- Add updated_at column if it doesn't exist (for schema validation)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_instances' AND column_name = 'updated_at') THEN
        ALTER TABLE lab_instances ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;
END $$;

-- 5. Create Console Access Log
CREATE TABLE IF NOT EXISTS console_access_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    instance_id UUID        NOT NULL REFERENCES lab_instances(id),
    node_id     VARCHAR(64) NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. Indexes for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_assignments_user      ON lab_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_assignments_template  ON lab_assignments(template_id);
CREATE INDEX IF NOT EXISTS idx_instances_user_tpl    ON lab_instances(user_id, template_id);
CREATE INDEX IF NOT EXISTS idx_console_log_instance  ON console_access_log(instance_id);
CREATE INDEX IF NOT EXISTS idx_console_log_user      ON console_access_log(user_id);
