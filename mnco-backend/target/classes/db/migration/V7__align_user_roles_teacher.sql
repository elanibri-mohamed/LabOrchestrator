-- ============================================================
-- MNCO Platform - Align User Roles With Application Enum
-- Version: V7
-- ============================================================

-- Normalize legacy role value before applying new check constraint.
UPDATE users
SET role = 'TEACHER'
WHERE role = 'INSTRUCTOR';

-- Replace old role constraint if present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_role'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users DROP CONSTRAINT chk_role;
    END IF;
END;
$$;

-- Keep RESEARCHER allowed for backward compatibility if rows still exist.
ALTER TABLE users
ADD CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT', 'RESEARCHER'));
