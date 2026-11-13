-- 118: meter reading rules (start / end, visibility + validation), meter photos, camera rules (CAMERA_DIRECT / CAMERA_ONLY). Safe to re-run.

USE acme_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'meter_start_image') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN meter_start_image VARCHAR(255) NULL COMMENT "photo of the meter at start of day", ADD COLUMN meter_end_image VARCHAR(255) NULL COMMENT "photo of the meter at end of day"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'METER_READING_DAYSTART','Meter reading mandatory at Start Day','DCR','START_DAY','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter start reading (km) must be entered to start a field day.',14),
 (NULL,'METER_READING_DAYEND_VISIBLITY','Show meter reading at Day End','DCR','DAY_END','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter end reading (km) at Day End / Final Submit.',15),
 (NULL,'METER_READING_DAYEND','Meter reading mandatory at Day End','DCR','DAY_END','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter end reading must be entered at Day End / Final Submit (not less than the start reading).',15),
 (NULL,'CAMERA_DIRECT','Direct camera button for photos','DCR','SUBMIT','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','TRUE shows a Camera button that opens the camera directly; the photo carries the latitude, longitude and time.',16),
 (NULL,'CAMERA_ONLY','Photos only from the camera','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a photo cannot be picked from the gallery / files, only taken with the camera.',16);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('METER_READING_DAYSTART','METER_READING_DAYEND_VISIBLITY','METER_READING_DAYEND','CAMERA_DIRECT','CAMERA_ONLY');

USE globex_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'meter_start_image') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN meter_start_image VARCHAR(255) NULL COMMENT "photo of the meter at start of day", ADD COLUMN meter_end_image VARCHAR(255) NULL COMMENT "photo of the meter at end of day"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'METER_READING_DAYSTART','Meter reading mandatory at Start Day','DCR','START_DAY','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter start reading (km) must be entered to start a field day.',14),
 (NULL,'METER_READING_DAYEND_VISIBLITY','Show meter reading at Day End','DCR','DAY_END','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter end reading (km) at Day End / Final Submit.',15),
 (NULL,'METER_READING_DAYEND','Meter reading mandatory at Day End','DCR','DAY_END','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter end reading must be entered at Day End / Final Submit (not less than the start reading).',15),
 (NULL,'CAMERA_DIRECT','Direct camera button for photos','DCR','SUBMIT','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','TRUE shows a Camera button that opens the camera directly; the photo carries the latitude, longitude and time.',16),
 (NULL,'CAMERA_ONLY','Photos only from the camera','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a photo cannot be picked from the gallery / files, only taken with the camera.',16);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('METER_READING_DAYSTART','METER_READING_DAYEND_VISIBLITY','METER_READING_DAYEND','CAMERA_DIRECT','CAMERA_ONLY');

USE initech_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'meter_start_image') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN meter_start_image VARCHAR(255) NULL COMMENT "photo of the meter at start of day", ADD COLUMN meter_end_image VARCHAR(255) NULL COMMENT "photo of the meter at end of day"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'METER_READING_DAYSTART','Meter reading mandatory at Start Day','DCR','START_DAY','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter start reading (km) must be entered to start a field day.',14),
 (NULL,'METER_READING_DAYEND_VISIBLITY','Show meter reading at Day End','DCR','DAY_END','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter end reading (km) at Day End / Final Submit.',15),
 (NULL,'METER_READING_DAYEND','Meter reading mandatory at Day End','DCR','DAY_END','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter end reading must be entered at Day End / Final Submit (not less than the start reading).',15),
 (NULL,'CAMERA_DIRECT','Direct camera button for photos','DCR','SUBMIT','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','TRUE shows a Camera button that opens the camera directly; the photo carries the latitude, longitude and time.',16),
 (NULL,'CAMERA_ONLY','Photos only from the camera','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a photo cannot be picked from the gallery / files, only taken with the camera.',16);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('METER_READING_DAYSTART','METER_READING_DAYEND_VISIBLITY','METER_READING_DAYEND','CAMERA_DIRECT','CAMERA_ONLY');

USE sfa_demo;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'meter_start_image') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN meter_start_image VARCHAR(255) NULL COMMENT "photo of the meter at start of day", ADD COLUMN meter_end_image VARCHAR(255) NULL COMMENT "photo of the meter at end of day"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'METER_READING_DAYSTART','Meter reading mandatory at Start Day','DCR','START_DAY','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter start reading (km) must be entered to start a field day.',14),
 (NULL,'METER_READING_DAYEND_VISIBLITY','Show meter reading at Day End','DCR','DAY_END','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter end reading (km) at Day End / Final Submit.',15),
 (NULL,'METER_READING_DAYEND','Meter reading mandatory at Day End','DCR','DAY_END','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE (with the visibility rule TRUE) = the meter end reading must be entered at Day End / Final Submit (not less than the start reading).',15),
 (NULL,'CAMERA_DIRECT','Direct camera button for photos','DCR','SUBMIT','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','TRUE shows a Camera button that opens the camera directly; the photo carries the latitude, longitude and time.',16),
 (NULL,'CAMERA_ONLY','Photos only from the camera','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a photo cannot be picked from the gallery / files, only taken with the camera.',16);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('METER_READING_DAYSTART','METER_READING_DAYEND_VISIBLITY','METER_READING_DAYEND','CAMERA_DIRECT','CAMERA_ONLY');
