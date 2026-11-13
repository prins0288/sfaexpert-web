-- ===========================================================================
-- 80_designation_master_v2.sql — PER TENANT database
--
-- The OLD designation table (oid, designation_code, designation_name, status,
-- emp_level) is DROPPED and nothing of it is kept. The new designation_master
-- is created EMPTY, exactly as designed:
--   id, designation, hierarchy_level (unique), display_order, grade,
--   designation_category, is_active, created_at
-- The employee keeps ONLY the designation (emp_detail.designation -> designation_master.id):
--   * emp_detail.emp_level is dropped
--   * emp_detail.designation becomes INT NULL and is CLEARED for every employee —
--     give each employee their designation again (Create Employee > Edit)
--   * emp_assignment_history.designation_id becomes INT NULL (old ids cleared)
--   * permission rules by designation code / emp level are deleted
--     (set them again by designation / hierarchy level in Permission Master)
--   * client_master_approval.emp_level is renamed hierarchy_level
-- Role = the designation's hierarchy_level by the company's ranges
-- (Settings > General > Role by hierarchy level; default Manager 15+, Admin 50+,
-- Super Admin 90+). An employee without a designation logs in as USER.
--
-- !! After this script NOBODY is admin until a designation is given back. !!
-- Run STEP 2 at the bottom (once per company, with your admin's emp_id) to create
-- the first designation for your admin, then log in and set up the rest.
--
-- Safe to re-run (does nothing once the new table is in place).
-- Run:  mysql -u myroot < src/main/resources/sql/80_designation_master_v2.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
DROP PROCEDURE IF EXISTS sfa_mig_80;
DELIMITER //
CREATE PROCEDURE sfa_mig_80()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'oid') THEN
    -- foreign keys that point at the old table
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_detail' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_detail DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_assignment_history' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_assignment_history DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;

    DROP TABLE designation_master;

    CREATE TABLE designation_master (
        id         INT PRIMARY KEY AUTO_INCREMENT,
        designation       VARCHAR(50) NOT NULL,        -- 'MR', 'ABM', 'Dy. RSM' etc
        hierarchy_level INT NOT NULL UNIQUE,          -- 10, 15, 20, 32 etc
        display_order   INT,                          -- UI sorting (1,2,3...)
        grade           VARCHAR(20) DEFAULT NULL,     -- Junior/Senior (agar grade hai)
        designation_category   VARCHAR(30),                  -- Field/Manager/Admin/Support
        is_active       BOOLEAN DEFAULT TRUE,
        created_at      TIMESTAMP DEFAULT NOW()
    );

    -- the employee keeps only the designation (cleared: the old ids mean nothing now)
    ALTER TABLE emp_detail MODIFY designation INT NULL;
    UPDATE emp_detail SET designation = NULL;
    ALTER TABLE emp_detail ADD CONSTRAINT fk_emp_designation FOREIGN KEY (designation) REFERENCES designation_master (id);
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND INDEX_NAME = 'idx_emp_active_level') THEN
      ALTER TABLE emp_detail DROP INDEX idx_emp_active_level;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE emp_detail DROP COLUMN emp_level;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
      UPDATE emp_assignment_history SET designation_id = NULL;
      ALTER TABLE emp_assignment_history ADD CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id);
    END IF;

    -- permission rules by the old designation code / emp level
    DELETE FROM permission_assignment WHERE target_type IN ('DESIGNATION', 'EMP_LEVEL');

    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'client_master_approval' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE client_master_approval CHANGE emp_level hierarchy_level INT NULL;
    END IF;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_80();
DROP PROCEDURE sfa_mig_80;
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM designation_master) AS designations,
       (SELECT COUNT(*) FROM emp_detail WHERE designation IS NULL) AS employees_without_designation;

