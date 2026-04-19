-- ============================================================
-- MNCO Platform - Seed Default Admin User
-- Version: V2
--
-- Default credentials (CHANGE IN PRODUCTION):
--   username : admin
--   email    : admin@mnco.internal
--   password : Admin@1234
--   password hash generated with BCrypt strength=12
-- ============================================================

INSERT INTO users (id, username, email, password, role, enabled, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@mnco.internal',
    '$2a$12$Hf3kVDhbMDmUYv7xPZlFVOy3zrX1iQvEbqE9WkHjN4QcL0M2sHJam',
    'ADMIN',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

-- Seed a default public lab template for onboarding
INSERT INTO lab_templates (id, name, description, topology_yaml, version, is_public, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Basic Router-Switch Lab',
    'A minimal topology with one router and one switch — ideal for routing and switching fundamentals.',
    E'---\nname: basic-router-switch\nnodes:\n  - name: R1\n    type: iol\n    image: i86bi_linux-adventerprisek9-ms.156-1.T\n    cpu: 1\n    ram: 256\n  - name: SW1\n    type: iol\n    image: i86bi_linux-l2-adventerprisek9-ms.SSA.high_iron\n    cpu: 1\n    ram: 256\nlinks:\n  - from: R1:e0/0\n    to: SW1:e0/0\n',
    '1.0',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO lab_templates (id, name, description, topology_yaml, version, is_public, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'OSPF Multi-Area Lab',
    'Three-area OSPF topology for practising inter-area routing and ABR/ASBR configuration.',
    E'---\nname: ospf-multi-area\nnodes:\n  - name: R-ABR1\n    type: iol\n    cpu: 1\n    ram: 256\n  - name: R-Area1\n    type: iol\n    cpu: 1\n    ram: 256\n  - name: R-Area2\n    type: iol\n    cpu: 1\n    ram: 256\nlinks:\n  - from: R-ABR1:e0/0\n    to: R-Area1:e0/0\n  - from: R-ABR1:e0/1\n    to: R-Area2:e0/0\n',
    '1.0',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (name) DO NOTHING;
