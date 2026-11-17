-- ===========================================================================
-- 79_client_add_delete_approval.sql — PER TENANT database
--
-- Client Master: adding AND deleting a client both go through the company's
-- approval flow (Settings > General > Client approval).
--   * a new client stays INACTIVE (is_active = 0) until its final approval
--   * a delete request keeps the client active until its final approval,
--     which sets is_active = 0
--   * client_master.delete_status    PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED
--   * client_master.add_requested_by who asked for the client (create / resubmit)
--   * client_master_approval         every request / approval / rejection with the
--                                    signed-in user's details (emp id, code, name,
--                                    designation, level, role) at that moment
--   * permissions CLIENT_ADD_APPROVE and CLIENT_DELETE_APPROVE (allowed by default:
--     role + team decide; deny one to stop someone approving that kind)
-- Nobody approves their own request (or a request about a client mapped to them).
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/79_client_add_delete_approval.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_status');
SET @new_delete_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'add_requested_by');
SET @new_add_requested_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN add_requested_by VARCHAR(40) NULL COMMENT ''who asked for the client (create / resubmit)'' AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- first run: requests already open keep waiting; a client still waiting for approval is not active yet
UPDATE client_master SET delete_status = 'PENDING_ADMIN' WHERE @new_delete_status AND is_delete_requested = 1;
UPDATE client_master SET add_requested_by = created_by WHERE @new_add_requested_by;
UPDATE client_master SET is_active = 0 WHERE @new_add_requested_by AND approval_status IN ('PENDING_MANAGER', 'PENDING_ADMIN', 'REJECTED');

CREATE TABLE IF NOT EXISTS client_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  client_id         BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / ADMIN / FINAL / AUTO',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL COMMENT 'request status after this action',
  remarks           VARCHAR(255) NULL COMMENT 'reason for a delete request or a rejection',
  emp_id            VARCHAR(40)  NULL COMMENT 'signed-in user at that moment',
  emp_code          VARCHAR(40)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  emp_level         INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cma_client (client_id, action_at),
  KEY idx_cma_emp (emp_id),
  CONSTRAINT fk_cma_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_ADD_APPROVE', 'Client Master', 'Approve / reject NEW clients (step by role and team)', 1, 1, NOW(), 'seed'),
    ('CLIENT_DELETE_APPROVE', 'Client Master', 'Approve / reject client DELETE requests (step by role and team)', 1, 1, NOW(), 'seed');
UPDATE permission_master SET description = 'Old code, replaced by CLIENT_ADD_APPROVE / CLIENT_DELETE_APPROVE' WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== globex_db ====================
USE globex_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_status');
SET @new_delete_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'add_requested_by');
SET @new_add_requested_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN add_requested_by VARCHAR(40) NULL COMMENT ''who asked for the client (create / resubmit)'' AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- first run: requests already open keep waiting; a client still waiting for approval is not active yet
UPDATE client_master SET delete_status = 'PENDING_ADMIN' WHERE @new_delete_status AND is_delete_requested = 1;
UPDATE client_master SET add_requested_by = created_by WHERE @new_add_requested_by;
UPDATE client_master SET is_active = 0 WHERE @new_add_requested_by AND approval_status IN ('PENDING_MANAGER', 'PENDING_ADMIN', 'REJECTED');

CREATE TABLE IF NOT EXISTS client_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  client_id         BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / ADMIN / FINAL / AUTO',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL COMMENT 'request status after this action',
  remarks           VARCHAR(255) NULL COMMENT 'reason for a delete request or a rejection',
  emp_id            VARCHAR(40)  NULL COMMENT 'signed-in user at that moment',
  emp_code          VARCHAR(40)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  emp_level         INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cma_client (client_id, action_at),
  KEY idx_cma_emp (emp_id),
  CONSTRAINT fk_cma_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_ADD_APPROVE', 'Client Master', 'Approve / reject NEW clients (step by role and team)', 1, 1, NOW(), 'seed'),
    ('CLIENT_DELETE_APPROVE', 'Client Master', 'Approve / reject client DELETE requests (step by role and team)', 1, 1, NOW(), 'seed');
