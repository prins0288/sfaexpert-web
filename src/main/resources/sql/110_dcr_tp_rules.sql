-- 110: DCR rules: DAY_END_TIME (latest time to end / submit the day), TP_SUBMIT_CHECK (tour plan submitted + approved before the DCR is submitted), TP_VALIDATION (from this day of the month the NEXT month's tour plan must be submitted). Safe to re-run.

USE acme_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_END_TIME','Latest time to end / submit the day','DCR','DAY_END','APP','LIMIT','TIME','23:59','23:59','The day cannot be ended / submitted after this time (today only). Past days are not affected.',15),
 (NULL,'TP_SUBMIT_CHECK','Tour plan must be submitted and approved to submit the DCR','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a field DCR cannot be submitted unless the tour plan of that month is submitted and then approved.',16),
 (NULL,'TP_VALIDATION','Next month tour plan due from day of month','DCR','SUBMIT','APP','VALIDATION','INTEGER','0','0','Day of the month, e.g. 25: from that day the DCR cannot be submitted unless the NEXT month tour plan is submitted (approval is not checked for next month). 0 = off.',17);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('DAY_END_TIME','TP_SUBMIT_CHECK','TP_VALIDATION');

USE globex_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_END_TIME','Latest time to end / submit the day','DCR','DAY_END','APP','LIMIT','TIME','23:59','23:59','The day cannot be ended / submitted after this time (today only). Past days are not affected.',15),
 (NULL,'TP_SUBMIT_CHECK','Tour plan must be submitted and approved to submit the DCR','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a field DCR cannot be submitted unless the tour plan of that month is submitted and then approved.',16),
 (NULL,'TP_VALIDATION','Next month tour plan due from day of month','DCR','SUBMIT','APP','VALIDATION','INTEGER','0','0','Day of the month, e.g. 25: from that day the DCR cannot be submitted unless the NEXT month tour plan is submitted (approval is not checked for next month). 0 = off.',17);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('DAY_END_TIME','TP_SUBMIT_CHECK','TP_VALIDATION');

USE initech_db;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_END_TIME','Latest time to end / submit the day','DCR','DAY_END','APP','LIMIT','TIME','23:59','23:59','The day cannot be ended / submitted after this time (today only). Past days are not affected.',15),
 (NULL,'TP_SUBMIT_CHECK','Tour plan must be submitted and approved to submit the DCR','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a field DCR cannot be submitted unless the tour plan of that month is submitted and then approved.',16),
 (NULL,'TP_VALIDATION','Next month tour plan due from day of month','DCR','SUBMIT','APP','VALIDATION','INTEGER','0','0','Day of the month, e.g. 25: from that day the DCR cannot be submitted unless the NEXT month tour plan is submitted (approval is not checked for next month). 0 = off.',17);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('DAY_END_TIME','TP_SUBMIT_CHECK','TP_VALIDATION');

USE sfa_demo;

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'DAY_END_TIME','Latest time to end / submit the day','DCR','DAY_END','APP','LIMIT','TIME','23:59','23:59','The day cannot be ended / submitted after this time (today only). Past days are not affected.',15),
 (NULL,'TP_SUBMIT_CHECK','Tour plan must be submitted and approved to submit the DCR','DCR','SUBMIT','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = a field DCR cannot be submitted unless the tour plan of that month is submitted and then approved.',16),
 (NULL,'TP_VALIDATION','Next month tour plan due from day of month','DCR','SUBMIT','APP','VALIDATION','INTEGER','0','0','Day of the month, e.g. 25: from that day the DCR cannot be submitted unless the NEXT month tour plan is submitted (approval is not checked for next month). 0 = off.',17);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('DAY_END_TIME','TP_SUBMIT_CHECK','TP_VALIDATION');