-- ==================== globex_db ====================
USE globex_db;
DROP PROCEDURE IF EXISTS sfa_mig_80;
DELIMITER //
CREATE PROCEDURE sfa_mig_80()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'oid') THEN
    -- foreign keys that point at the old table
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_detail' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_detail DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_assignment_history' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_assignment_history DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;

    DROP TABLE designation_master;

    CREATE TABLE designation_master (
        id         INT PRIMARY KEY AUTO_INCREMENT,
        designation       VARCHAR(50) NOT NULL,        -- 'MR', 'ABM', 'Dy. RSM' etc
        hierarchy_level INT NOT NULL UNIQUE,          -- 10, 15, 20, 32 etc
        display_order   INT,                          -- UI sorting (1,2,3...)
        grade           VARCHAR(20) DEFAULT NULL,     -- Junior/Senior (agar grade hai)
        designation_category   VARCHAR(30),                  -- Field/Manager/Admin/Support
        is_active       BOOLEAN DEFAULT TRUE,
        created_at      TIMESTAMP DEFAULT NOW()
    );

    -- the employee keeps only the designation (cleared: the old ids mean nothing now)
    ALTER TABLE emp_detail MODIFY designation INT NULL;
    UPDATE emp_detail SET designation = NULL;
    ALTER TABLE emp_detail ADD CONSTRAINT fk_emp_designation FOREIGN KEY (designation) REFERENCES designation_master (id);
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND INDEX_NAME = 'idx_emp_active_level') THEN
      ALTER TABLE emp_detail DROP INDEX idx_emp_active_level;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE emp_detail DROP COLUMN emp_level;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
      UPDATE emp_assignment_history SET designation_id = NULL;
      ALTER TABLE emp_assignment_history ADD CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id);
    END IF;

    -- permission rules by the old designation code / emp level
    DELETE FROM permission_assignment WHERE target_type IN ('DESIGNATION', 'EMP_LEVEL');

    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'client_master_approval' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE client_master_approval CHANGE emp_level hierarchy_level INT NULL;
    END IF;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_80();
DROP PROCEDURE sfa_mig_80;
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM designation_master) AS designations,
       (SELECT COUNT(*) FROM emp_detail WHERE designation IS NULL) AS employees_without_designation;

-- ==================== initech_db ====================
USE initech_db;
DROP PROCEDURE IF EXISTS sfa_mig_80;
DELIMITER //
CREATE PROCEDURE sfa_mig_80()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'oid') THEN
    -- foreign keys that point at the old table
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_detail' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_detail DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_assignment_history' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_assignment_history DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;

    DROP TABLE designation_master;

    CREATE TABLE designation_master (
        id         INT PRIMARY KEY AUTO_INCREMENT,
        designation       VARCHAR(50) NOT NULL,        -- 'MR', 'ABM', 'Dy. RSM' etc
        hierarchy_level INT NOT NULL UNIQUE,          -- 10, 15, 20, 32 etc
        display_order   INT,                          -- UI sorting (1,2,3...)
        grade           VARCHAR(20) DEFAULT NULL,     -- Junior/Senior (agar grade hai)
        designation_category   VARCHAR(30),                  -- Field/Manager/Admin/Support
        is_active       BOOLEAN DEFAULT TRUE,
        created_at      TIMESTAMP DEFAULT NOW()
    );

    -- the employee keeps only the designation (cleared: the old ids mean nothing now)
    ALTER TABLE emp_detail MODIFY designation INT NULL;
    UPDATE emp_detail SET designation = NULL;
    ALTER TABLE emp_detail ADD CONSTRAINT fk_emp_designation FOREIGN KEY (designation) REFERENCES designation_master (id);
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND INDEX_NAME = 'idx_emp_active_level') THEN
      ALTER TABLE emp_detail DROP INDEX idx_emp_active_level;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE emp_detail DROP COLUMN emp_level;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
      UPDATE emp_assignment_history SET designation_id = NULL;
      ALTER TABLE emp_assignment_history ADD CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id);
    END IF;

    -- permission rules by the old designation code / emp level
    DELETE FROM permission_assignment WHERE target_type IN ('DESIGNATION', 'EMP_LEVEL');

    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'client_master_approval' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE client_master_approval CHANGE emp_level hierarchy_level INT NULL;
    END IF;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_80();