UPDATE permission_master SET description = 'Old code, replaced by CLIENT_ADD_APPROVE / CLIENT_DELETE_APPROVE' WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== initech_db ====================
USE initech_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_status');
SET @new_delete_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'add_requested_by');
SET @new_add_requested_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN add_requested_by VARCHAR(40) NULL COMMENT ''who asked for the client (create / resubmit)'' AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- first run: requests already open keep waiting; a client still waiting for approval is not active yet
UPDATE client_master SET delete_status = 'PENDING_ADMIN' WHERE @new_delete_status AND is_delete_requested = 1;
UPDATE client_master SET add_requested_by = created_by WHERE @new_add_requested_by;
UPDATE client_master SET is_active = 0 WHERE @new_add_requested_by AND approval_status IN ('PENDING_MANAGER', 'PENDING_ADMIN', 'REJECTED');

CREATE TABLE IF NOT EXISTS client_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  client_id         BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / ADMIN / FINAL / AUTO',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL COMMENT 'request status after this action',
  remarks           VARCHAR(255) NULL COMMENT 'reason for a delete request or a rejection',
  emp_id            VARCHAR(40)  NULL COMMENT 'signed-in user at that moment',
  emp_code          VARCHAR(40)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  emp_level         INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cma_client (client_id, action_at),
  KEY idx_cma_emp (emp_id),
  CONSTRAINT fk_cma_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_ADD_APPROVE', 'Client Master', 'Approve / reject NEW clients (step by role and team)', 1, 1, NOW(), 'seed'),
    ('CLIENT_DELETE_APPROVE', 'Client Master', 'Approve / reject client DELETE requests (step by role and team)', 1, 1, NOW(), 'seed');
UPDATE permission_master SET description = 'Old code, replaced by CLIENT_ADD_APPROVE / CLIENT_DELETE_APPROVE' WHERE permission_code = 'CLIENT_MASTER_APPROVE';

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_status');
SET @new_delete_status := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'add_requested_by');
SET @new_add_requested_by := (@has = 0);
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN add_requested_by VARCHAR(40) NULL COMMENT ''who asked for the client (create / resubmit)'' AFTER approval_status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- first run: requests already open keep waiting; a client still waiting for approval is not active yet
UPDATE client_master SET delete_status = 'PENDING_ADMIN' WHERE @new_delete_status AND is_delete_requested = 1;
UPDATE client_master SET add_requested_by = created_by WHERE @new_add_requested_by;
UPDATE client_master SET is_active = 0 WHERE @new_add_requested_by AND approval_status IN ('PENDING_MANAGER', 'PENDING_ADMIN', 'REJECTED');

CREATE TABLE IF NOT EXISTS client_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  client_id         BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / ADMIN / FINAL / AUTO',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL COMMENT 'request status after this action',
  remarks           VARCHAR(255) NULL COMMENT 'reason for a delete request or a rejection',
  emp_id            VARCHAR(40)  NULL COMMENT 'signed-in user at that moment',
  emp_code          VARCHAR(40)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  emp_level         INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cma_client (client_id, action_at),
  KEY idx_cma_emp (emp_id),
  CONSTRAINT fk_cma_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_ADD_APPROVE', 'Client Master', 'Approve / reject NEW clients (step by role and team)', 1, 1, NOW(), 'seed'),
    ('CLIENT_DELETE_APPROVE', 'Client Master', 'Approve / reject client DELETE requests (step by role and team)', 1, 1, NOW(), 'seed');
UPDATE permission_master SET description = 'Old code, replaced by CLIENT_ADD_APPROVE / CLIENT_DELETE_APPROVE' WHERE permission_code = 'CLIENT_MASTER_APPROVE';
