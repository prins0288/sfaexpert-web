-- 120: area_master.is_home (the starting point of an employee, one per employee) + rule AUTO_HOME_AREA. Safe to re-run.

USE acme_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'is_home') = 0,
  'ALTER TABLE area_master ADD COLUMN is_home TINYINT(1) NOT NULL DEFAULT 0 COMMENT "1 = the starting point of the employee (emp_id); only one per employee"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- one home per employee, guaranteed by the database: the key is the emp_id of a home row and NULL otherwise (NULLs may repeat)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'home_emp_key') = 0,
  'ALTER TABLE area_master ADD COLUMN home_emp_key VARCHAR(40) GENERATED ALWAYS AS (CASE WHEN is_home = 1 THEN emp_id ELSE NULL END) STORED, ADD UNIQUE KEY uq_area_one_home (home_emp_key)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- employees that have no home yet: an area of theirs named like their HQ becomes the home (nothing is created here)
UPDATE area_master a
JOIN (SELECT e.emp_id, MIN(x.id) AS id FROM emp_detail e JOIN hq_master h ON h.oid = e.hq_id JOIN area_master x ON x.emp_id = e.emp_id AND LOWER(TRIM(x.area_name)) = LOWER(TRIM(h.hq_name))
      WHERE NOT EXISTS (SELECT 1 FROM area_master z WHERE z.emp_id = e.emp_id AND z.is_home = 1) GROUP BY e.emp_id) t ON t.id = a.id
SET a.is_home = 1;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'AUTO_HOME_AREA','Create the home area of a new employee automatically','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = when an employee is created, an area named like his HQ is added to Area Master as his starting point (home) and is pre-selected at Start Day. FALSE = home areas are set by hand in Area Master (Starting point).',20);

USE globex_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'is_home') = 0,
  'ALTER TABLE area_master ADD COLUMN is_home TINYINT(1) NOT NULL DEFAULT 0 COMMENT "1 = the starting point of the employee (emp_id); only one per employee"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- one home per employee, guaranteed by the database: the key is the emp_id of a home row and NULL otherwise (NULLs may repeat)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'home_emp_key') = 0,
  'ALTER TABLE area_master ADD COLUMN home_emp_key VARCHAR(40) GENERATED ALWAYS AS (CASE WHEN is_home = 1 THEN emp_id ELSE NULL END) STORED, ADD UNIQUE KEY uq_area_one_home (home_emp_key)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- employees that have no home yet: an area of theirs named like their HQ becomes the home (nothing is created here)
UPDATE area_master a
JOIN (SELECT e.emp_id, MIN(x.id) AS id FROM emp_detail e JOIN hq_master h ON h.oid = e.hq_id JOIN area_master x ON x.emp_id = e.emp_id AND LOWER(TRIM(x.area_name)) = LOWER(TRIM(h.hq_name))
      WHERE NOT EXISTS (SELECT 1 FROM area_master z WHERE z.emp_id = e.emp_id AND z.is_home = 1) GROUP BY e.emp_id) t ON t.id = a.id
SET a.is_home = 1;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'AUTO_HOME_AREA','Create the home area of a new employee automatically','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = when an employee is created, an area named like his HQ is added to Area Master as his starting point (home) and is pre-selected at Start Day. FALSE = home areas are set by hand in Area Master (Starting point).',20);

USE initech_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'is_home') = 0,
  'ALTER TABLE area_master ADD COLUMN is_home TINYINT(1) NOT NULL DEFAULT 0 COMMENT "1 = the starting point of the employee (emp_id); only one per employee"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- one home per employee, guaranteed by the database: the key is the emp_id of a home row and NULL otherwise (NULLs may repeat)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'home_emp_key') = 0,
  'ALTER TABLE area_master ADD COLUMN home_emp_key VARCHAR(40) GENERATED ALWAYS AS (CASE WHEN is_home = 1 THEN emp_id ELSE NULL END) STORED, ADD UNIQUE KEY uq_area_one_home (home_emp_key)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- employees that have no home yet: an area of theirs named like their HQ becomes the home (nothing is created here)
UPDATE area_master a
JOIN (SELECT e.emp_id, MIN(x.id) AS id FROM emp_detail e JOIN hq_master h ON h.oid = e.hq_id JOIN area_master x ON x.emp_id = e.emp_id AND LOWER(TRIM(x.area_name)) = LOWER(TRIM(h.hq_name))
      WHERE NOT EXISTS (SELECT 1 FROM area_master z WHERE z.emp_id = e.emp_id AND z.is_home = 1) GROUP BY e.emp_id) t ON t.id = a.id
SET a.is_home = 1;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'AUTO_HOME_AREA','Create the home area of a new employee automatically','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = when an employee is created, an area named like his HQ is added to Area Master as his starting point (home) and is pre-selected at Start Day. FALSE = home areas are set by hand in Area Master (Starting point).',20);

USE sfa_demo;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'is_home') = 0,
  'ALTER TABLE area_master ADD COLUMN is_home TINYINT(1) NOT NULL DEFAULT 0 COMMENT "1 = the starting point of the employee (emp_id); only one per employee"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- one home per employee, guaranteed by the database: the key is the emp_id of a home row and NULL otherwise (NULLs may repeat)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'home_emp_key') = 0,
  'ALTER TABLE area_master ADD COLUMN home_emp_key VARCHAR(40) GENERATED ALWAYS AS (CASE WHEN is_home = 1 THEN emp_id ELSE NULL END) STORED, ADD UNIQUE KEY uq_area_one_home (home_emp_key)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- employees that have no home yet: an area of theirs named like their HQ becomes the home (nothing is created here)
UPDATE area_master a
JOIN (SELECT e.emp_id, MIN(x.id) AS id FROM emp_detail e JOIN hq_master h ON h.oid = e.hq_id JOIN area_master x ON x.emp_id = e.emp_id AND LOWER(TRIM(x.area_name)) = LOWER(TRIM(h.hq_name))
      WHERE NOT EXISTS (SELECT 1 FROM area_master z WHERE z.emp_id = e.emp_id AND z.is_home = 1) GROUP BY e.emp_id) t ON t.id = a.id
SET a.is_home = 1;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'AUTO_HOME_AREA','Create the home area of a new employee automatically','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = when an employee is created, an area named like his HQ is added to Area Master as his starting point (home) and is pre-selected at Start Day. FALSE = home areas are set by hand in Area Master (Starting point).',20);
