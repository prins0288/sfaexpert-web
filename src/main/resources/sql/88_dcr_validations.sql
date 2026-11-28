-- ===========================================================================
-- 88: DCR rules in app_validations (master + per-employee).
--   emp_id NULL = master rule; a row with an emp_id overrides that ONE rule for that employee.
--   Rule codes are the names used in the old app (DAY_START_TIME, DCR_BETWEEN_TIME ...).
--   The old UNIQUE(config_code) is dropped - it allowed only ONE row per code, so an
--   employee could never have their own copy; UNIQUE(emp_key, config_code) replaces it
--   (and also stops duplicate master rows, which UNIQUE(emp_id, ..) lets through for NULL).
-- Safe to re-run.
-- ===========================================================================
-- Runs for every tenant DB below. Run: mysql -u myroot < 88_dcr_validations.sql

-- ==================== acme_db ====================
USE acme_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND index_name = 'uk_code') > 0,
             'ALTER TABLE app_validations DROP INDEX uk_code', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND column_name = 'emp_key') = 0,
             "ALTER TABLE app_validations ADD emp_key VARCHAR(100) AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows', ADD UNIQUE KEY uk_emp_key_code (emp_key, config_code)",
             'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_START_STOP_BEFORE_TIME','Earliest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','08:00','08:00','A day cannot be started before this time (today only).',10),
 (NULL,'DAY_START_TIME','Latest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','23:59','23:59','A day cannot be started after this time (today only).',11),
 (NULL,'DAY_START_TRAVEL_MODE_VISIBLITY','Show travel mode at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the travel mode field.',12),
 (NULL,'DAY_START_TRAVEL_MODE_MANDATORY','Travel mode is mandatory','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = a field day cannot start without a travel mode.',13),
 (NULL,'METER_READING_DAYSTART_VISIBLITY','Show meter reading at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter start field.',14),
 (NULL,'DCR_BETWEEN_TIME','Client calls allowed between','DCR','CHECKIN','APP','LIMIT','STRING','08:00-18:00','08:00-18:00','Check-in / check-out time of a call must fall in this window (HH:mm-HH:mm).',20),
 (NULL,'CHECKOUT','Check-out is compulsory','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = every call must be checked out before the day is submitted.',21),
 (NULL,'DAYEND_BEFORE_DAYSTART_VALIDATION','Day end cannot be before day start','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a past day cannot be submitted with a time out earlier than its time in.',31),
 (NULL,'DCR_Remarks','Day remarks are mandatory','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the day cannot be submitted without remarks.',30),
 (NULL,'UNLISTED_CLIENT_MAXIMUM_VALIDATION','Max unlisted check-ins per day','DCR','UNLISTED','APP','LIMIT','INTEGER','0','0','0 = no limit.',40),
 (NULL,'UNLISTED_VISIT_CATEGORY_VISIBLITY','Unlisted: show category','DCR','UNLISTED','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the field; the mandatory rule works only while this is TRUE.',45),
 (NULL,'UNLISTED_VISIT_SPECIALITY_VISIBLITY','Unlisted: show speciality','DCR','UNLISTED','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the field; the mandatory rule works only while this is TRUE.',46),
 (NULL,'UNLISTED_VISIT_MOBILE_VISIBLITY','Unlisted: show mobile','DCR','UNLISTED','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the field; the mandatory rule works only while this is TRUE.',47),
 (NULL,'UNLISTED_VISIT_CATEGORY','Unlisted: Category is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',41),
 (NULL,'UNLISTED_VISIT_SPECIALITY','Unlisted: Speciality is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',42),
 (NULL,'UNLISTED_VISIT_MOBILE','Unlisted: Mobile is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',43),
 (NULL,'UNLISTED_VISIT_EMAIL','Unlisted: E-mail is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',44);

-- every employee gets their own copy of the master DCR rules
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR';

-- ==================== globex_db ====================
USE globex_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND index_name = 'uk_code') > 0,
             'ALTER TABLE app_validations DROP INDEX uk_code', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND column_name = 'emp_key') = 0,
             "ALTER TABLE app_validations ADD emp_key VARCHAR(100) AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows', ADD UNIQUE KEY uk_emp_key_code (emp_key, config_code)",
             'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_START_STOP_BEFORE_TIME','Earliest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','08:00','08:00','A day cannot be started before this time (today only).',10),
 (NULL,'DAY_START_TIME','Latest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','23:59','23:59','A day cannot be started after this time (today only).',11),
 (NULL,'DAY_START_TRAVEL_MODE_VISIBLITY','Show travel mode at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the travel mode field.',12),
 (NULL,'DAY_START_TRAVEL_MODE_MANDATORY','Travel mode is mandatory','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = a field day cannot start without a travel mode.',13),
 (NULL,'METER_READING_DAYSTART_VISIBLITY','Show meter reading at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter start field.',14),
 (NULL,'DCR_BETWEEN_TIME','Client calls allowed between','DCR','CHECKIN','APP','LIMIT','STRING','08:00-18:00','08:00-18:00','Check-in / check-out time of a call must fall in this window (HH:mm-HH:mm).',20),
 (NULL,'CHECKOUT','Check-out is compulsory','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = every call must be checked out before the day is submitted.',21),
 (NULL,'DAYEND_BEFORE_DAYSTART_VALIDATION','Day end cannot be before day start','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a past day cannot be submitted with a time out earlier than its time in.',31),
 (NULL,'DCR_Remarks','Day remarks are mandatory','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the day cannot be submitted without remarks.',30),
 (NULL,'UNLISTED_CLIENT_MAXIMUM_VALIDATION','Max unlisted check-ins per day','DCR','UNLISTED','APP','LIMIT','INTEGER','0','0','0 = no limit.',40),
 (NULL,'UNLISTED_VISIT_CATEGORY','Unlisted: Category is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',41),
 (NULL,'UNLISTED_VISIT_SPECIALITY','Unlisted: Speciality is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',42),
 (NULL,'UNLISTED_VISIT_MOBILE','Unlisted: Mobile is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',43),
 (NULL,'UNLISTED_VISIT_EMAIL','Unlisted: E-mail is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',44);

