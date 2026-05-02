-- ============================================================
-- MNCO Platform - Add EVE-NG Credential Storage
-- Version: V8
-- 
-- Purpose: Add encrypted EVE-NG password column to support
--          per-user EVE-NG authentication credentials
-- ============================================================

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS eveng_password_encrypted VARCHAR(512);
