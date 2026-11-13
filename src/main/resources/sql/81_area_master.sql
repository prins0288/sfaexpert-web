-- ===========================================================================
-- 81_area_master.sql — PER TENANT database
--
-- Area master:
--   * city_master is renamed area_master (data, ids and the foreign keys from
--     client_master / route_area_map stay as they are)
--   * area_master.emp_id -> emp_detail.emp_id: whose area it is (an admin picks
--     the employee; a manager / user always adds for himself)
--   * who sees / edits / deletes: admin all; manager his own + his team's (everyone
--     reporting to him, any depth); user only his own. Areas without an employee
--     (all old ones) are seen by admins only until one is set.
--   * ADD and DELETE go through approval exactly like Client Master
--     (Settings > General > Area approval: Auto / Manager / Admin / Manager -> Admin):
--       - a new area stays INACTIVE (status N) until its final approval
--       - a delete request keeps the area active until its final approval (then N)
--       - an admin's request is approved directly; a manager's does the manager step
--       - nobody approves his own request, or an area that is his
--       - every request / approval / rejection is logged in area_master_approval
--         with the signed-in user's details
--     Existing areas are APPROVED.
--   * permissions AREA_DELETE (raise a delete request), AREA_ADD_APPROVE,
--     AREA_DELETE_APPROVE (allowed by default: role + team decide)
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/81_area_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
DROP PROCEDURE IF EXISTS sfa_mig_81;
DELIMITER //
CREATE PROCEDURE sfa_mig_81()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'city_master')
     AND NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'area_master') THEN
    RENAME TABLE city_master TO area_master;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'emp_id') THEN
    -- same charset / collation as emp_detail.emp_id, else the foreign key cannot be made
    SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
    SET @sql := CONCAT('ALTER TABLE area_master ADD COLUMN emp_id VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co,
                       ' NULL COMMENT ''-> emp_detail.emp_id (whose area)'' AFTER hq_oid');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    ALTER TABLE area_master ADD KEY ix_area_emp (emp_id);
    ALTER TABLE area_master ADD CONSTRAINT fk_area_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approval_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'add_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN add_requested_by VARCHAR(40) NULL AFTER approval_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER add_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_by VARCHAR(40) NULL AFTER manager_approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'reject_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'is_delete_requested') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN is_delete_requested TINYINT(1) NOT NULL DEFAULT 0 AFTER reject_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER delete_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_request_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'created_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN created_by VARCHAR(40) NULL AFTER delete_request_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'updated_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN updated_by VARCHAR(40) NULL AFTER created_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND INDEX_NAME = 'ix_area_approval') THEN
    ALTER TABLE area_master ADD KEY ix_area_approval (approval_status);
  END IF;
END //
DELIMITER ;
CALL sfa_mig_81();
DROP PROCEDURE sfa_mig_81;

CREATE TABLE IF NOT EXISTS area_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  area_id           BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / AUTO / MANAGER / ADMIN / FINAL',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL,
  remarks           VARCHAR(255) NULL,
  emp_id            VARCHAR(40)  NULL COMMENT 'who did it (signed-in user)',
  emp_code          VARCHAR(30)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  hierarchy_level   INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_area_approval_area (area_id),
  CONSTRAINT fk_area_approval_area FOREIGN KEY (area_id) REFERENCES area_master (oid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('AREA_DELETE', 'Area', 'Ask for an Area to be deleted (own / team / all by role)', 1, NOW(), 'seed'),
    ('AREA_ADD_APPROVE', 'Area', 'Approve / reject NEW areas (step by role and team)', 1, NOW(), 'seed'),
    ('AREA_DELETE_APPROVE', 'Area', 'Approve / reject area DELETE requests (step by role and team)', 1, NOW(), 'seed');
DELETE FROM permission_master WHERE permission_code = 'AREA_APPROVE';
SELECT DATABASE() AS db, COUNT(*) AS areas, SUM(emp_id IS NULL) AS without_employee, SUM(approval_status <> 'APPROVED') AS not_approved FROM area_master;

-- ==================== globex_db ====================
USE globex_db;
DROP PROCEDURE IF EXISTS sfa_mig_81;
DELIMITER //
CREATE PROCEDURE sfa_mig_81()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'city_master')
     AND NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'area_master') THEN
    RENAME TABLE city_master TO area_master;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'emp_id') THEN
    -- same charset / collation as emp_detail.emp_id, else the foreign key cannot be made
    SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
    SET @sql := CONCAT('ALTER TABLE area_master ADD COLUMN emp_id VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co,
                       ' NULL COMMENT ''-> emp_detail.emp_id (whose area)'' AFTER hq_oid');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    ALTER TABLE area_master ADD KEY ix_area_emp (emp_id);
    ALTER TABLE area_master ADD CONSTRAINT fk_area_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approval_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'add_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN add_requested_by VARCHAR(40) NULL AFTER approval_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER add_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_by VARCHAR(40) NULL AFTER manager_approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'reject_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'is_delete_requested') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN is_delete_requested TINYINT(1) NOT NULL DEFAULT 0 AFTER reject_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER delete_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_request_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'created_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN created_by VARCHAR(40) NULL AFTER delete_request_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'updated_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN updated_by VARCHAR(40) NULL AFTER created_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND INDEX_NAME = 'ix_area_approval') THEN
    ALTER TABLE area_master ADD KEY ix_area_approval (approval_status);
  END IF;
