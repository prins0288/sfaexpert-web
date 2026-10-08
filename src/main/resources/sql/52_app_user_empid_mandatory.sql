-- ============================================================================
-- 52_app_user_empid_mandatory.sql  —  COMMON db (sfa_central)
-- Make app_user.emp_id MANDATORY (every login is linked to an employee).
-- Existing rows with no emp_id are backfilled from username first so the
-- NOT NULL change can apply.
-- ============================================================================

UPDATE app_user SET emp_id = username WHERE emp_id IS NULL OR emp_id = '';

ALTER TABLE app_user MODIFY emp_id VARCHAR(40) NOT NULL;
