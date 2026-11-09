-- ===========================================================================
-- 95: TOUR PLAN + HOLIDAYS.
--   tour_emp_parent / tour_child / tour_emp_area / tour_emp_clients / tour_emp_visit_with keep their NAMES with a new 2026 structure
--   (an old-structure table is kept as <name>_legacy), plus holiday_master, holiday_assignment, tour_emp_approval_log,
--   permissions TP_SAVE / TP_APPROVE / HOLIDAY_SAVE, the DCR rule TP_VALIDATE_FILTER, dcr_parent_summary.start_area_type, menus.
--   Settings (no migration): tourPlanApprovalFlow (AUTO / MANAGER / ADMIN / MANAGER_ADMIN, default MANAGER_ADMIN), weeklyOffDays (e.g. SUN).
-- Safe to re-run. Run: mysql -u myroot < 95_tour_plan.sql
-- ===========================================================================

USE acme_db;

-- ---- old tour_* tables (old structure, same names) are kept as *_legacy; the new tables take their names ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_parent' AND column_name = 'month') > 0,
             'RENAME TABLE tour_emp_parent TO tour_emp_parent_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_child' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_child TO tour_child_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_area' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_area TO tour_emp_area_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_clients' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_clients TO tour_emp_clients_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_visit_with' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_visit_with TO tour_emp_visit_with_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- holidays ----
