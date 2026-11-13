-- 117: position (lat / lng / address) of the Day End and of every client Check-out + rule FINAL_SUBMIT_REQUIRED. Safe to re-run.

USE acme_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'day_end_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN day_end_lat DECIMAL(10,7) NULL, ADD COLUMN day_end_lng DECIMAL(10,7) NULL, ADD COLUMN day_end_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'FINAL_SUBMIT_REQUIRED','Final submit required','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the DCR counts only after the Final Submit (a draft is Absent). FALSE = Final Submit is not needed: Day End is the final submit, and a started DCR counts in the Attendance Report even if it stays a draft.',19);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'FINAL_SUBMIT_REQUIRED';

USE globex_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'day_end_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN day_end_lat DECIMAL(10,7) NULL, ADD COLUMN day_end_lng DECIMAL(10,7) NULL, ADD COLUMN day_end_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'FINAL_SUBMIT_REQUIRED','Final submit required','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the DCR counts only after the Final Submit (a draft is Absent). FALSE = Final Submit is not needed: Day End is the final submit, and a started DCR counts in the Attendance Report even if it stays a draft.',19);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'FINAL_SUBMIT_REQUIRED';

USE initech_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'day_end_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN day_end_lat DECIMAL(10,7) NULL, ADD COLUMN day_end_lng DECIMAL(10,7) NULL, ADD COLUMN day_end_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'FINAL_SUBMIT_REQUIRED','Final submit required','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the DCR counts only after the Final Submit (a draft is Absent). FALSE = Final Submit is not needed: Day End is the final submit, and a started DCR counts in the Attendance Report even if it stays a draft.',19);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'FINAL_SUBMIT_REQUIRED';

USE sfa_demo;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'day_end_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN day_end_lat DECIMAL(10,7) NULL, ADD COLUMN day_end_lng DECIMAL(10,7) NULL, ADD COLUMN day_end_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkout_lat') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkout_lat DECIMAL(10,7) NULL, ADD COLUMN checkout_lng DECIMAL(10,7) NULL, ADD COLUMN checkout_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'FINAL_SUBMIT_REQUIRED','Final submit required','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = the DCR counts only after the Final Submit (a draft is Absent). FALSE = Final Submit is not needed: Day End is the final submit, and a started DCR counts in the Attendance Report even if it stays a draft.',19);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'FINAL_SUBMIT_REQUIRED';
