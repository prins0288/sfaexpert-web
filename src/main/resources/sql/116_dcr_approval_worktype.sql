-- 116: DCR approval per work type (app_dcr_worktype.needs_approval) and the company switch DCR_APPROVAL_REQUIRED (DCR Rules). Safe to re-run.

USE acme_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'needs_approval') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN needs_approval TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = a submitted DCR of this work type waits for the manager approval; 0 = approved automatically on submit"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DCR_APPROVAL_REQUIRED','DCR approval system','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the company works with DCR approval: the work types marked "needs approval" wait for the manager, and a DCR that is not approved shows as Absent in the Attendance Report. FALSE = no approval at all: every submitted DCR is approved automatically and nothing is Absent for want of approval.',18);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'DCR_APPROVAL_REQUIRED';

USE globex_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'needs_approval') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN needs_approval TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = a submitted DCR of this work type waits for the manager approval; 0 = approved automatically on submit"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DCR_APPROVAL_REQUIRED','DCR approval system','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the company works with DCR approval: the work types marked "needs approval" wait for the manager, and a DCR that is not approved shows as Absent in the Attendance Report. FALSE = no approval at all: every submitted DCR is approved automatically and nothing is Absent for want of approval.',18);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'DCR_APPROVAL_REQUIRED';

USE initech_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'needs_approval') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN needs_approval TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = a submitted DCR of this work type waits for the manager approval; 0 = approved automatically on submit"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DCR_APPROVAL_REQUIRED','DCR approval system','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the company works with DCR approval: the work types marked "needs approval" wait for the manager, and a DCR that is not approved shows as Absent in the Attendance Report. FALSE = no approval at all: every submitted DCR is approved automatically and nothing is Absent for want of approval.',18);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'DCR_APPROVAL_REQUIRED';

USE sfa_demo;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'needs_approval') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN needs_approval TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = a submitted DCR of this work type waits for the manager approval; 0 = approved automatically on submit"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DCR_APPROVAL_REQUIRED','DCR approval system','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the company works with DCR approval: the work types marked "needs approval" wait for the manager, and a DCR that is not approved shows as Absent in the Attendance Report. FALSE = no approval at all: every submitted DCR is approved automatically and nothing is Absent for want of approval.',18);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'DCR_APPROVAL_REQUIRED';
