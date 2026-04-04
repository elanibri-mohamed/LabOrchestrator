-- ============================================================
-- MNCO Platform - Initial Schema Migration
-- Version: V1
-- ============================================================

-- Users table
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'INSTRUCTOR', 'STUDENT', 'RESEARCHER'))
);

-- Resource Quota table (one per user)
CREATE TABLE resource_quotas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    max_labs        INT          NOT NULL DEFAULT 3,
    max_cpu         INT          NOT NULL DEFAULT 8,
    max_ram_gb      INT          NOT NULL DEFAULT 16,
    max_storage_gb  INT          NOT NULL DEFAULT 50,
    used_labs       INT          NOT NULL DEFAULT 0,
    used_cpu        INT          NOT NULL DEFAULT 0,
    used_ram_gb     INT          NOT NULL DEFAULT 0,
    used_storage_gb INT          NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Lab Templates table
CREATE TABLE lab_templates (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100) NOT NULL UNIQUE,
    description    TEXT,
    topology_yaml  TEXT,
    version        VARCHAR(20)  NOT NULL DEFAULT '1.0',
    author_id      UUID         REFERENCES users(id) ON DELETE SET NULL,
    is_public      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Labs table
CREATE TABLE labs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    owner_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id     UUID         REFERENCES lab_templates(id) ON DELETE SET NULL,
    eveng_lab_id    VARCHAR(255),
    eveng_node_id   VARCHAR(255),
    cpu_allocated   INT          NOT NULL DEFAULT 0,
    ram_allocated   INT          NOT NULL DEFAULT 0,
    storage_allocated INT        NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ,
    stopped_at      TIMESTAMPTZ,
    last_active_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CREATING', 'RUNNING', 'STOPPING', 'STOPPED', 'DELETING', 'ERROR', 'DELETED'))
);

-- Resources table (hardware resources for labs)
CREATE TABLE resources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id          UUID         NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    cpu             INT          NOT NULL DEFAULT 1,
    ram             INT          NOT NULL DEFAULT 1,
    storage         INT          NOT NULL DEFAULT 10,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Audit Log table
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   VARCHAR(255),
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Refresh Tokens table
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_labs_owner_id ON labs(owner_id);
CREATE INDEX idx_labs_status ON labs(status);
CREATE INDEX idx_labs_eveng_lab_id ON labs(eveng_lab_id);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_labs_updated_at BEFORE UPDATE ON labs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lab_templates_updated_at BEFORE UPDATE ON lab_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_resource_quotas_updated_at BEFORE UPDATE ON resource_quotas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
