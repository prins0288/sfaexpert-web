-- ===========================================================================
-- 82_emp_assignment_history.sql — PER TENANT database
--
-- Promotion / transfer history: emp_assignment_history already existed but was
-- never written to. From now on, saving an employee whose designation, manager,
-- division, HQ or state actually changes writes a new row here (effective_from
-- = the chosen date, default today) and closes the previous open row
-- (effective_to = the day before). Applied directly by admin/manager — no
-- approval. change_reason is auto-detected from what changed (PROMOTION /
-- DEMOTION / MANAGER_CHANGE / TRANSFER) unless the form sends one explicitly
-- (e.g. CORRECTION); a new employee gets an initial JOINING row.
--
-- This script only relaxes two NOT NULL columns left over from the old schema
-- (division_id, designation_id can be null — not every employee has one) and
-- adds an optional free-text remarks column. No data is touched.
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/82_emp_assignment_history.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
DROP PROCEDURE IF EXISTS sfa_mig_82;
DELIMITER //
CREATE PROCEDURE sfa_mig_82()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'division_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY division_id BIGINT NULL;
    END IF;
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'designation_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'remarks') THEN
      ALTER TABLE emp_assignment_history ADD COLUMN remarks VARCHAR(255) NULL AFTER change_reason;
    END IF;
  ELSE
    CREATE TABLE emp_assignment_history (
      id              BIGINT       NOT NULL AUTO_INCREMENT,
      emp_id          VARCHAR(40)  NOT NULL,
      division_id     BIGINT       NULL,
      state_id        BIGINT       NULL,
      hq_id           BIGINT       NULL,
      designation_id  INT          NULL,
      manager_id      VARCHAR(40)  NULL,
      effective_from  DATE         NOT NULL,
      effective_to    DATE         NULL COMMENT 'NULL = current',
      change_reason   VARCHAR(30)  NULL COMMENT 'JOINING / TRANSFER / PROMOTION / DEMOTION / MANAGER_CHANGE / CORRECTION',
      remarks         VARCHAR(255) NULL,
      created_by      VARCHAR(30)  NULL,
      created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
      PRIMARY KEY (id),
      KEY idx_hist_emp_from (emp_id, effective_from),
      KEY idx_hist_mgr (manager_id, effective_from),
      CONSTRAINT fk_hist_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_82();
DROP PROCEDURE sfa_mig_82;
SELECT DATABASE() AS db, COUNT(*) AS history_rows FROM emp_assignment_history;

-- ==================== globex_db ====================
USE globex_db;
DROP PROCEDURE IF EXISTS sfa_mig_82;
DELIMITER //
CREATE PROCEDURE sfa_mig_82()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'division_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY division_id BIGINT NULL;
    END IF;
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'designation_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'remarks') THEN
      ALTER TABLE emp_assignment_history ADD COLUMN remarks VARCHAR(255) NULL AFTER change_reason;
    END IF;
  ELSE
    CREATE TABLE emp_assignment_history (
      id              BIGINT       NOT NULL AUTO_INCREMENT,
      emp_id          VARCHAR(40)  NOT NULL,
      division_id     BIGINT       NULL,
      state_id        BIGINT       NULL,
      hq_id           BIGINT       NULL,
      designation_id  INT          NULL,
      manager_id      VARCHAR(40)  NULL,
      effective_from  DATE         NOT NULL,
      effective_to    DATE         NULL COMMENT 'NULL = current',
      change_reason   VARCHAR(30)  NULL COMMENT 'JOINING / TRANSFER / PROMOTION / DEMOTION / MANAGER_CHANGE / CORRECTION',
      remarks         VARCHAR(255) NULL,
      created_by      VARCHAR(30)  NULL,
      created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
      PRIMARY KEY (id),
      KEY idx_hist_emp_from (emp_id, effective_from),
      KEY idx_hist_mgr (manager_id, effective_from),
      CONSTRAINT fk_hist_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_82();
DROP PROCEDURE sfa_mig_82;
SELECT DATABASE() AS db, COUNT(*) AS history_rows FROM emp_assignment_history;

-- ==================== initech_db ====================
USE initech_db;
DROP PROCEDURE IF EXISTS sfa_mig_82;
DELIMITER //
CREATE PROCEDURE sfa_mig_82()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'division_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY division_id BIGINT NULL;
    END IF;
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'designation_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'remarks') THEN
      ALTER TABLE emp_assignment_history ADD COLUMN remarks VARCHAR(255) NULL AFTER change_reason;
    END IF;
  ELSE
    CREATE TABLE emp_assignment_history (
      id              BIGINT       NOT NULL AUTO_INCREMENT,
      emp_id          VARCHAR(40)  NOT NULL,
      division_id     BIGINT       NULL,
      state_id        BIGINT       NULL,
      hq_id           BIGINT       NULL,
      designation_id  INT          NULL,
      manager_id      VARCHAR(40)  NULL,
      effective_from  DATE         NOT NULL,
      effective_to    DATE         NULL COMMENT 'NULL = current',
      change_reason   VARCHAR(30)  NULL COMMENT 'JOINING / TRANSFER / PROMOTION / DEMOTION / MANAGER_CHANGE / CORRECTION',
      remarks         VARCHAR(255) NULL,
      created_by      VARCHAR(30)  NULL,
      created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
      PRIMARY KEY (id),
      KEY idx_hist_emp_from (emp_id, effective_from),
      KEY idx_hist_mgr (manager_id, effective_from),
      CONSTRAINT fk_hist_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_82();
DROP PROCEDURE sfa_mig_82;
SELECT DATABASE() AS db, COUNT(*) AS history_rows FROM emp_assignment_history;

-- ==================== sfa_demo ====================
USE sfa_demo;
DROP PROCEDURE IF EXISTS sfa_mig_82;
DELIMITER //
CREATE PROCEDURE sfa_mig_82()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'division_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY division_id BIGINT NULL;
    END IF;
    SET @t := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'designation_id');
    IF @t = 'NO' THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'emp_assignment_history' AND COLUMN_NAME = 'remarks') THEN
      ALTER TABLE emp_assignment_history ADD COLUMN remarks VARCHAR(255) NULL AFTER change_reason;
    END IF;
  ELSE
    CREATE TABLE emp_assignment_history (
      id              BIGINT       NOT NULL AUTO_INCREMENT,
      emp_id          VARCHAR(40)  NOT NULL,
      division_id     BIGINT       NULL,
      state_id        BIGINT       NULL,
      hq_id           BIGINT       NULL,
      designation_id  INT          NULL,
      manager_id      VARCHAR(40)  NULL,
      effective_from  DATE         NOT NULL,
      effective_to    DATE         NULL COMMENT 'NULL = current',
      change_reason   VARCHAR(30)  NULL COMMENT 'JOINING / TRANSFER / PROMOTION / DEMOTION / MANAGER_CHANGE / CORRECTION',
      remarks         VARCHAR(255) NULL,
      created_by      VARCHAR(30)  NULL,
      created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
      PRIMARY KEY (id),
      KEY idx_hist_emp_from (emp_id, effective_from),
      KEY idx_hist_mgr (manager_id, effective_from),
      CONSTRAINT fk_hist_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
      CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_82();
DROP PROCEDURE sfa_mig_82;
SELECT DATABASE() AS db, COUNT(*) AS history_rows FROM emp_assignment_history;