END //
DELIMITER ;
CALL sfa_mig_81();
DROP PROCEDURE sfa_mig_81;

CREATE TABLE IF NOT EXISTS area_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  area_id           BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / AUTO / MANAGER / ADMIN / FINAL',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL,
  remarks           VARCHAR(255) NULL,
  emp_id            VARCHAR(40)  NULL COMMENT 'who did it (signed-in user)',
  emp_code          VARCHAR(30)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  hierarchy_level   INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_area_approval_area (area_id),
  CONSTRAINT fk_area_approval_area FOREIGN KEY (area_id) REFERENCES area_master (oid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('AREA_DELETE', 'Area', 'Ask for an Area to be deleted (own / team / all by role)', 1, NOW(), 'seed'),
    ('AREA_ADD_APPROVE', 'Area', 'Approve / reject NEW areas (step by role and team)', 1, NOW(), 'seed'),
    ('AREA_DELETE_APPROVE', 'Area', 'Approve / reject area DELETE requests (step by role and team)', 1, NOW(), 'seed');
DELETE FROM permission_master WHERE permission_code = 'AREA_APPROVE';
SELECT DATABASE() AS db, COUNT(*) AS areas, SUM(emp_id IS NULL) AS without_employee, SUM(approval_status <> 'APPROVED') AS not_approved FROM area_master;

-- ==================== initech_db ====================
USE initech_db;
DROP PROCEDURE IF EXISTS sfa_mig_81;
DELIMITER //
CREATE PROCEDURE sfa_mig_81()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'city_master')
     AND NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'area_master') THEN
    RENAME TABLE city_master TO area_master;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'emp_id') THEN
    -- same charset / collation as emp_detail.emp_id, else the foreign key cannot be made
    SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
    SET @sql := CONCAT('ALTER TABLE area_master ADD COLUMN emp_id VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co,
                       ' NULL COMMENT ''-> emp_detail.emp_id (whose area)'' AFTER hq_oid');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    ALTER TABLE area_master ADD KEY ix_area_emp (emp_id);
    ALTER TABLE area_master ADD CONSTRAINT fk_area_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approval_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'add_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN add_requested_by VARCHAR(40) NULL AFTER approval_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER add_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_by VARCHAR(40) NULL AFTER manager_approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'reject_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'is_delete_requested') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN is_delete_requested TINYINT(1) NOT NULL DEFAULT 0 AFTER reject_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER delete_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_request_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'created_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN created_by VARCHAR(40) NULL AFTER delete_request_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'updated_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN updated_by VARCHAR(40) NULL AFTER created_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND INDEX_NAME = 'ix_area_approval') THEN
    ALTER TABLE area_master ADD KEY ix_area_approval (approval_status);
  END IF;
END //
DELIMITER ;
CALL sfa_mig_81();
DROP PROCEDURE sfa_mig_81;