DROP PROCEDURE sfa_mig_80;
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM designation_master) AS designations,
       (SELECT COUNT(*) FROM emp_detail WHERE designation IS NULL) AS employees_without_designation;

-- ==================== sfa_demo ====================
USE sfa_demo;
DROP PROCEDURE IF EXISTS sfa_mig_80;
DELIMITER //
CREATE PROCEDURE sfa_mig_80()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'oid') THEN
    -- foreign keys that point at the old table
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_detail' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_detail DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
    SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
                AND TABLE_NAME = 'emp_assignment_history' AND REFERENCED_TABLE_NAME = 'designation_master' LIMIT 1);
    IF @fk IS NOT NULL THEN
      SET @sql := CONCAT('ALTER TABLE emp_assignment_history DROP FOREIGN KEY `', @fk, '`'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;

    DROP TABLE designation_master;

    CREATE TABLE designation_master (
        id         INT PRIMARY KEY AUTO_INCREMENT,
        designation       VARCHAR(50) NOT NULL,        -- 'MR', 'ABM', 'Dy. RSM' etc
        hierarchy_level INT NOT NULL UNIQUE,          -- 10, 15, 20, 32 etc
        display_order   INT,                          -- UI sorting (1,2,3...)
        grade           VARCHAR(20) DEFAULT NULL,     -- Junior/Senior (agar grade hai)
        designation_category   VARCHAR(30),                  -- Field/Manager/Admin/Support
        is_active       BOOLEAN DEFAULT TRUE,
        created_at      TIMESTAMP DEFAULT NOW()
    );

    -- the employee keeps only the designation (cleared: the old ids mean nothing now)
    ALTER TABLE emp_detail MODIFY designation INT NULL;
    UPDATE emp_detail SET designation = NULL;
    ALTER TABLE emp_detail ADD CONSTRAINT fk_emp_designation FOREIGN KEY (designation) REFERENCES designation_master (id);
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND INDEX_NAME = 'idx_emp_active_level') THEN
      ALTER TABLE emp_detail DROP INDEX idx_emp_active_level;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE emp_detail DROP COLUMN emp_level;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_assignment_history') THEN
      ALTER TABLE emp_assignment_history MODIFY designation_id INT NULL;
      UPDATE emp_assignment_history SET designation_id = NULL;
      ALTER TABLE emp_assignment_history ADD CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (id);
    END IF;

    -- permission rules by the old designation code / emp level
    DELETE FROM permission_assignment WHERE target_type IN ('DESIGNATION', 'EMP_LEVEL');

    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'client_master_approval' AND COLUMN_NAME = 'emp_level') THEN
      ALTER TABLE client_master_approval CHANGE emp_level hierarchy_level INT NULL;
    END IF;
  END IF;
END //
DELIMITER ;
CALL sfa_mig_80();
DROP PROCEDURE sfa_mig_80;
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM designation_master) AS designations,
       (SELECT COUNT(*) FROM emp_detail WHERE designation IS NULL) AS employees_without_designation;

-- ===========================================================================
-- STEP 2 — run ONCE per company after the script above (edit the values first):
-- the first designation for your admin, so someone can log in as Super Admin and
-- create the other designations. 90 = default "Super Admin from level".
-- ===========================================================================
-- USE acme_db;
-- INSERT INTO designation_master (designation, hierarchy_level, display_order, designation_category)
--     VALUES ('Super Admin', 99, 99, 'Admin');
-- UPDATE emp_detail SET designation = LAST_INSERT_ID() WHERE emp_id = 'EMP-ADM';   -- your admin's emp_id