-- every employee gets their own copy of the master DCR rules
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR';

-- ==================== initech_db ====================
USE initech_db;

SET @s := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND index_name = 'uk_code') > 0,
             'ALTER TABLE app_validations DROP INDEX uk_code', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND column_name = 'emp_key') = 0,
             "ALTER TABLE app_validations ADD emp_key VARCHAR(100) AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows', ADD UNIQUE KEY uk_emp_key_code (emp_key, config_code)",
             'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_START_STOP_BEFORE_TIME','Earliest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','08:00','08:00','A day cannot be started before this time (today only).',10),
 (NULL,'DAY_START_TIME','Latest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','23:59','23:59','A day cannot be started after this time (today only).',11),
 (NULL,'DAY_START_TRAVEL_MODE_VISIBLITY','Show travel mode at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the travel mode field.',12),
 (NULL,'DAY_START_TRAVEL_MODE_MANDATORY','Travel mode is mandatory','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = a field day cannot start without a travel mode.',13),
 (NULL,'METER_READING_DAYSTART_VISIBLITY','Show meter reading at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter start field.',14),
 (NULL,'DCR_BETWEEN_TIME','Client calls allowed between','DCR','CHECKIN','APP','LIMIT','STRING','08:00-18:00','08:00-18:00','Check-in / check-out time of a call must fall in this window (HH:mm-HH:mm).',20),
 (NULL,'CHECKOUT','Check-out is compulsory','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = every call must be checked out before the day is submitted.',21),
 (NULL,'DAYEND_BEFORE_DAYSTART_VALIDATION','Day end cannot be before day start','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a past day cannot be submitted with a time out earlier than its time in.',31),
 (NULL,'DCR_Remarks','Day remarks are mandatory','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the day cannot be submitted without remarks.',30),
 (NULL,'UNLISTED_CLIENT_MAXIMUM_VALIDATION','Max unlisted check-ins per day','DCR','UNLISTED','APP','LIMIT','INTEGER','0','0','0 = no limit.',40),
 (NULL,'UNLISTED_VISIT_CATEGORY','Unlisted: Category is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',41),
 (NULL,'UNLISTED_VISIT_SPECIALITY','Unlisted: Speciality is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',42),
 (NULL,'UNLISTED_VISIT_MOBILE','Unlisted: Mobile is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',43),
 (NULL,'UNLISTED_VISIT_EMAIL','Unlisted: E-mail is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',44);

-- every employee gets their own copy of the master DCR rules
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR';

-- ==================== sfa_demo ====================
USE sfa_demo;

SET @s := IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND index_name = 'uk_code') > 0,
             'ALTER TABLE app_validations DROP INDEX uk_code', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_validations' AND column_name = 'emp_key') = 0,
             "ALTER TABLE app_validations ADD emp_key VARCHAR(100) AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows', ADD UNIQUE KEY uk_emp_key_code (emp_key, config_code)",
             'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_START_STOP_BEFORE_TIME','Earliest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','08:00','08:00','A day cannot be started before this time (today only).',10),
 (NULL,'DAY_START_TIME','Latest time to start the day','DCR','START_DAY','APP','LIMIT','TIME','23:59','23:59','A day cannot be started after this time (today only).',11),
 (NULL,'DAY_START_TRAVEL_MODE_VISIBLITY','Show travel mode at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','TRUE','TRUE','FALSE hides the travel mode field.',12),
 (NULL,'DAY_START_TRAVEL_MODE_MANDATORY','Travel mode is mandatory','DCR','START_DAY','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = a field day cannot start without a travel mode.',13),
 (NULL,'METER_READING_DAYSTART_VISIBLITY','Show meter reading at Start Day','DCR','START_DAY','APP','VISIBILITY','BOOLEAN','FALSE','FALSE','TRUE shows the meter start field.',14),
 (NULL,'DCR_BETWEEN_TIME','Client calls allowed between','DCR','CHECKIN','APP','LIMIT','STRING','08:00-18:00','08:00-18:00','Check-in / check-out time of a call must fall in this window (HH:mm-HH:mm).',20),
 (NULL,'CHECKOUT','Check-out is compulsory','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = every call must be checked out before the day is submitted.',21),
 (NULL,'DAYEND_BEFORE_DAYSTART_VALIDATION','Day end cannot be before day start','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a past day cannot be submitted with a time out earlier than its time in.',31),
 (NULL,'DCR_Remarks','Day remarks are mandatory','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the day cannot be submitted without remarks.',30),
 (NULL,'UNLISTED_CLIENT_MAXIMUM_VALIDATION','Max unlisted check-ins per day','DCR','UNLISTED','APP','LIMIT','INTEGER','0','0','0 = no limit.',40),
 (NULL,'UNLISTED_VISIT_CATEGORY','Unlisted: Category is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',41),
 (NULL,'UNLISTED_VISIT_SPECIALITY','Unlisted: Speciality is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',42),
 (NULL,'UNLISTED_VISIT_MOBILE','Unlisted: Mobile is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','TRUE','TRUE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',43),
 (NULL,'UNLISTED_VISIT_EMAIL','Unlisted: E-mail is mandatory','DCR','UNLISTED','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = mandatory, FALSE = optional (works only while its _VISIBLITY rule is TRUE)',44);

-- every employee gets their own copy of the master DCR rules
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR';

