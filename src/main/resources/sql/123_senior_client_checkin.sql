-- 123: rule SENIOR_CLIENT_CHECKIN (a senior can check in the clients of his team, location-checked like the owner). Safe to re-run.

USE acme_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'SENIOR_CLIENT_CHECKIN','Seniors can check in the clients of their team','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a manager / senior can add the clients of the employees under him to his own DCR. The location check is the same: he must be at a tagged place of that client (any of its places, within GEO_FENCING_DISTANCE), otherwise the far-away message is shown. FALSE = a user sees only his own clients.',21);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'SENIOR_CLIENT_CHECKIN';

USE globex_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'SENIOR_CLIENT_CHECKIN','Seniors can check in the clients of their team','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a manager / senior can add the clients of the employees under him to his own DCR. The location check is the same: he must be at a tagged place of that client (any of its places, within GEO_FENCING_DISTANCE), otherwise the far-away message is shown. FALSE = a user sees only his own clients.',21);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'SENIOR_CLIENT_CHECKIN';

USE initech_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'SENIOR_CLIENT_CHECKIN','Seniors can check in the clients of their team','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a manager / senior can add the clients of the employees under him to his own DCR. The location check is the same: he must be at a tagged place of that client (any of its places, within GEO_FENCING_DISTANCE), otherwise the far-away message is shown. FALSE = a user sees only his own clients.',21);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'SENIOR_CLIENT_CHECKIN';

USE sfa_demo;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'SENIOR_CLIENT_CHECKIN','Seniors can check in the clients of their team','DCR','CHECKIN','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a manager / senior can add the clients of the employees under him to his own DCR. The location check is the same: he must be at a tagged place of that client (any of its places, within GEO_FENCING_DISTANCE), otherwise the far-away message is shown. FALSE = a user sees only his own clients.',21);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code = 'SENIOR_CLIENT_CHECKIN';