CREATE TABLE IF NOT EXISTS holiday_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_date  DATE         NOT NULL COMMENT 'the day',
  holiday_name  VARCHAR(100) NOT NULL COMMENT 'Holi, Diwali, 2 October, 15 August ...',
  remarks       VARCHAR(250) NULL COMMENT 'shown with the holiday on the tour plan calendar',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = in force',
  created_by    VARCHAR(40)  NULL COMMENT 'who added it',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by    VARCHAR(40)  NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday (holiday_date, holiday_name),
  KEY ix_holiday_date (holiday_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holidays; no rows in holiday_assignment = for everyone';
CREATE TABLE IF NOT EXISTS holiday_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_id   INT UNSIGNED NOT NULL COMMENT '-> holiday_master.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE (state_master.oid), HQ (hq_master.oid) or EMP (emp_detail.emp_id)',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday_target (holiday_id, target_type, target_value),
  KEY ix_holiday_target (target_type, target_value),
  CONSTRAINT fk_ha_holiday FOREIGN KEY (holiday_id) REFERENCES holiday_master (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holiday -> state / HQ / employee; none = everyone';

-- ---- tour plan (one plan per employee per month) ----
CREATE TABLE IF NOT EXISTS tour_emp_parent (
  tour_main          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'plan id',
  emp_id             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan',
  tour_year          SMALLINT UNSIGNED NOT NULL,
  tour_month         TINYINT UNSIGNED  NOT NULL COMMENT '1-12',
  designation_id     INT          NULL COMMENT 'designation when the plan was made (promotion-proof)',
  manager_id         VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager when the plan was made',
  month_goal         TEXT         NULL COMMENT 'goal of the month',
  approval_status    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED',
  add_requested_by   VARCHAR(40)  NULL COMMENT 'who submitted it',
  submitted_at       DATETIME     NULL,
  manager_approved_by VARCHAR(40) NULL,
  manager_approved_on DATETIME    NULL,
  approved_by        VARCHAR(40)  NULL COMMENT 'final approver',
  approved_on        DATETIME     NULL,
  rejected_by        VARCHAR(40)  NULL,
  rejected_on        DATETIME     NULL,
  reject_reason      VARCHAR(255) NULL,
  created_by         VARCHAR(40)  NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by         VARCHAR(40)  NULL,
  updated_at         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tour_main),
  UNIQUE KEY uq_tour_emp_month (emp_id, tour_year, tour_month),
  KEY ix_tour_status (approval_status, tour_year, tour_month),
  KEY ix_tour_manager (manager_id, tour_year, tour_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_child (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date     DATE         NOT NULL,
  worktype_code VARCHAR(30)  NOT NULL COMMENT 'app_dcr_worktype day type, e.g. WORKING / LEAVE',
  stay_area_id  BIGINT       NULL COMMENT 'night stay area (area_master.oid), if staying',
  tour_remark   VARCHAR(250) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_day (tour_main, tour_date),
  KEY ix_tour_child_date (tour_date),
  CONSTRAINT fk_tc_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_area (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'area_master.oid planned for the day',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_area (tour_main, tour_date, area_id),
  KEY ix_tour_area_date (tour_date),
  CONSTRAINT fk_ta_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_clients (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'the planned area this client was picked under',
  client_id  BIGINT NOT NULL COMMENT 'client_master.id (client / retailer / party)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_client (tour_main, tour_date, client_id),
  KEY ix_tour_client_date (tour_date),
  CONSTRAINT fk_tcl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_visit_with (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL,
  tour_date     DATE        NOT NULL,
  visit_with_id VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'emp_id of the senior / junior planned to work with',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_with (tour_main, tour_date, visit_with_id),
  KEY ix_tour_with_date (tour_date),
  CONSTRAINT fk_tw_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_approval_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main    BIGINT UNSIGNED NOT NULL,
  step         VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / FINAL / AUTO / ADMIN',
  action       VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after VARCHAR(20)  NOT NULL,
  remarks      VARCHAR(255) NULL,
  by_emp_id    VARCHAR(40)  NULL,
  by_name      VARCHAR(150) NULL,
  by_designation VARCHAR(100) NULL,
  at_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_tour_log (tour_main),
  CONSTRAINT fk_tl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---- DCR: the type (HQ / EX / OUT) of the start area, saved with the day ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_area_type') = 0,
 "ALTER TABLE dcr_parent_summary ADD start_area_type VARCHAR(30) NULL COMMENT 'area_master.area_type (HQ / EX / OUT ...) of the start area, copied when the day starts' AFTER start_area_id", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- permissions (everyone allowed by default; every existing employee also gets their own rows) ----
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
  ('TP_SAVE', 'Tour Plan', 'Plan / submit my own tour plan', 1, 1, NOW(), 'seed'),
  ('TP_APPROVE', 'Tour Plan', 'Approve / reject tour plans (manager step / admin final)', 1, 1, NOW(), 'seed'),
  ('HOLIDAY_SAVE', 'Holiday', 'Add / edit holidays and weekly off days', 1, 1, NOW(), 'seed');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code, 1, NOW(), 'migration-95'
FROM emp_detail e JOIN permission_master m ON m.permission_code IN ('TP_SAVE', 'TP_APPROVE', 'HOLIDAY_SAVE')
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

-- ---- DCR rule: only the tour plan's areas / clients are offered when TRUE ----
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL, 'TP_VALIDATE_FILTER', 'DCR follows the approved tour plan', 'DCR', 'START_DAY', 'APP', 'VALIDATION', 'BOOLEAN', 'FALSE', 'FALSE',
  'TRUE = the day can start only from an area of the approved tour plan of that date, and check-in lists only the planned clients (an area with no client picked offers all its clients).', 15);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.config_code = 'TP_VALIDATE_FILTER';

-- ---- menus: Tour Plan next to the DCR, Holiday Master under Master Entry ----
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan (month calendar, submit, approval)', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'Holiday Master', 'Holidays and weekly off', 'calendar-event', 'holidaymaster', 'master/holiday-master.html', '_SELF', 25, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'holidaymaster');

USE globex_db;

-- ---- old tour_* tables (old structure, same names) are kept as *_legacy; the new tables take their names ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_parent' AND column_name = 'month') > 0,
             'RENAME TABLE tour_emp_parent TO tour_emp_parent_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_child' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_child TO tour_child_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_area' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_area TO tour_emp_area_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_clients' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_clients TO tour_emp_clients_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_visit_with' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_visit_with TO tour_emp_visit_with_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- holidays ----
CREATE TABLE IF NOT EXISTS holiday_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_date  DATE         NOT NULL COMMENT 'the day',
  holiday_name  VARCHAR(100) NOT NULL COMMENT 'Holi, Diwali, 2 October, 15 August ...',
  remarks       VARCHAR(250) NULL COMMENT 'shown with the holiday on the tour plan calendar',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = in force',
  created_by    VARCHAR(40)  NULL COMMENT 'who added it',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by    VARCHAR(40)  NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday (holiday_date, holiday_name),
  KEY ix_holiday_date (holiday_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holidays; no rows in holiday_assignment = for everyone';
CREATE TABLE IF NOT EXISTS holiday_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_id   INT UNSIGNED NOT NULL COMMENT '-> holiday_master.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE (state_master.oid), HQ (hq_master.oid) or EMP (emp_detail.emp_id)',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday_target (holiday_id, target_type, target_value),
  KEY ix_holiday_target (target_type, target_value),
  CONSTRAINT fk_ha_holiday FOREIGN KEY (holiday_id) REFERENCES holiday_master (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holiday -> state / HQ / employee; none = everyone';

-- ---- tour plan (one plan per employee per month) ----
CREATE TABLE IF NOT EXISTS tour_emp_parent (
  tour_main          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'plan id',
  emp_id             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan',
  tour_year          SMALLINT UNSIGNED NOT NULL,
  tour_month         TINYINT UNSIGNED  NOT NULL COMMENT '1-12',
  designation_id     INT          NULL COMMENT 'designation when the plan was made (promotion-proof)',
  manager_id         VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager when the plan was made',
  month_goal         TEXT         NULL COMMENT 'goal of the month',
  approval_status    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED',
  add_requested_by   VARCHAR(40)  NULL COMMENT 'who submitted it',
  submitted_at       DATETIME     NULL,
  manager_approved_by VARCHAR(40) NULL,
  manager_approved_on DATETIME    NULL,
  approved_by        VARCHAR(40)  NULL COMMENT 'final approver',
  approved_on        DATETIME     NULL,
  rejected_by        VARCHAR(40)  NULL,
  rejected_on        DATETIME     NULL,
  reject_reason      VARCHAR(255) NULL,
  created_by         VARCHAR(40)  NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by         VARCHAR(40)  NULL,
  updated_at         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tour_main),
  UNIQUE KEY uq_tour_emp_month (emp_id, tour_year, tour_month),
  KEY ix_tour_status (approval_status, tour_year, tour_month),
  KEY ix_tour_manager (manager_id, tour_year, tour_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_child (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date     DATE         NOT NULL,
  worktype_code VARCHAR(30)  NOT NULL COMMENT 'app_dcr_worktype day type, e.g. WORKING / LEAVE',
  stay_area_id  BIGINT       NULL COMMENT 'night stay area (area_master.oid), if staying',
  tour_remark   VARCHAR(250) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_day (tour_main, tour_date),
  KEY ix_tour_child_date (tour_date),
  CONSTRAINT fk_tc_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_area (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'area_master.oid planned for the day',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_area (tour_main, tour_date, area_id),
  KEY ix_tour_area_date (tour_date),
  CONSTRAINT fk_ta_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_clients (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'the planned area this client was picked under',
  client_id  BIGINT NOT NULL COMMENT 'client_master.id (client / retailer / party)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_client (tour_main, tour_date, client_id),
  KEY ix_tour_client_date (tour_date),
  CONSTRAINT fk_tcl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_visit_with (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL,
  tour_date     DATE        NOT NULL,
  visit_with_id VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'emp_id of the senior / junior planned to work with',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_with (tour_main, tour_date, visit_with_id),
  KEY ix_tour_with_date (tour_date),
  CONSTRAINT fk_tw_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_approval_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main    BIGINT UNSIGNED NOT NULL,
  step         VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / FINAL / AUTO / ADMIN',
  action       VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after VARCHAR(20)  NOT NULL,
  remarks      VARCHAR(255) NULL,
  by_emp_id    VARCHAR(40)  NULL,
  by_name      VARCHAR(150) NULL,
  by_designation VARCHAR(100) NULL,
  at_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_tour_log (tour_main),
  CONSTRAINT fk_tl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---- DCR: the type (HQ / EX / OUT) of the start area, saved with the day ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_area_type') = 0,
 "ALTER TABLE dcr_parent_summary ADD start_area_type VARCHAR(30) NULL COMMENT 'area_master.area_type (HQ / EX / OUT ...) of the start area, copied when the day starts' AFTER start_area_id", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- permissions (everyone allowed by default; every existing employee also gets their own rows) ----
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
  ('TP_SAVE', 'Tour Plan', 'Plan / submit my own tour plan', 1, 1, NOW(), 'seed'),
  ('TP_APPROVE', 'Tour Plan', 'Approve / reject tour plans (manager step / admin final)', 1, 1, NOW(), 'seed'),
  ('HOLIDAY_SAVE', 'Holiday', 'Add / edit holidays and weekly off days', 1, 1, NOW(), 'seed');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code, 1, NOW(), 'migration-95'
FROM emp_detail e JOIN permission_master m ON m.permission_code IN ('TP_SAVE', 'TP_APPROVE', 'HOLIDAY_SAVE')
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

-- ---- DCR rule: only the tour plan's areas / clients are offered when TRUE ----
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL, 'TP_VALIDATE_FILTER', 'DCR follows the approved tour plan', 'DCR', 'START_DAY', 'APP', 'VALIDATION', 'BOOLEAN', 'FALSE', 'FALSE',
  'TRUE = the day can start only from an area of the approved tour plan of that date, and check-in lists only the planned clients (an area with no client picked offers all its clients).', 15);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.config_code = 'TP_VALIDATE_FILTER';

-- ---- menus: Tour Plan next to the DCR, Holiday Master under Master Entry ----
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan (month calendar, submit, approval)', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'Holiday Master', 'Holidays and weekly off', 'calendar-event', 'holidaymaster', 'master/holiday-master.html', '_SELF', 25, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'holidaymaster');

USE initech_db;

-- ---- old tour_* tables (old structure, same names) are kept as *_legacy; the new tables take their names ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_parent' AND column_name = 'month') > 0,
             'RENAME TABLE tour_emp_parent TO tour_emp_parent_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_child' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_child TO tour_child_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_area' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_area TO tour_emp_area_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_clients' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_clients TO tour_emp_clients_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_visit_with' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_visit_with TO tour_emp_visit_with_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- holidays ----
CREATE TABLE IF NOT EXISTS holiday_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_date  DATE         NOT NULL COMMENT 'the day',
  holiday_name  VARCHAR(100) NOT NULL COMMENT 'Holi, Diwali, 2 October, 15 August ...',
  remarks       VARCHAR(250) NULL COMMENT 'shown with the holiday on the tour plan calendar',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = in force',
  created_by    VARCHAR(40)  NULL COMMENT 'who added it',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by    VARCHAR(40)  NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday (holiday_date, holiday_name),
  KEY ix_holiday_date (holiday_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holidays; no rows in holiday_assignment = for everyone';
CREATE TABLE IF NOT EXISTS holiday_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_id   INT UNSIGNED NOT NULL COMMENT '-> holiday_master.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE (state_master.oid), HQ (hq_master.oid) or EMP (emp_detail.emp_id)',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday_target (holiday_id, target_type, target_value),
  KEY ix_holiday_target (target_type, target_value),
  CONSTRAINT fk_ha_holiday FOREIGN KEY (holiday_id) REFERENCES holiday_master (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holiday -> state / HQ / employee; none = everyone';

-- ---- tour plan (one plan per employee per month) ----
CREATE TABLE IF NOT EXISTS tour_emp_parent (
  tour_main          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'plan id',
  emp_id             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan',
  tour_year          SMALLINT UNSIGNED NOT NULL,
  tour_month         TINYINT UNSIGNED  NOT NULL COMMENT '1-12',
  designation_id     INT          NULL COMMENT 'designation when the plan was made (promotion-proof)',
  manager_id         VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager when the plan was made',
  month_goal         TEXT         NULL COMMENT 'goal of the month',
  approval_status    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED',
  add_requested_by   VARCHAR(40)  NULL COMMENT 'who submitted it',
  submitted_at       DATETIME     NULL,
  manager_approved_by VARCHAR(40) NULL,
  manager_approved_on DATETIME    NULL,
  approved_by        VARCHAR(40)  NULL COMMENT 'final approver',
  approved_on        DATETIME     NULL,
  rejected_by        VARCHAR(40)  NULL,
  rejected_on        DATETIME     NULL,
  reject_reason      VARCHAR(255) NULL,
  created_by         VARCHAR(40)  NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by         VARCHAR(40)  NULL,
  updated_at         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tour_main),
  UNIQUE KEY uq_tour_emp_month (emp_id, tour_year, tour_month),
  KEY ix_tour_status (approval_status, tour_year, tour_month),
  KEY ix_tour_manager (manager_id, tour_year, tour_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_child (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date     DATE         NOT NULL,
  worktype_code VARCHAR(30)  NOT NULL COMMENT 'app_dcr_worktype day type, e.g. WORKING / LEAVE',
  stay_area_id  BIGINT       NULL COMMENT 'night stay area (area_master.oid), if staying',
  tour_remark   VARCHAR(250) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_day (tour_main, tour_date),
  KEY ix_tour_child_date (tour_date),
  CONSTRAINT fk_tc_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_area (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'area_master.oid planned for the day',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_area (tour_main, tour_date, area_id),
  KEY ix_tour_area_date (tour_date),
  CONSTRAINT fk_ta_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_clients (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'the planned area this client was picked under',
  client_id  BIGINT NOT NULL COMMENT 'client_master.id (client / retailer / party)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_client (tour_main, tour_date, client_id),
  KEY ix_tour_client_date (tour_date),
  CONSTRAINT fk_tcl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_visit_with (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL,
  tour_date     DATE        NOT NULL,
  visit_with_id VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'emp_id of the senior / junior planned to work with',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_with (tour_main, tour_date, visit_with_id),
  KEY ix_tour_with_date (tour_date),
  CONSTRAINT fk_tw_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_approval_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main    BIGINT UNSIGNED NOT NULL,
  step         VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / FINAL / AUTO / ADMIN',
  action       VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after VARCHAR(20)  NOT NULL,
  remarks      VARCHAR(255) NULL,
  by_emp_id    VARCHAR(40)  NULL,
  by_name      VARCHAR(150) NULL,
  by_designation VARCHAR(100) NULL,
  at_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_tour_log (tour_main),
  CONSTRAINT fk_tl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---- DCR: the type (HQ / EX / OUT) of the start area, saved with the day ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_area_type') = 0,
 "ALTER TABLE dcr_parent_summary ADD start_area_type VARCHAR(30) NULL COMMENT 'area_master.area_type (HQ / EX / OUT ...) of the start area, copied when the day starts' AFTER start_area_id", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- permissions (everyone allowed by default; every existing employee also gets their own rows) ----
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
  ('TP_SAVE', 'Tour Plan', 'Plan / submit my own tour plan', 1, 1, NOW(), 'seed'),
  ('TP_APPROVE', 'Tour Plan', 'Approve / reject tour plans (manager step / admin final)', 1, 1, NOW(), 'seed'),
  ('HOLIDAY_SAVE', 'Holiday', 'Add / edit holidays and weekly off days', 1, 1, NOW(), 'seed');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code, 1, NOW(), 'migration-95'
FROM emp_detail e JOIN permission_master m ON m.permission_code IN ('TP_SAVE', 'TP_APPROVE', 'HOLIDAY_SAVE')
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

-- ---- DCR rule: only the tour plan's areas / clients are offered when TRUE ----
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL, 'TP_VALIDATE_FILTER', 'DCR follows the approved tour plan', 'DCR', 'START_DAY', 'APP', 'VALIDATION', 'BOOLEAN', 'FALSE', 'FALSE',
  'TRUE = the day can start only from an area of the approved tour plan of that date, and check-in lists only the planned clients (an area with no client picked offers all its clients).', 15);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.config_code = 'TP_VALIDATE_FILTER';

-- ---- menus: Tour Plan next to the DCR, Holiday Master under Master Entry ----
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan (month calendar, submit, approval)', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'Holiday Master', 'Holidays and weekly off', 'calendar-event', 'holidaymaster', 'master/holiday-master.html', '_SELF', 25, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'holidaymaster');

USE sfa_demo;

-- ---- old tour_* tables (old structure, same names) are kept as *_legacy; the new tables take their names ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_parent' AND column_name = 'month') > 0,
             'RENAME TABLE tour_emp_parent TO tour_emp_parent_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_child' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_child TO tour_child_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_area' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_area TO tour_emp_area_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_clients' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_clients TO tour_emp_clients_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tour_emp_visit_with' AND column_name = 'create_date') > 0,
             'RENAME TABLE tour_emp_visit_with TO tour_emp_visit_with_legacy', 'SELECT 1'); PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- holidays ----
CREATE TABLE IF NOT EXISTS holiday_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_date  DATE         NOT NULL COMMENT 'the day',
  holiday_name  VARCHAR(100) NOT NULL COMMENT 'Holi, Diwali, 2 October, 15 August ...',
  remarks       VARCHAR(250) NULL COMMENT 'shown with the holiday on the tour plan calendar',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = in force',
  created_by    VARCHAR(40)  NULL COMMENT 'who added it',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by    VARCHAR(40)  NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday (holiday_date, holiday_name),
  KEY ix_holiday_date (holiday_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holidays; no rows in holiday_assignment = for everyone';
CREATE TABLE IF NOT EXISTS holiday_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  holiday_id   INT UNSIGNED NOT NULL COMMENT '-> holiday_master.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE (state_master.oid), HQ (hq_master.oid) or EMP (emp_detail.emp_id)',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_holiday_target (holiday_id, target_type, target_value),
  KEY ix_holiday_target (target_type, target_value),
  CONSTRAINT fk_ha_holiday FOREIGN KEY (holiday_id) REFERENCES holiday_master (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'holiday -> state / HQ / employee; none = everyone';

-- ---- tour plan (one plan per employee per month) ----
CREATE TABLE IF NOT EXISTS tour_emp_parent (
  tour_main          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'plan id',
  emp_id             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan',
  tour_year          SMALLINT UNSIGNED NOT NULL,
  tour_month         TINYINT UNSIGNED  NOT NULL COMMENT '1-12',
  designation_id     INT          NULL COMMENT 'designation when the plan was made (promotion-proof)',
  manager_id         VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager when the plan was made',
  month_goal         TEXT         NULL COMMENT 'goal of the month',
  approval_status    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED',
  add_requested_by   VARCHAR(40)  NULL COMMENT 'who submitted it',
  submitted_at       DATETIME     NULL,
  manager_approved_by VARCHAR(40) NULL,
  manager_approved_on DATETIME    NULL,
  approved_by        VARCHAR(40)  NULL COMMENT 'final approver',
  approved_on        DATETIME     NULL,
  rejected_by        VARCHAR(40)  NULL,
  rejected_on        DATETIME     NULL,
  reject_reason      VARCHAR(255) NULL,
  created_by         VARCHAR(40)  NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by         VARCHAR(40)  NULL,
  updated_at         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tour_main),
  UNIQUE KEY uq_tour_emp_month (emp_id, tour_year, tour_month),
  KEY ix_tour_status (approval_status, tour_year, tour_month),
  KEY ix_tour_manager (manager_id, tour_year, tour_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_child (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date     DATE         NOT NULL,
  worktype_code VARCHAR(30)  NOT NULL COMMENT 'app_dcr_worktype day type, e.g. WORKING / LEAVE',
  stay_area_id  BIGINT       NULL COMMENT 'night stay area (area_master.oid), if staying',
  tour_remark   VARCHAR(250) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_day (tour_main, tour_date),
  KEY ix_tour_child_date (tour_date),
  CONSTRAINT fk_tc_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_area (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'area_master.oid planned for the day',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_area (tour_main, tour_date, area_id),
  KEY ix_tour_area_date (tour_date),
  CONSTRAINT fk_ta_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_clients (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main  BIGINT UNSIGNED NOT NULL,
  tour_date  DATE   NOT NULL,
  area_id    BIGINT NOT NULL COMMENT 'the planned area this client was picked under',
  client_id  BIGINT NOT NULL COMMENT 'client_master.id (client / retailer / party)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_client (tour_main, tour_date, client_id),
  KEY ix_tour_client_date (tour_date),
  CONSTRAINT fk_tcl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_visit_with (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main     BIGINT UNSIGNED NOT NULL,
  tour_date     DATE        NOT NULL,
  visit_with_id VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'emp_id of the senior / junior planned to work with',
  PRIMARY KEY (id),
  UNIQUE KEY uq_tour_with (tour_main, tour_date, visit_with_id),
  KEY ix_tour_with_date (tour_date),
  CONSTRAINT fk_tw_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE TABLE IF NOT EXISTS tour_emp_approval_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main    BIGINT UNSIGNED NOT NULL,
  step         VARCHAR(20)  NOT NULL COMMENT 'REQUEST / MANAGER / FINAL / AUTO / ADMIN',
  action       VARCHAR(20)  NOT NULL COMMENT 'REQUESTED / APPROVED / REJECTED',
  status_after VARCHAR(20)  NOT NULL,
  remarks      VARCHAR(255) NULL,
  by_emp_id    VARCHAR(40)  NULL,
  by_name      VARCHAR(150) NULL,
  by_designation VARCHAR(100) NULL,
  at_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_tour_log (tour_main),
  CONSTRAINT fk_tl_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---- DCR: the type (HQ / EX / OUT) of the start area, saved with the day ----
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_area_type') = 0,
 "ALTER TABLE dcr_parent_summary ADD start_area_type VARCHAR(30) NULL COMMENT 'area_master.area_type (HQ / EX / OUT ...) of the start area, copied when the day starts' AFTER start_area_id", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ---- permissions (everyone allowed by default; every existing employee also gets their own rows) ----
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
  ('TP_SAVE', 'Tour Plan', 'Plan / submit my own tour plan', 1, 1, NOW(), 'seed'),
  ('TP_APPROVE', 'Tour Plan', 'Approve / reject tour plans (manager step / admin final)', 1, 1, NOW(), 'seed'),
  ('HOLIDAY_SAVE', 'Holiday', 'Add / edit holidays and weekly off days', 1, 1, NOW(), 'seed');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code, 1, NOW(), 'migration-95'
FROM emp_detail e JOIN permission_master m ON m.permission_code IN ('TP_SAVE', 'TP_APPROVE', 'HOLIDAY_SAVE')
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

-- ---- DCR rule: only the tour plan's areas / clients are offered when TRUE ----
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL, 'TP_VALIDATE_FILTER', 'DCR follows the approved tour plan', 'DCR', 'START_DAY', 'APP', 'VALIDATION', 'BOOLEAN', 'FALSE', 'FALSE',
  'TRUE = the day can start only from an area of the approved tour plan of that date, and check-in lists only the planned clients (an area with no client picked offers all its clients).', 15);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.config_code = 'TP_VALIDATE_FILTER';

-- ---- menus: Tour Plan next to the DCR, Holiday Master under Master Entry ----
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan (month calendar, submit, approval)', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'Holiday Master', 'Holidays and weekly off', 'calendar-event', 'holidaymaster', 'master/holiday-master.html', '_SELF', 25, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'holidaymaster');
