-- ============================================================================
-- 54_emp_detail_emp_id.sql   (run against each TENANT db, e.g. acme_db)
-- ----------------------------------------------------------------------------
-- emp_detail gets a second identity column, emp_id.
--
--   emp_id   = the DEVELOPER / system-facing stable key. Never changes, never
--              reformatted. This is what the login row links to
--              (sfa_central.user_login_master.emp_id -> emp_detail.emp_id).
--   emp_code = the COMPANY-facing display code (AWT201). Its format is
--              customisable per tenant (empcode.prefix / width / suffix) and may
--              be regenerated, so it is NOT safe as the auth link.
--
-- Existing rows: emp_id is backfilled from emp_code (they were the same until now).
-- ============================================================================

ALTER TABLE emp_detail
    ADD COLUMN emp_id VARCHAR(40) NULL AFTER id;

UPDATE emp_detail SET emp_id = emp_code WHERE emp_id IS NULL OR emp_id = '';

ALTER TABLE emp_detail
    MODIFY COLUMN emp_id VARCHAR(40) NOT NULL,
    ADD UNIQUE KEY uq_emp_detail_emp_id (emp_id);
