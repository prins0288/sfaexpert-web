-- 107: automatic DCR for holidays and weekly offs. A MySQL event runs every day at 8 PM (server time) and puts a submitted DCR (work type HOLIDAY / WEEKLY_OFF)
-- on every employee whose same day is a holiday / weekly off as set in Holiday Master. Office staff are skipped. Safe to re-run.
-- Needs the MySQL event scheduler ON (this script tries SET GLOBAL event_scheduler = ON; also put event_scheduler=ON in my.ini / my.cnf).

USE acme_db;

SET GLOBAL event_scheduler = ON;
DROP EVENT IF EXISTS ev_auto_holiday_dcr;
DROP PROCEDURE IF EXISTS sp_auto_holiday_dcr;
DELIMITER $$
CREATE PROCEDURE sp_auto_holiday_dcr(IN p_date DATE)
BEGIN
  -- Puts a submitted DCR (work type HOLIDAY / WEEKLY_OFF) for p_date on every employee whose day is a holiday / weekly off AS SET in Holiday Master
  -- (holidays + their state / HQ / employee assignment, the company weekly-off days, the weekly-off rules). Employees who already have a DCR that day are skipped.
  DECLARE v_company VARCHAR(64);
  DECLARE v_week VARCHAR(100);
  DECLARE v_dow VARCHAR(3);
  DECLARE v_nth INT;
  DECLARE v_last INT;
  SET v_company = (SELECT company_code FROM sfa_central.client_details WHERE db_name = DATABASE() LIMIT 1);
  SET v_week = COALESCE((SELECT UPPER(REPLACE(setting_value, ' ', '')) FROM sfa_central.company_setting_master WHERE company_code = v_company AND setting_key = 'weeklyOffDays' LIMIT 1), '');
  SET v_dow = ELT(WEEKDAY(p_date) + 1, 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
  SET v_nth = (DAYOFMONTH(p_date) - 1) DIV 7 + 1;
  SET v_last = IF(DAYOFMONTH(p_date) + 7 > DAYOFMONTH(LAST_DAY(p_date)), 1, 0);

  -- 1. holidays (a holiday wins over a weekly off)
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'HOLIDAY', e.designation, e.manager_id, LEFT(h.holiday_name, 150), 1, NOW(), 3
  FROM emp_detail e JOIN holiday_master h ON h.holiday_date = p_date AND h.is_active = 1
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)
         OR EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id AND (
              (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
           OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
           OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))
  ORDER BY h.id DESC;

  -- 2. weekly off: the company days, or a weekly-off rule (nth weekday of the month) that covers the employee
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'WEEKLY_OFF', e.designation, e.manager_id, 'Weekly off', 1, NOW(), 3
  FROM emp_detail e
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (FIND_IN_SET(v_dow, v_week) > 0
         OR EXISTS (SELECT 1 FROM weekly_off_rule r
                    WHERE r.is_active = 1 AND FIND_IN_SET(v_dow, REPLACE(UPPER(r.weekdays), ' ', '')) > 0
                      AND (UPPER(r.weeks) = 'ALL' OR r.weeks = '' OR FIND_IN_SET(v_nth, REPLACE(r.weeks, ' ', '')) > 0 OR (v_last = 1 AND FIND_IN_SET('L', REPLACE(UPPER(r.weeks), ' ', '')) > 0))
                      AND (NOT EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id)
                           OR EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id AND (
                                (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
                             OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
                             OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))));
END$$