CREATE TABLE IF NOT EXISTS area_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  area_id           BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / AUTO / MANAGER / ADMIN / FINAL',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL,
  remarks           VARCHAR(255) NULL,
  emp_id            VARCHAR(40)  NULL COMMENT 'who did it (signed-in user)',
  emp_code          VARCHAR(30)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  hierarchy_level   INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_area_approval_area (area_id),
  CONSTRAINT fk_area_approval_area FOREIGN KEY (area_id) REFERENCES area_master (oid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('AREA_DELETE', 'Area', 'Ask for an Area to be deleted (own / team / all by role)', 1, NOW(), 'seed'),
    ('AREA_ADD_APPROVE', 'Area', 'Approve / reject NEW areas (step by role and team)', 1, NOW(), 'seed'),
    ('AREA_DELETE_APPROVE', 'Area', 'Approve / reject area DELETE requests (step by role and team)', 1, NOW(), 'seed');
DELETE FROM permission_master WHERE permission_code = 'AREA_APPROVE';
SELECT DATABASE() AS db, COUNT(*) AS areas, SUM(emp_id IS NULL) AS without_employee, SUM(approval_status <> 'APPROVED') AS not_approved FROM area_master;

-- ==================== sfa_demo ====================
USE sfa_demo;
DROP PROCEDURE IF EXISTS sfa_mig_81;
DELIMITER //
CREATE PROCEDURE sfa_mig_81()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'city_master')
     AND NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'area_master') THEN
    RENAME TABLE city_master TO area_master;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'emp_id') THEN
    -- same charset / collation as emp_detail.emp_id, else the foreign key cannot be made
    SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
    SET @sql := CONCAT('ALTER TABLE area_master ADD COLUMN emp_id VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co,
                       ' NULL COMMENT ''-> emp_detail.emp_id (whose area)'' AFTER hq_oid');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    ALTER TABLE area_master ADD KEY ix_area_emp (emp_id);
    ALTER TABLE area_master ADD CONSTRAINT fk_area_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approval_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'add_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN add_requested_by VARCHAR(40) NULL AFTER approval_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_by VARCHAR(40) NULL AFTER add_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'manager_approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN manager_approved_on DATETIME NULL AFTER manager_approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_by VARCHAR(40) NULL AFTER manager_approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'approved_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_by VARCHAR(40) NULL AFTER approved_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'rejected_on') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN rejected_on DATETIME NULL AFTER rejected_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'reject_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN reject_reason VARCHAR(255) NULL AFTER rejected_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'is_delete_requested') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN is_delete_requested TINYINT(1) NOT NULL DEFAULT 0 AFTER reject_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_status') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_status VARCHAR(20) NULL COMMENT ''PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED'' AFTER is_delete_requested';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_requested_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER delete_status';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'delete_request_reason') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'created_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN created_by VARCHAR(40) NULL AFTER delete_request_reason';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND COLUMN_NAME = 'updated_by') THEN
    SET @sql := 'ALTER TABLE area_master ADD COLUMN updated_by VARCHAR(40) NULL AFTER created_on';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'area_master' AND INDEX_NAME = 'ix_area_approval') THEN
    ALTER TABLE area_master ADD KEY ix_area_approval (approval_status);
  END IF;
END //
DELIMITER ;
CALL sfa_mig_81();
DROP PROCEDURE sfa_mig_81;

CREATE TABLE IF NOT EXISTS area_master_approval (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  area_id           BIGINT       NOT NULL,
  request_type      VARCHAR(10)  NOT NULL COMMENT 'ADD / DELETE',
  step              VARCHAR(20)  NOT NULL COMMENT 'REQUEST / AUTO / MANAGER / ADMIN / FINAL',
  action            VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after      VARCHAR(20)  NULL,
  remarks           VARCHAR(255) NULL,
  emp_id            VARCHAR(40)  NULL COMMENT 'who did it (signed-in user)',
  emp_code          VARCHAR(30)  NULL,
  emp_name          VARCHAR(150) NULL,
  designation_name  VARCHAR(120) NULL,
  hierarchy_level   INT          NULL,
  role              VARCHAR(20)  NULL,
  action_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_area_approval_area (area_id),
  CONSTRAINT fk_area_approval_area FOREIGN KEY (area_id) REFERENCES area_master (oid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('AREA_DELETE', 'Area', 'Ask for an Area to be deleted (own / team / all by role)', 1, NOW(), 'seed'),
    ('AREA_ADD_APPROVE', 'Area', 'Approve / reject NEW areas (step by role and team)', 1, NOW(), 'seed'),
    ('AREA_DELETE_APPROVE', 'Area', 'Approve / reject area DELETE requests (step by role and team)', 1, NOW(), 'seed');
DELETE FROM permission_master WHERE permission_code = 'AREA_APPROVE';
SELECT DATABASE() AS db, COUNT(*) AS areas, SUM(emp_id IS NULL) AS without_employee, SUM(approval_status <> 'APPROVED') AS not_approved FROM area_master;
