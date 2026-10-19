-- ===========================================================================
-- 77_client_approval_flow.sql — PER TENANT database
--
-- Client Master approval in steps. Each company picks its flow in
-- Settings > General > "Client approval" (company setting clientApprovalFlow):
--   AUTO           new client is approved at once
--   MANAGER        the field employee's manager approves -> approved
--   ADMIN          an admin approves -> approved
--   MANAGER_ADMIN  manager approves, then an admin gives the final approval (default)
-- approval_status: PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED
-- (is_approved stays = approval_status 'APPROVED', for reports).
-- Who may approve is decided by role (emp level) and team, so the permission
-- CLIENT_MASTER_APPROVE is now allowed by default (deny it for someone to block them).
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/77_client_approval_flow.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approval_status');
SET @new_approval_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''PENDING_MANAGER'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_approved', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_by');
SET @new_manager_approved_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_on');
SET @new_manager_approved_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_by');
SET @new_rejected_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_on');
SET @new_rejected_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'reject_reason');
SET @new_reject_reason := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- first run only: rows approved before keep APPROVED, the rest wait for the manager
UPDATE client_master SET approval_status = IF(is_approved = 1, 'APPROVED', 'PENDING_MANAGER') WHERE @new_approval_status;
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND INDEX_NAME = 'idx_client_master_approval');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD KEY idx_client_master_approval (approval_status)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE permission_master SET default_allowed = 1, description = 'Approve / reject clients and decide delete requests (who may approve which step is set by role and team)'
WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== globex_db ====================
USE globex_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approval_status');
SET @new_approval_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''PENDING_MANAGER'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_approved', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_by');
SET @new_manager_approved_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_on');
SET @new_manager_approved_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_by');
SET @new_rejected_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_on');
SET @new_rejected_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'reject_reason');
SET @new_reject_reason := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- first run only: rows approved before keep APPROVED, the rest wait for the manager
UPDATE client_master SET approval_status = IF(is_approved = 1, 'APPROVED', 'PENDING_MANAGER') WHERE @new_approval_status;
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND INDEX_NAME = 'idx_client_master_approval');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD KEY idx_client_master_approval (approval_status)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE permission_master SET default_allowed = 1, description = 'Approve / reject clients and decide delete requests (who may approve which step is set by role and team)'
WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== initech_db ====================
USE initech_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approval_status');
SET @new_approval_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''PENDING_MANAGER'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_approved', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_by');
SET @new_manager_approved_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_on');
SET @new_manager_approved_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_by');
SET @new_rejected_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_on');
SET @new_rejected_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'reject_reason');
SET @new_reject_reason := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- first run only: rows approved before keep APPROVED, the rest wait for the manager
UPDATE client_master SET approval_status = IF(is_approved = 1, 'APPROVED', 'PENDING_MANAGER') WHERE @new_approval_status;
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND INDEX_NAME = 'idx_client_master_approval');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD KEY idx_client_master_approval (approval_status)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE permission_master SET default_allowed = 1, description = 'Approve / reject clients and decide delete requests (who may approve which step is set by role and team)'
WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approval_status');
SET @new_approval_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''PENDING_MANAGER'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_approved', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_by');
SET @new_manager_approved_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'manager_approved_on');
SET @new_manager_approved_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_by');
SET @new_rejected_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'rejected_on');
SET @new_rejected_on := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'reject_reason');
SET @new_reject_reason := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- first run only: rows approved before keep APPROVED, the rest wait for the manager
UPDATE client_master SET approval_status = IF(is_approved = 1, 'APPROVED', 'PENDING_MANAGER') WHERE @new_approval_status;
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND INDEX_NAME = 'idx_client_master_approval');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD KEY idx_client_master_approval (approval_status)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE permission_master SET default_allowed = 1, description = 'Approve / reject clients and decide delete requests (who may approve which step is set by role and team)'
WHERE permission_code = 'CLIENT_MASTER_APPROVE';