CREATE EVENT ev_auto_holiday_dcr
  ON SCHEDULE EVERY 1 DAY STARTS IF(CURTIME() < '20:00:00', TIMESTAMP(CURDATE(), '20:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '20:00:00'))
  ON COMPLETION PRESERVE ENABLE
  COMMENT 'every day 8 PM: holiday / weekly-off DCR of the same day'
  DO CALL sp_auto_holiday_dcr(CURDATE())$$
DELIMITER ;

USE globex_db;

SET GLOBAL event_scheduler = ON;
DROP EVENT IF EXISTS ev_auto_holiday_dcr;
DROP PROCEDURE IF EXISTS sp_auto_holiday_dcr;
DELIMITER $$
CREATE PROCEDURE sp_auto_holiday_dcr(IN p_date DATE)
BEGIN
  -- Puts a submitted DCR (work type HOLIDAY / WEEKLY_OFF) for p_date on every employee whose day is a holiday / weekly off AS SET in Holiday Master
  -- (holidays + their state / HQ / employee assignment, the company weekly-off days, the weekly-off rules). Employees who already have a DCR that day are skipped.
  DECLARE v_company VARCHAR(64);
  DECLARE v_week VARCHAR(100);
  DECLARE v_dow VARCHAR(3);
  DECLARE v_nth INT;
  DECLARE v_last INT;
  SET v_company = (SELECT company_code FROM sfa_central.client_details WHERE db_name = DATABASE() LIMIT 1);
  SET v_week = COALESCE((SELECT UPPER(REPLACE(setting_value, ' ', '')) FROM sfa_central.company_setting_master WHERE company_code = v_company AND setting_key = 'weeklyOffDays' LIMIT 1), '');
  SET v_dow = ELT(WEEKDAY(p_date) + 1, 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
  SET v_nth = (DAYOFMONTH(p_date) - 1) DIV 7 + 1;
  SET v_last = IF(DAYOFMONTH(p_date) + 7 > DAYOFMONTH(LAST_DAY(p_date)), 1, 0);

  -- 1. holidays (a holiday wins over a weekly off)
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'HOLIDAY', e.designation, e.manager_id, LEFT(h.holiday_name, 150), 1, NOW(), 3
  FROM emp_detail e JOIN holiday_master h ON h.holiday_date = p_date AND h.is_active = 1
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)
         OR EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id AND (
              (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
           OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
           OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))
  ORDER BY h.id DESC;

  -- 2. weekly off: the company days, or a weekly-off rule (nth weekday of the month) that covers the employee
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'WEEKLY_OFF', e.designation, e.manager_id, 'Weekly off', 1, NOW(), 3
  FROM emp_detail e
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (FIND_IN_SET(v_dow, v_week) > 0
         OR EXISTS (SELECT 1 FROM weekly_off_rule r
                    WHERE r.is_active = 1 AND FIND_IN_SET(v_dow, REPLACE(UPPER(r.weekdays), ' ', '')) > 0
                      AND (UPPER(r.weeks) = 'ALL' OR r.weeks = '' OR FIND_IN_SET(v_nth, REPLACE(r.weeks, ' ', '')) > 0 OR (v_last = 1 AND FIND_IN_SET('L', REPLACE(UPPER(r.weeks), ' ', '')) > 0))
                      AND (NOT EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id)
                           OR EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id AND (
                                (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
                             OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
                             OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))));
END$$

CREATE EVENT ev_auto_holiday_dcr
  ON SCHEDULE EVERY 1 DAY STARTS IF(CURTIME() < '20:00:00', TIMESTAMP(CURDATE(), '20:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '20:00:00'))
  ON COMPLETION PRESERVE ENABLE
  COMMENT 'every day 8 PM: holiday / weekly-off DCR of the same day'
  DO CALL sp_auto_holiday_dcr(CURDATE())$$
DELIMITER ;

USE initech_db;

SET GLOBAL event_scheduler = ON;
DROP EVENT IF EXISTS ev_auto_holiday_dcr;
DROP PROCEDURE IF EXISTS sp_auto_holiday_dcr;
DELIMITER $$
CREATE PROCEDURE sp_auto_holiday_dcr(IN p_date DATE)
BEGIN
  -- Puts a submitted DCR (work type HOLIDAY / WEEKLY_OFF) for p_date on every employee whose day is a holiday / weekly off AS SET in Holiday Master
  -- (holidays + their state / HQ / employee assignment, the company weekly-off days, the weekly-off rules). Employees who already have a DCR that day are skipped.
  DECLARE v_company VARCHAR(64);
  DECLARE v_week VARCHAR(100);
  DECLARE v_dow VARCHAR(3);
  DECLARE v_nth INT;
  DECLARE v_last INT;
  SET v_company = (SELECT company_code FROM sfa_central.client_details WHERE db_name = DATABASE() LIMIT 1);
  SET v_week = COALESCE((SELECT UPPER(REPLACE(setting_value, ' ', '')) FROM sfa_central.company_setting_master WHERE company_code = v_company AND setting_key = 'weeklyOffDays' LIMIT 1), '');
  SET v_dow = ELT(WEEKDAY(p_date) + 1, 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
  SET v_nth = (DAYOFMONTH(p_date) - 1) DIV 7 + 1;
  SET v_last = IF(DAYOFMONTH(p_date) + 7 > DAYOFMONTH(LAST_DAY(p_date)), 1, 0);

  -- 1. holidays (a holiday wins over a weekly off)
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'HOLIDAY', e.designation, e.manager_id, LEFT(h.holiday_name, 150), 1, NOW(), 3
  FROM emp_detail e JOIN holiday_master h ON h.holiday_date = p_date AND h.is_active = 1
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)
         OR EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id AND (
              (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
           OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
           OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))
  ORDER BY h.id DESC;

  -- 2. weekly off: the company days, or a weekly-off rule (nth weekday of the month) that covers the employee
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'WEEKLY_OFF', e.designation, e.manager_id, 'Weekly off', 1, NOW(), 3
  FROM emp_detail e
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (FIND_IN_SET(v_dow, v_week) > 0
         OR EXISTS (SELECT 1 FROM weekly_off_rule r
                    WHERE r.is_active = 1 AND FIND_IN_SET(v_dow, REPLACE(UPPER(r.weekdays), ' ', '')) > 0
                      AND (UPPER(r.weeks) = 'ALL' OR r.weeks = '' OR FIND_IN_SET(v_nth, REPLACE(r.weeks, ' ', '')) > 0 OR (v_last = 1 AND FIND_IN_SET('L', REPLACE(UPPER(r.weeks), ' ', '')) > 0))
                      AND (NOT EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id)
                           OR EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id AND (
                                (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
                             OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
                             OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))));
END$$

CREATE EVENT ev_auto_holiday_dcr
  ON SCHEDULE EVERY 1 DAY STARTS IF(CURTIME() < '20:00:00', TIMESTAMP(CURDATE(), '20:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '20:00:00'))
  ON COMPLETION PRESERVE ENABLE
  COMMENT 'every day 8 PM: holiday / weekly-off DCR of the same day'
  DO CALL sp_auto_holiday_dcr(CURDATE())$$
DELIMITER ;

USE sfa_demo;

SET GLOBAL event_scheduler = ON;
DROP EVENT IF EXISTS ev_auto_holiday_dcr;
DROP PROCEDURE IF EXISTS sp_auto_holiday_dcr;
DELIMITER $$
CREATE PROCEDURE sp_auto_holiday_dcr(IN p_date DATE)
BEGIN
  -- Puts a submitted DCR (work type HOLIDAY / WEEKLY_OFF) for p_date on every employee whose day is a holiday / weekly off AS SET in Holiday Master
  -- (holidays + their state / HQ / employee assignment, the company weekly-off days, the weekly-off rules). Employees who already have a DCR that day are skipped.
  DECLARE v_company VARCHAR(64);
  DECLARE v_week VARCHAR(100);
  DECLARE v_dow VARCHAR(3);
  DECLARE v_nth INT;
  DECLARE v_last INT;
  SET v_company = (SELECT company_code FROM sfa_central.client_details WHERE db_name = DATABASE() LIMIT 1);
  SET v_week = COALESCE((SELECT UPPER(REPLACE(setting_value, ' ', '')) FROM sfa_central.company_setting_master WHERE company_code = v_company AND setting_key = 'weeklyOffDays' LIMIT 1), '');
  SET v_dow = ELT(WEEKDAY(p_date) + 1, 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
  SET v_nth = (DAYOFMONTH(p_date) - 1) DIV 7 + 1;
  SET v_last = IF(DAYOFMONTH(p_date) + 7 > DAYOFMONTH(LAST_DAY(p_date)), 1, 0);

  -- 1. holidays (a holiday wins over a weekly off)
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'HOLIDAY', e.designation, e.manager_id, LEFT(h.holiday_name, 150), 1, NOW(), 3
  FROM emp_detail e JOIN holiday_master h ON h.holiday_date = p_date AND h.is_active = 1
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)
         OR EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id AND (
              (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
           OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
           OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))
  ORDER BY h.id DESC;

  -- 2. weekly off: the company days, or a weekly-off rule (nth weekday of the month) that covers the employee
  INSERT IGNORE INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through)
  SELECT e.emp_id, p_date, 'WEEKLY_OFF', e.designation, e.manager_id, 'Weekly off', 1, NOW(), 3
  FROM emp_detail e
  WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0
    AND (e.date_of_joining IS NULL OR e.date_of_joining <= p_date) AND (e.last_working_date IS NULL OR e.last_working_date >= p_date)
    AND (FIND_IN_SET(v_dow, v_week) > 0
         OR EXISTS (SELECT 1 FROM weekly_off_rule r
                    WHERE r.is_active = 1 AND FIND_IN_SET(v_dow, REPLACE(UPPER(r.weekdays), ' ', '')) > 0
                      AND (UPPER(r.weeks) = 'ALL' OR r.weeks = '' OR FIND_IN_SET(v_nth, REPLACE(r.weeks, ' ', '')) > 0 OR (v_last = 1 AND FIND_IN_SET('L', REPLACE(UPPER(r.weeks), ' ', '')) > 0))
                      AND (NOT EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id)
                           OR EXISTS (SELECT 1 FROM weekly_off_assignment a WHERE a.rule_id = r.id AND (
                                (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
                             OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
                             OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))));
END$$

CREATE EVENT ev_auto_holiday_dcr
  ON SCHEDULE EVERY 1 DAY STARTS IF(CURTIME() < '20:00:00', TIMESTAMP(CURDATE(), '20:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '20:00:00'))
  ON COMPLETION PRESERVE ENABLE
  COMMENT 'every day 8 PM: holiday / weekly-off DCR of the same day'
  DO CALL sp_auto_holiday_dcr(CURDATE())$$
DELIMITER ;
