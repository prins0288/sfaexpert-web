-- ===========================================================================
-- 84_dcr_v2.sql — PER TENANT database
--
-- DCR (Daily Call Report) rebuilt for 2026. The 2012 tables keep their names but
-- get a clean structure: real FKs to the masters, DATE / TIME / DECIMAL types,
-- utf8mb4, only columns the DCR form really uses, one UNIQUE key per table so a
-- re-submit can never double an entry.
--
--   dcr_parent_summary          one row per employee per day  (UNIQUE emp_id + dcr_date)
--   dcr_client_summary          each listed client visited (client_master) + POB
--   dcr_unlisted_client_summary each visit to a client NOT in client_master
--   dcr_client_sample           items given (sample / gift / input / POB) to listed OR unlisted
--   dcr_leaflets_summary        leaflets given to listed OR unlisted
--   dcr_area_summary            the unique areas of the day (start, visited, end, stay)
--   dcr_expense                 daily + travel expense in ONE table (client optional)
--   dcr_sm_visits               joint working (manager with MR / MR with manager)
--   dcr_client_treatment        special treatments noted at a client
--   app_dcr_worktype            which DCR tabs (Client / Retailer / Party / Unlisted /
--                               Expense / Joint / Day End ...) each user gets:
--                               for ALL, an employee, a designation or a hierarchy level
--   expense_master, treatment_master   the two masters the DCR needs
-- "client_kind" is 1 = listed (client_master.id) or 2 = unlisted
-- (dcr_unlisted_client_summary.id); 0 = not about a client.
--
-- The old `dcr` table (one row per visit) is DROPPED.
-- Needs: emp_detail, designation_master, area_master, client_master, client_type,
-- speciality_master, category_master, item_master (migrations 61-83).
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/84_dcr_v2.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

DROP TABLE IF EXISTS dcr;

CREATE TABLE IF NOT EXISTS expense_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  expense_code  VARCHAR(30)  NOT NULL,
  expense_name  VARCHAR(80)  NOT NULL,
  is_travel     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = asks from / to area and km',
  sort_order    SMALLINT     NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_expense_code (expense_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order) VALUES
  ('TRAVEL', 'Travel', 1, 1), ('FOOD', 'Food', 0, 2), ('STAY', 'Stay / Hotel', 0, 3), ('PARKING', 'Parking / Toll', 0, 4),
  ('GIFT', 'Gift to client', 0, 5), ('COURIER', 'Courier / Postage', 0, 6), ('OTHER', 'Other', 0, 9);

CREATE TABLE IF NOT EXISTS treatment_master (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  treatment_type  VARCHAR(40)  NOT NULL COMMENT 'group, e.g. Therapy / Procedure',
  treatment_name  VARCHAR(100) NOT NULL,
  sort_order      SMALLINT     NOT NULL DEFAULT 0,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_treatment (treatment_type, treatment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Which tabs a user sees in the DCR form. One row = one tab for a target; the most
-- specific row wins per worktype_code: EMP_ID > DESIGNATION > HIERARCHY_LEVEL > ALL.
-- is_enabled = 0 on a more specific row switches the tab OFF for that target.
CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  target_type    VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL / EMP_ID / DESIGNATION / HIERARCHY_LEVEL',
  target_value   VARCHAR(40)  NOT NULL DEFAULT ''    COMMENT 'emp_id / designation id / hierarchy level (empty for ALL)',
  worktype_code  VARCHAR(30)  NOT NULL               COMMENT 'CLIENT, RETAILER, PARTY, UNLISTED, JOINT, EXPENSE, DAYEND ...',
  display_name   VARCHAR(60)  NOT NULL               COMMENT 'tab title shown to the user',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'form it opens: CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND',
  client_type_id BIGINT       NULL                   COMMENT 'CHECKIN only: just this client type (NULL = the other types)',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT ''    COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0,
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype_target (target_type, target_value, worktype_code),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO app_dcr_worktype (target_type, target_value, worktype_code, display_name, form_open, sub_forms, sort_order, is_enabled) VALUES
  ('ALL', '', 'CLIENT',   'Client Check-in',   'CHECKIN',  'ITEMS,LEAFLETS,TREATMENT', 10, 1),
  ('ALL', '', 'RETAILER', 'Retailer Check-in', 'CHECKIN',  'ITEMS',                    20, 0),
  ('ALL', '', 'PARTY',    'Party Check-in',    'CHECKIN',  'ITEMS',                    30, 0),
  ('ALL', '', 'UNLISTED', 'Unlisted Check-in', 'UNLISTED', 'ITEMS,LEAFLETS',           40, 1),
  ('ALL', '', 'JOINT',    'Joint Working',     'JOINT',    '',                         50, 1),
  ('ALL', '', 'EXPENSE',  'Daily Expense',     'EXPENSE',  '',                         60, 1),
  ('ALL', '', 'DAYEND',   'Day End',           'DAYEND',   '',                         70, 1);

CREATE TABLE IF NOT EXISTS dcr_parent_summary (
  dcr_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id            VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date          DATE         NOT NULL,
  work_type         VARCHAR(20)  NOT NULL DEFAULT 'WORKING' COMMENT 'WORKING / LEAVE / HOLIDAY / MEETING / TRAINING',
  designation_id    INT          NULL     COMMENT 'designation on that day (promotion-proof)',
  manager_id        VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager on that day',
  start_area_id     BIGINT       NULL     COMMENT 'where the day started',
  working_area_id   BIGINT       NULL,
  time_in           TIME         NULL,
  time_out          TIME         NULL,
  travel_mode       VARCHAR(30)  NULL,
  meter_start       DECIMAL(9,1) NULL,
  meter_end         DECIMAL(9,1) NULL,
  remarks           TEXT         NULL,
  day_end_area_id   BIGINT       NULL,
  day_end_at        DATETIME     NULL,
  day_end_remarks   VARCHAR(200) NULL,
  stay_area_id      BIGINT       NULL,
  stay_hotel        VARCHAR(100) NULL,
  listed_calls      SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'kept in step on save: fast lists, no counting',
  unlisted_calls    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  pob_total         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  expense_total     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0 draft, 1 submitted, 2 approved, 3 rejected',
  submitted_at      DATETIME     NULL,
  submitted_through TINYINT      NOT NULL DEFAULT 2 COMMENT '1 app, 2 web',
  approved_by       VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at       DATETIME     NULL,
  approval_remarks  VARCHAR(150) NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (dcr_id),
  UNIQUE KEY uq_dcr_emp_date (emp_id, dcr_date),
  KEY ix_dcr_date (dcr_date),
  KEY ix_dcr_manager_date (manager_id, dcr_date),
  KEY ix_dcr_status_date (status, dcr_date),
  CONSTRAINT fk_dcr_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_des FOREIGN KEY (designation_id) REFERENCES designation_master (id),
  CONSTRAINT fk_dcr_area_start FOREIGN KEY (start_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_work FOREIGN KEY (working_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_end FOREIGN KEY (day_end_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_stay FOREIGN KEY (stay_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'copy of the parent, for per-employee queries',
  client_id           BIGINT       NOT NULL COMMENT '-> client_master.id',
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'CLIENT' COMMENT 'the tab it was entered under',
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  area_id             BIGINT       NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'client refused to meet',
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_client (dcr_id, client_id, visit_type),
  KEY ix_dcr_client_client (client_id, dcr_id),
  KEY ix_dcr_client_emp (emp_id, dcr_id),
  CONSTRAINT fk_dcs_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcs_client FOREIGN KEY (client_id) REFERENCES client_master (id),
  CONSTRAINT fk_dcs_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_unlisted_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'UNLISTED',
  client_name         VARCHAR(120) NOT NULL,
  client_type_id      BIGINT       NULL,
  speciality_id       BIGINT       NULL,
  category_id         BIGINT       NULL,
  qualification       VARCHAR(100) NULL,
  mobile              VARCHAR(15)  NOT NULL DEFAULT '',
  contact_person      VARCHAR(100) NULL,
  address             VARCHAR(300) NULL,
  area_id             BIGINT       NULL,
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0,
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  approval_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 pending, 1 added to client master, 2 rejected',
  approved_by         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_unlisted (dcr_id, client_name, mobile),
  KEY ix_dcr_unlisted_emp (emp_id, dcr_id),
  KEY ix_dcr_unlisted_approval (approval_status),
  CONSTRAINT fk_dcu_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcu_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid),
  CONSTRAINT fk_dcu_spec FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid),
  CONSTRAINT fk_dcu_cat FOREIGN KEY (category_id) REFERENCES category_master (oid),
  CONSTRAINT fk_dcu_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_sample (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL COMMENT 'client_master.id or dcr_unlisted_client_summary.id',
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id',
  item_type      VARCHAR(20)  NOT NULL COMMENT 'PRODUCT / SAMPLE / GIFT / INPUT (as on the item)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  pob_qty        INT UNSIGNED NOT NULL DEFAULT 0,
  rate           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  amount         DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  is_detailed    TINYINT(1)   NOT NULL DEFAULT 0,
  remarks        VARCHAR(200) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_sample (dcr_id, client_kind, client_ref_id, item_id, item_type),
  KEY ix_dcr_sample_item (item_id, dcr_id),
  CONSTRAINT fk_dsm_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsm_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_leaflets_summary (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id (the leaflet)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  value          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_leaflet (dcr_id, client_kind, client_ref_id, item_id),
  KEY ix_dcr_leaflet_item (item_id),
  CONSTRAINT fk_dlf_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dlf_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_area_summary (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id     BIGINT UNSIGNED NOT NULL,
  emp_id     VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date   DATE         NOT NULL COMMENT 'copy of the parent: who worked in an area on which day, without a join',
  area_id    BIGINT       NOT NULL,
  area_role  VARCHAR(10)  NOT NULL COMMENT 'START / VISITED / END / STAY',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_area (dcr_id, area_id, area_role),
  KEY ix_dcr_area_area (area_id, dcr_date),
  KEY ix_dcr_area_emp (emp_id, dcr_date),
  CONSTRAINT fk_dca_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dca_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_expense (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  expense_id     INT UNSIGNED NOT NULL COMMENT '-> expense_master.id',
  amount         DECIMAL(12,2) NOT NULL,
  description    VARCHAR(150) NULL,
  client_kind    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 none, 1 listed, 2 unlisted (e.g. a gift to a client)',
  client_ref_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  from_area_id   BIGINT       NULL COMMENT 'travel only',
  to_area_id     BIGINT       NULL,
  start_km       DECIMAL(9,1) NULL,
  end_km         DECIMAL(9,1) NULL,
  total_km       DECIMAL(9,1) GENERATED ALWAYS AS (IF(start_km IS NULL OR end_km IS NULL, NULL, end_km - start_km)) VIRTUAL,
  start_image_path VARCHAR(255) NULL COMMENT 'odometer photo (a stored file, not base64)',
  end_image_path   VARCHAR(255) NULL,
  file_name      VARCHAR(255) NULL COMMENT 'bill / receipt',
  file_type      VARCHAR(100) NULL,
  file_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_dcr_expense_dcr (dcr_id),
  KEY ix_dcr_expense_type (expense_id, dcr_id),
  CONSTRAINT fk_dex_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dex_type FOREIGN KEY (expense_id) REFERENCES expense_master (id),
  CONSTRAINT fk_dex_from FOREIGN KEY (from_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dex_to FOREIGN KEY (to_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_sm_visits (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  with_emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'who worked together with the DCR owner',
  with_designation_id INT          NULL COMMENT 'his designation on that day',
  client_kind         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 = the whole day, 1 listed, 2 unlisted',
  client_ref_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_joint (dcr_id, with_emp_id, client_kind, client_ref_id),
  KEY ix_dcr_joint_with (with_emp_id, dcr_id),
  CONSTRAINT fk_dsv_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsv_emp FOREIGN KEY (with_emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dsv_des FOREIGN KEY (with_designation_id) REFERENCES designation_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_treatment (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  treatment_id   INT UNSIGNED NOT NULL COMMENT '-> treatment_master.id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_treatment (dcr_id, client_kind, client_ref_id, treatment_id),
  KEY ix_dcr_treatment_t (treatment_id),
  CONSTRAINT fk_dct_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dct_t FOREIGN KEY (treatment_id) REFERENCES treatment_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
  ('DCR_SAVE', 'DCR', 'Fill / submit my own DCR', 1, NOW(), 'seed'),
  ('DCR_APPROVE', 'DCR', 'Approve / reject the DCR of my team (immediate manager, or admin)', 1, NOW(), 'seed');

-- menus (added only when the parent menu exists in this company)
SET @sales := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                       (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @sales, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sales LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @sales IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
SET @entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs (who gets which tab)', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');

-- ==================== globex_db ====================
USE globex_db;

DROP TABLE IF EXISTS dcr;

CREATE TABLE IF NOT EXISTS expense_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  expense_code  VARCHAR(30)  NOT NULL,
  expense_name  VARCHAR(80)  NOT NULL,
  is_travel     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = asks from / to area and km',
  sort_order    SMALLINT     NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_expense_code (expense_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order) VALUES
  ('TRAVEL', 'Travel', 1, 1), ('FOOD', 'Food', 0, 2), ('STAY', 'Stay / Hotel', 0, 3), ('PARKING', 'Parking / Toll', 0, 4),
  ('GIFT', 'Gift to client', 0, 5), ('COURIER', 'Courier / Postage', 0, 6), ('OTHER', 'Other', 0, 9);

CREATE TABLE IF NOT EXISTS treatment_master (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  treatment_type  VARCHAR(40)  NOT NULL COMMENT 'group, e.g. Therapy / Procedure',
  treatment_name  VARCHAR(100) NOT NULL,
  sort_order      SMALLINT     NOT NULL DEFAULT 0,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_treatment (treatment_type, treatment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Which tabs a user sees in the DCR form. One row = one tab for a target; the most
-- specific row wins per worktype_code: EMP_ID > DESIGNATION > HIERARCHY_LEVEL > ALL.
-- is_enabled = 0 on a more specific row switches the tab OFF for that target.
CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  target_type    VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL / EMP_ID / DESIGNATION / HIERARCHY_LEVEL',
  target_value   VARCHAR(40)  NOT NULL DEFAULT ''    COMMENT 'emp_id / designation id / hierarchy level (empty for ALL)',
  worktype_code  VARCHAR(30)  NOT NULL               COMMENT 'CLIENT, RETAILER, PARTY, UNLISTED, JOINT, EXPENSE, DAYEND ...',
  display_name   VARCHAR(60)  NOT NULL               COMMENT 'tab title shown to the user',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'form it opens: CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND',
  client_type_id BIGINT       NULL                   COMMENT 'CHECKIN only: just this client type (NULL = the other types)',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT ''    COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0,
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype_target (target_type, target_value, worktype_code),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO app_dcr_worktype (target_type, target_value, worktype_code, display_name, form_open, sub_forms, sort_order, is_enabled) VALUES
  ('ALL', '', 'CLIENT',   'Client Check-in',   'CHECKIN',  'ITEMS,LEAFLETS,TREATMENT', 10, 1),
  ('ALL', '', 'RETAILER', 'Retailer Check-in', 'CHECKIN',  'ITEMS',                    20, 0),
  ('ALL', '', 'PARTY',    'Party Check-in',    'CHECKIN',  'ITEMS',                    30, 0),
  ('ALL', '', 'UNLISTED', 'Unlisted Check-in', 'UNLISTED', 'ITEMS,LEAFLETS',           40, 1),
  ('ALL', '', 'JOINT',    'Joint Working',     'JOINT',    '',                         50, 1),
  ('ALL', '', 'EXPENSE',  'Daily Expense',     'EXPENSE',  '',                         60, 1),
  ('ALL', '', 'DAYEND',   'Day End',           'DAYEND',   '',                         70, 1);

CREATE TABLE IF NOT EXISTS dcr_parent_summary (
  dcr_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id            VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date          DATE         NOT NULL,
  work_type         VARCHAR(20)  NOT NULL DEFAULT 'WORKING' COMMENT 'WORKING / LEAVE / HOLIDAY / MEETING / TRAINING',
  designation_id    INT          NULL     COMMENT 'designation on that day (promotion-proof)',
  manager_id        VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager on that day',
  start_area_id     BIGINT       NULL     COMMENT 'where the day started',
  working_area_id   BIGINT       NULL,
  time_in           TIME         NULL,
  time_out          TIME         NULL,
  travel_mode       VARCHAR(30)  NULL,
  meter_start       DECIMAL(9,1) NULL,
  meter_end         DECIMAL(9,1) NULL,
  remarks           TEXT         NULL,
  day_end_area_id   BIGINT       NULL,
  day_end_at        DATETIME     NULL,
  day_end_remarks   VARCHAR(200) NULL,
  stay_area_id      BIGINT       NULL,
  stay_hotel        VARCHAR(100) NULL,
  listed_calls      SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'kept in step on save: fast lists, no counting',
  unlisted_calls    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  pob_total         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  expense_total     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0 draft, 1 submitted, 2 approved, 3 rejected',
  submitted_at      DATETIME     NULL,
  submitted_through TINYINT      NOT NULL DEFAULT 2 COMMENT '1 app, 2 web',
  approved_by       VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at       DATETIME     NULL,
  approval_remarks  VARCHAR(150) NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (dcr_id),
  UNIQUE KEY uq_dcr_emp_date (emp_id, dcr_date),
  KEY ix_dcr_date (dcr_date),
  KEY ix_dcr_manager_date (manager_id, dcr_date),
  KEY ix_dcr_status_date (status, dcr_date),
  CONSTRAINT fk_dcr_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_des FOREIGN KEY (designation_id) REFERENCES designation_master (id),
  CONSTRAINT fk_dcr_area_start FOREIGN KEY (start_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_work FOREIGN KEY (working_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_end FOREIGN KEY (day_end_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_stay FOREIGN KEY (stay_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'copy of the parent, for per-employee queries',
  client_id           BIGINT       NOT NULL COMMENT '-> client_master.id',
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'CLIENT' COMMENT 'the tab it was entered under',
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  area_id             BIGINT       NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'client refused to meet',
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_client (dcr_id, client_id, visit_type),
  KEY ix_dcr_client_client (client_id, dcr_id),
  KEY ix_dcr_client_emp (emp_id, dcr_id),
  CONSTRAINT fk_dcs_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcs_client FOREIGN KEY (client_id) REFERENCES client_master (id),
  CONSTRAINT fk_dcs_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_unlisted_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'UNLISTED',
  client_name         VARCHAR(120) NOT NULL,
  client_type_id      BIGINT       NULL,
  speciality_id       BIGINT       NULL,
  category_id         BIGINT       NULL,
  qualification       VARCHAR(100) NULL,
  mobile              VARCHAR(15)  NOT NULL DEFAULT '',
  contact_person      VARCHAR(100) NULL,
  address             VARCHAR(300) NULL,
  area_id             BIGINT       NULL,
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0,
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  approval_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 pending, 1 added to client master, 2 rejected',
  approved_by         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_unlisted (dcr_id, client_name, mobile),
  KEY ix_dcr_unlisted_emp (emp_id, dcr_id),
  KEY ix_dcr_unlisted_approval (approval_status),
  CONSTRAINT fk_dcu_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcu_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid),
  CONSTRAINT fk_dcu_spec FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid),
  CONSTRAINT fk_dcu_cat FOREIGN KEY (category_id) REFERENCES category_master (oid),
  CONSTRAINT fk_dcu_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_sample (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL COMMENT 'client_master.id or dcr_unlisted_client_summary.id',
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id',
  item_type      VARCHAR(20)  NOT NULL COMMENT 'PRODUCT / SAMPLE / GIFT / INPUT (as on the item)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  pob_qty        INT UNSIGNED NOT NULL DEFAULT 0,
  rate           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  amount         DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  is_detailed    TINYINT(1)   NOT NULL DEFAULT 0,
  remarks        VARCHAR(200) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_sample (dcr_id, client_kind, client_ref_id, item_id, item_type),
  KEY ix_dcr_sample_item (item_id, dcr_id),
  CONSTRAINT fk_dsm_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsm_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_leaflets_summary (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id (the leaflet)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  value          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_leaflet (dcr_id, client_kind, client_ref_id, item_id),
  KEY ix_dcr_leaflet_item (item_id),
  CONSTRAINT fk_dlf_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dlf_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_area_summary (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id     BIGINT UNSIGNED NOT NULL,
  emp_id     VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date   DATE         NOT NULL COMMENT 'copy of the parent: who worked in an area on which day, without a join',
  area_id    BIGINT       NOT NULL,
  area_role  VARCHAR(10)  NOT NULL COMMENT 'START / VISITED / END / STAY',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_area (dcr_id, area_id, area_role),
  KEY ix_dcr_area_area (area_id, dcr_date),
  KEY ix_dcr_area_emp (emp_id, dcr_date),
  CONSTRAINT fk_dca_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dca_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_expense (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  expense_id     INT UNSIGNED NOT NULL COMMENT '-> expense_master.id',
  amount         DECIMAL(12,2) NOT NULL,
  description    VARCHAR(150) NULL,
  client_kind    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 none, 1 listed, 2 unlisted (e.g. a gift to a client)',
  client_ref_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  from_area_id   BIGINT       NULL COMMENT 'travel only',
  to_area_id     BIGINT       NULL,
  start_km       DECIMAL(9,1) NULL,
  end_km         DECIMAL(9,1) NULL,
  total_km       DECIMAL(9,1) GENERATED ALWAYS AS (IF(start_km IS NULL OR end_km IS NULL, NULL, end_km - start_km)) VIRTUAL,
  start_image_path VARCHAR(255) NULL COMMENT 'odometer photo (a stored file, not base64)',
  end_image_path   VARCHAR(255) NULL,
  file_name      VARCHAR(255) NULL COMMENT 'bill / receipt',
  file_type      VARCHAR(100) NULL,
  file_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_dcr_expense_dcr (dcr_id),
  KEY ix_dcr_expense_type (expense_id, dcr_id),
  CONSTRAINT fk_dex_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dex_type FOREIGN KEY (expense_id) REFERENCES expense_master (id),
  CONSTRAINT fk_dex_from FOREIGN KEY (from_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dex_to FOREIGN KEY (to_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_sm_visits (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  with_emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'who worked together with the DCR owner',
  with_designation_id INT          NULL COMMENT 'his designation on that day',
  client_kind         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 = the whole day, 1 listed, 2 unlisted',
  client_ref_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_joint (dcr_id, with_emp_id, client_kind, client_ref_id),
  KEY ix_dcr_joint_with (with_emp_id, dcr_id),
  CONSTRAINT fk_dsv_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsv_emp FOREIGN KEY (with_emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dsv_des FOREIGN KEY (with_designation_id) REFERENCES designation_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_treatment (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  treatment_id   INT UNSIGNED NOT NULL COMMENT '-> treatment_master.id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_treatment (dcr_id, client_kind, client_ref_id, treatment_id),
  KEY ix_dcr_treatment_t (treatment_id),
  CONSTRAINT fk_dct_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dct_t FOREIGN KEY (treatment_id) REFERENCES treatment_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
  ('DCR_SAVE', 'DCR', 'Fill / submit my own DCR', 1, NOW(), 'seed'),
  ('DCR_APPROVE', 'DCR', 'Approve / reject the DCR of my team (immediate manager, or admin)', 1, NOW(), 'seed');

-- menus (added only when the parent menu exists in this company)
SET @sales := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                       (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @sales, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sales LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @sales IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
SET @entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs (who gets which tab)', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');

-- ==================== initech_db ====================
USE initech_db;

DROP TABLE IF EXISTS dcr;

CREATE TABLE IF NOT EXISTS expense_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  expense_code  VARCHAR(30)  NOT NULL,
  expense_name  VARCHAR(80)  NOT NULL,
  is_travel     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = asks from / to area and km',
  sort_order    SMALLINT     NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_expense_code (expense_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order) VALUES
  ('TRAVEL', 'Travel', 1, 1), ('FOOD', 'Food', 0, 2), ('STAY', 'Stay / Hotel', 0, 3), ('PARKING', 'Parking / Toll', 0, 4),
  ('GIFT', 'Gift to client', 0, 5), ('COURIER', 'Courier / Postage', 0, 6), ('OTHER', 'Other', 0, 9);

CREATE TABLE IF NOT EXISTS treatment_master (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  treatment_type  VARCHAR(40)  NOT NULL COMMENT 'group, e.g. Therapy / Procedure',
  treatment_name  VARCHAR(100) NOT NULL,
  sort_order      SMALLINT     NOT NULL DEFAULT 0,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_treatment (treatment_type, treatment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Which tabs a user sees in the DCR form. One row = one tab for a target; the most
-- specific row wins per worktype_code: EMP_ID > DESIGNATION > HIERARCHY_LEVEL > ALL.
-- is_enabled = 0 on a more specific row switches the tab OFF for that target.
CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  target_type    VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL / EMP_ID / DESIGNATION / HIERARCHY_LEVEL',
  target_value   VARCHAR(40)  NOT NULL DEFAULT ''    COMMENT 'emp_id / designation id / hierarchy level (empty for ALL)',
  worktype_code  VARCHAR(30)  NOT NULL               COMMENT 'CLIENT, RETAILER, PARTY, UNLISTED, JOINT, EXPENSE, DAYEND ...',
  display_name   VARCHAR(60)  NOT NULL               COMMENT 'tab title shown to the user',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'form it opens: CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND',
  client_type_id BIGINT       NULL                   COMMENT 'CHECKIN only: just this client type (NULL = the other types)',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT ''    COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0,
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype_target (target_type, target_value, worktype_code),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO app_dcr_worktype (target_type, target_value, worktype_code, display_name, form_open, sub_forms, sort_order, is_enabled) VALUES
  ('ALL', '', 'CLIENT',   'Client Check-in',   'CHECKIN',  'ITEMS,LEAFLETS,TREATMENT', 10, 1),
  ('ALL', '', 'RETAILER', 'Retailer Check-in', 'CHECKIN',  'ITEMS',                    20, 0),
  ('ALL', '', 'PARTY',    'Party Check-in',    'CHECKIN',  'ITEMS',                    30, 0),
  ('ALL', '', 'UNLISTED', 'Unlisted Check-in', 'UNLISTED', 'ITEMS,LEAFLETS',           40, 1),
  ('ALL', '', 'JOINT',    'Joint Working',     'JOINT',    '',                         50, 1),
  ('ALL', '', 'EXPENSE',  'Daily Expense',     'EXPENSE',  '',                         60, 1),
  ('ALL', '', 'DAYEND',   'Day End',           'DAYEND',   '',                         70, 1);

CREATE TABLE IF NOT EXISTS dcr_parent_summary (
  dcr_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id            VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date          DATE         NOT NULL,
  work_type         VARCHAR(20)  NOT NULL DEFAULT 'WORKING' COMMENT 'WORKING / LEAVE / HOLIDAY / MEETING / TRAINING',
  designation_id    INT          NULL     COMMENT 'designation on that day (promotion-proof)',
  manager_id        VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager on that day',
  start_area_id     BIGINT       NULL     COMMENT 'where the day started',
  working_area_id   BIGINT       NULL,
  time_in           TIME         NULL,
  time_out          TIME         NULL,
  travel_mode       VARCHAR(30)  NULL,
  meter_start       DECIMAL(9,1) NULL,
  meter_end         DECIMAL(9,1) NULL,
  remarks           TEXT         NULL,
  day_end_area_id   BIGINT       NULL,
  day_end_at        DATETIME     NULL,
  day_end_remarks   VARCHAR(200) NULL,
  stay_area_id      BIGINT       NULL,
  stay_hotel        VARCHAR(100) NULL,
  listed_calls      SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'kept in step on save: fast lists, no counting',
  unlisted_calls    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  pob_total         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  expense_total     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0 draft, 1 submitted, 2 approved, 3 rejected',
  submitted_at      DATETIME     NULL,
  submitted_through TINYINT      NOT NULL DEFAULT 2 COMMENT '1 app, 2 web',
  approved_by       VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at       DATETIME     NULL,
  approval_remarks  VARCHAR(150) NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (dcr_id),
  UNIQUE KEY uq_dcr_emp_date (emp_id, dcr_date),
  KEY ix_dcr_date (dcr_date),
  KEY ix_dcr_manager_date (manager_id, dcr_date),
  KEY ix_dcr_status_date (status, dcr_date),
  CONSTRAINT fk_dcr_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_des FOREIGN KEY (designation_id) REFERENCES designation_master (id),
  CONSTRAINT fk_dcr_area_start FOREIGN KEY (start_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_work FOREIGN KEY (working_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_end FOREIGN KEY (day_end_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_stay FOREIGN KEY (stay_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'copy of the parent, for per-employee queries',
  client_id           BIGINT       NOT NULL COMMENT '-> client_master.id',
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'CLIENT' COMMENT 'the tab it was entered under',
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  area_id             BIGINT       NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'client refused to meet',
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_client (dcr_id, client_id, visit_type),
  KEY ix_dcr_client_client (client_id, dcr_id),
  KEY ix_dcr_client_emp (emp_id, dcr_id),
  CONSTRAINT fk_dcs_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcs_client FOREIGN KEY (client_id) REFERENCES client_master (id),
  CONSTRAINT fk_dcs_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_unlisted_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'UNLISTED',
  client_name         VARCHAR(120) NOT NULL,
  client_type_id      BIGINT       NULL,
  speciality_id       BIGINT       NULL,
  category_id         BIGINT       NULL,
  qualification       VARCHAR(100) NULL,
  mobile              VARCHAR(15)  NOT NULL DEFAULT '',
  contact_person      VARCHAR(100) NULL,
  address             VARCHAR(300) NULL,
  area_id             BIGINT       NULL,
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0,
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  approval_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 pending, 1 added to client master, 2 rejected',
  approved_by         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_unlisted (dcr_id, client_name, mobile),
  KEY ix_dcr_unlisted_emp (emp_id, dcr_id),
  KEY ix_dcr_unlisted_approval (approval_status),
  CONSTRAINT fk_dcu_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcu_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid),
  CONSTRAINT fk_dcu_spec FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid),
  CONSTRAINT fk_dcu_cat FOREIGN KEY (category_id) REFERENCES category_master (oid),
  CONSTRAINT fk_dcu_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_sample (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL COMMENT 'client_master.id or dcr_unlisted_client_summary.id',
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id',
  item_type      VARCHAR(20)  NOT NULL COMMENT 'PRODUCT / SAMPLE / GIFT / INPUT (as on the item)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  pob_qty        INT UNSIGNED NOT NULL DEFAULT 0,
  rate           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  amount         DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  is_detailed    TINYINT(1)   NOT NULL DEFAULT 0,
  remarks        VARCHAR(200) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_sample (dcr_id, client_kind, client_ref_id, item_id, item_type),
  KEY ix_dcr_sample_item (item_id, dcr_id),
  CONSTRAINT fk_dsm_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsm_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_leaflets_summary (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id (the leaflet)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  value          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_leaflet (dcr_id, client_kind, client_ref_id, item_id),
  KEY ix_dcr_leaflet_item (item_id),
  CONSTRAINT fk_dlf_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dlf_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_area_summary (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id     BIGINT UNSIGNED NOT NULL,
  emp_id     VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date   DATE         NOT NULL COMMENT 'copy of the parent: who worked in an area on which day, without a join',
  area_id    BIGINT       NOT NULL,
  area_role  VARCHAR(10)  NOT NULL COMMENT 'START / VISITED / END / STAY',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_area (dcr_id, area_id, area_role),
  KEY ix_dcr_area_area (area_id, dcr_date),
  KEY ix_dcr_area_emp (emp_id, dcr_date),
  CONSTRAINT fk_dca_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dca_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_expense (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  expense_id     INT UNSIGNED NOT NULL COMMENT '-> expense_master.id',
  amount         DECIMAL(12,2) NOT NULL,
  description    VARCHAR(150) NULL,
  client_kind    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 none, 1 listed, 2 unlisted (e.g. a gift to a client)',
  client_ref_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  from_area_id   BIGINT       NULL COMMENT 'travel only',
  to_area_id     BIGINT       NULL,
  start_km       DECIMAL(9,1) NULL,
  end_km         DECIMAL(9,1) NULL,
  total_km       DECIMAL(9,1) GENERATED ALWAYS AS (IF(start_km IS NULL OR end_km IS NULL, NULL, end_km - start_km)) VIRTUAL,
  start_image_path VARCHAR(255) NULL COMMENT 'odometer photo (a stored file, not base64)',
  end_image_path   VARCHAR(255) NULL,
  file_name      VARCHAR(255) NULL COMMENT 'bill / receipt',
  file_type      VARCHAR(100) NULL,
  file_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_dcr_expense_dcr (dcr_id),
  KEY ix_dcr_expense_type (expense_id, dcr_id),
  CONSTRAINT fk_dex_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dex_type FOREIGN KEY (expense_id) REFERENCES expense_master (id),
  CONSTRAINT fk_dex_from FOREIGN KEY (from_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dex_to FOREIGN KEY (to_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_sm_visits (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  with_emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'who worked together with the DCR owner',
  with_designation_id INT          NULL COMMENT 'his designation on that day',
  client_kind         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 = the whole day, 1 listed, 2 unlisted',
  client_ref_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_joint (dcr_id, with_emp_id, client_kind, client_ref_id),
  KEY ix_dcr_joint_with (with_emp_id, dcr_id),
  CONSTRAINT fk_dsv_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsv_emp FOREIGN KEY (with_emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dsv_des FOREIGN KEY (with_designation_id) REFERENCES designation_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_treatment (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  treatment_id   INT UNSIGNED NOT NULL COMMENT '-> treatment_master.id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_treatment (dcr_id, client_kind, client_ref_id, treatment_id),
  KEY ix_dcr_treatment_t (treatment_id),
  CONSTRAINT fk_dct_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dct_t FOREIGN KEY (treatment_id) REFERENCES treatment_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
  ('DCR_SAVE', 'DCR', 'Fill / submit my own DCR', 1, NOW(), 'seed'),
  ('DCR_APPROVE', 'DCR', 'Approve / reject the DCR of my team (immediate manager, or admin)', 1, NOW(), 'seed');

-- menus (added only when the parent menu exists in this company)
SET @sales := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                       (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @sales, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sales LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @sales IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
SET @entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs (who gets which tab)', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');

-- ==================== sfa_demo ====================
USE sfa_demo;

DROP TABLE IF EXISTS dcr;

CREATE TABLE IF NOT EXISTS expense_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  expense_code  VARCHAR(30)  NOT NULL,
  expense_name  VARCHAR(80)  NOT NULL,
  is_travel     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = asks from / to area and km',
  sort_order    SMALLINT     NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_expense_code (expense_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order) VALUES
  ('TRAVEL', 'Travel', 1, 1), ('FOOD', 'Food', 0, 2), ('STAY', 'Stay / Hotel', 0, 3), ('PARKING', 'Parking / Toll', 0, 4),
  ('GIFT', 'Gift to client', 0, 5), ('COURIER', 'Courier / Postage', 0, 6), ('OTHER', 'Other', 0, 9);

CREATE TABLE IF NOT EXISTS treatment_master (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  treatment_type  VARCHAR(40)  NOT NULL COMMENT 'group, e.g. Therapy / Procedure',
  treatment_name  VARCHAR(100) NOT NULL,
  sort_order      SMALLINT     NOT NULL DEFAULT 0,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_treatment (treatment_type, treatment_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Which tabs a user sees in the DCR form. One row = one tab for a target; the most
-- specific row wins per worktype_code: EMP_ID > DESIGNATION > HIERARCHY_LEVEL > ALL.
-- is_enabled = 0 on a more specific row switches the tab OFF for that target.
CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  target_type    VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL / EMP_ID / DESIGNATION / HIERARCHY_LEVEL',
  target_value   VARCHAR(40)  NOT NULL DEFAULT ''    COMMENT 'emp_id / designation id / hierarchy level (empty for ALL)',
  worktype_code  VARCHAR(30)  NOT NULL               COMMENT 'CLIENT, RETAILER, PARTY, UNLISTED, JOINT, EXPENSE, DAYEND ...',
  display_name   VARCHAR(60)  NOT NULL               COMMENT 'tab title shown to the user',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'form it opens: CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND',
  client_type_id BIGINT       NULL                   COMMENT 'CHECKIN only: just this client type (NULL = the other types)',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT ''    COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0,
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype_target (target_type, target_value, worktype_code),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO app_dcr_worktype (target_type, target_value, worktype_code, display_name, form_open, sub_forms, sort_order, is_enabled) VALUES
  ('ALL', '', 'CLIENT',   'Client Check-in',   'CHECKIN',  'ITEMS,LEAFLETS,TREATMENT', 10, 1),
  ('ALL', '', 'RETAILER', 'Retailer Check-in', 'CHECKIN',  'ITEMS',                    20, 0),
  ('ALL', '', 'PARTY',    'Party Check-in',    'CHECKIN',  'ITEMS',                    30, 0),
  ('ALL', '', 'UNLISTED', 'Unlisted Check-in', 'UNLISTED', 'ITEMS,LEAFLETS',           40, 1),
  ('ALL', '', 'JOINT',    'Joint Working',     'JOINT',    '',                         50, 1),
  ('ALL', '', 'EXPENSE',  'Daily Expense',     'EXPENSE',  '',                         60, 1),
  ('ALL', '', 'DAYEND',   'Day End',           'DAYEND',   '',                         70, 1);

CREATE TABLE IF NOT EXISTS dcr_parent_summary (
  dcr_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id            VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date          DATE         NOT NULL,
  work_type         VARCHAR(20)  NOT NULL DEFAULT 'WORKING' COMMENT 'WORKING / LEAVE / HOLIDAY / MEETING / TRAINING',
  designation_id    INT          NULL     COMMENT 'designation on that day (promotion-proof)',
  manager_id        VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'immediate manager on that day',
  start_area_id     BIGINT       NULL     COMMENT 'where the day started',
  working_area_id   BIGINT       NULL,
  time_in           TIME         NULL,
  time_out          TIME         NULL,
  travel_mode       VARCHAR(30)  NULL,
  meter_start       DECIMAL(9,1) NULL,
  meter_end         DECIMAL(9,1) NULL,
  remarks           TEXT         NULL,
  day_end_area_id   BIGINT       NULL,
  day_end_at        DATETIME     NULL,
  day_end_remarks   VARCHAR(200) NULL,
  stay_area_id      BIGINT       NULL,
  stay_hotel        VARCHAR(100) NULL,
  listed_calls      SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'kept in step on save: fast lists, no counting',
  unlisted_calls    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  pob_total         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  expense_total     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0 draft, 1 submitted, 2 approved, 3 rejected',
  submitted_at      DATETIME     NULL,
  submitted_through TINYINT      NOT NULL DEFAULT 2 COMMENT '1 app, 2 web',
  approved_by       VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at       DATETIME     NULL,
  approval_remarks  VARCHAR(150) NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (dcr_id),
  UNIQUE KEY uq_dcr_emp_date (emp_id, dcr_date),
  KEY ix_dcr_date (dcr_date),
  KEY ix_dcr_manager_date (manager_id, dcr_date),
  KEY ix_dcr_status_date (status, dcr_date),
  CONSTRAINT fk_dcr_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_mgr FOREIGN KEY (manager_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dcr_des FOREIGN KEY (designation_id) REFERENCES designation_master (id),
  CONSTRAINT fk_dcr_area_start FOREIGN KEY (start_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_work FOREIGN KEY (working_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_end FOREIGN KEY (day_end_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dcr_area_stay FOREIGN KEY (stay_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'copy of the parent, for per-employee queries',
  client_id           BIGINT       NOT NULL COMMENT '-> client_master.id',
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'CLIENT' COMMENT 'the tab it was entered under',
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  area_id             BIGINT       NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'client refused to meet',
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_client (dcr_id, client_id, visit_type),
  KEY ix_dcr_client_client (client_id, dcr_id),
  KEY ix_dcr_client_emp (emp_id, dcr_id),
  CONSTRAINT fk_dcs_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcs_client FOREIGN KEY (client_id) REFERENCES client_master (id),
  CONSTRAINT fk_dcs_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_unlisted_client_summary (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  emp_id              VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  worktype_code       VARCHAR(30)  NOT NULL DEFAULT 'UNLISTED',
  client_name         VARCHAR(120) NOT NULL,
  client_type_id      BIGINT       NULL,
  speciality_id       BIGINT       NULL,
  category_id         BIGINT       NULL,
  qualification       VARCHAR(100) NULL,
  mobile              VARCHAR(15)  NOT NULL DEFAULT '',
  contact_person      VARCHAR(100) NULL,
  address             VARCHAR(300) NULL,
  area_id             BIGINT       NULL,
  visit_type          VARCHAR(30)  NOT NULL DEFAULT 'CALL',
  activity_type       VARCHAR(60)  NULL,
  in_time             TIME         NULL,
  out_time            TIME         NULL,
  checkin_at          DATETIME     NULL,
  checkin_lat         DECIMAL(10,7) NULL,
  checkin_lng         DECIMAL(10,7) NULL,
  pob_value           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  remarks             TEXT         NULL,
  is_refused          TINYINT(1)   NOT NULL DEFAULT 0,
  next_visit          DATE         NULL,
  next_visit_priority VARCHAR(20)  NULL,
  next_visit_remarks  VARCHAR(200) NULL,
  signature_path      VARCHAR(255) NULL,
  approval_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 pending, 1 added to client master, 2 rejected',
  approved_by         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  approved_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_unlisted (dcr_id, client_name, mobile),
  KEY ix_dcr_unlisted_emp (emp_id, dcr_id),
  KEY ix_dcr_unlisted_approval (approval_status),
  CONSTRAINT fk_dcu_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dcu_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid),
  CONSTRAINT fk_dcu_spec FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid),
  CONSTRAINT fk_dcu_cat FOREIGN KEY (category_id) REFERENCES category_master (oid),
  CONSTRAINT fk_dcu_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_sample (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL COMMENT 'client_master.id or dcr_unlisted_client_summary.id',
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id',
  item_type      VARCHAR(20)  NOT NULL COMMENT 'PRODUCT / SAMPLE / GIFT / INPUT (as on the item)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  pob_qty        INT UNSIGNED NOT NULL DEFAULT 0,
  rate           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  amount         DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  is_detailed    TINYINT(1)   NOT NULL DEFAULT 0,
  remarks        VARCHAR(200) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_sample (dcr_id, client_kind, client_ref_id, item_id, item_type),
  KEY ix_dcr_sample_item (item_id, dcr_id),
  CONSTRAINT fk_dsm_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsm_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_leaflets_summary (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  item_id        BIGINT UNSIGNED NOT NULL COMMENT '-> item_master.id (the leaflet)',
  qty            INT UNSIGNED NOT NULL DEFAULT 0,
  value          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_leaflet (dcr_id, client_kind, client_ref_id, item_id),
  KEY ix_dcr_leaflet_item (item_id),
  CONSTRAINT fk_dlf_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dlf_item FOREIGN KEY (item_id) REFERENCES item_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_area_summary (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id     BIGINT UNSIGNED NOT NULL,
  emp_id     VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  dcr_date   DATE         NOT NULL COMMENT 'copy of the parent: who worked in an area on which day, without a join',
  area_id    BIGINT       NOT NULL,
  area_role  VARCHAR(10)  NOT NULL COMMENT 'START / VISITED / END / STAY',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_area (dcr_id, area_id, area_role),
  KEY ix_dcr_area_area (area_id, dcr_date),
  KEY ix_dcr_area_emp (emp_id, dcr_date),
  CONSTRAINT fk_dca_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dca_area FOREIGN KEY (area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_expense (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  expense_id     INT UNSIGNED NOT NULL COMMENT '-> expense_master.id',
  amount         DECIMAL(12,2) NOT NULL,
  description    VARCHAR(150) NULL,
  client_kind    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 none, 1 listed, 2 unlisted (e.g. a gift to a client)',
  client_ref_id  BIGINT UNSIGNED NOT NULL DEFAULT 0,
  from_area_id   BIGINT       NULL COMMENT 'travel only',
  to_area_id     BIGINT       NULL,
  start_km       DECIMAL(9,1) NULL,
  end_km         DECIMAL(9,1) NULL,
  total_km       DECIMAL(9,1) GENERATED ALWAYS AS (IF(start_km IS NULL OR end_km IS NULL, NULL, end_km - start_km)) VIRTUAL,
  start_image_path VARCHAR(255) NULL COMMENT 'odometer photo (a stored file, not base64)',
  end_image_path   VARCHAR(255) NULL,
  file_name      VARCHAR(255) NULL COMMENT 'bill / receipt',
  file_type      VARCHAR(100) NULL,
  file_path      VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_dcr_expense_dcr (dcr_id),
  KEY ix_dcr_expense_type (expense_id, dcr_id),
  CONSTRAINT fk_dex_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dex_type FOREIGN KEY (expense_id) REFERENCES expense_master (id),
  CONSTRAINT fk_dex_from FOREIGN KEY (from_area_id) REFERENCES area_master (oid),
  CONSTRAINT fk_dex_to FOREIGN KEY (to_area_id) REFERENCES area_master (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_sm_visits (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id              BIGINT UNSIGNED NOT NULL,
  with_emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'who worked together with the DCR owner',
  with_designation_id INT          NULL COMMENT 'his designation on that day',
  client_kind         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 = the whole day, 1 listed, 2 unlisted',
  client_ref_id       BIGINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_joint (dcr_id, with_emp_id, client_kind, client_ref_id),
  KEY ix_dcr_joint_with (with_emp_id, dcr_id),
  CONSTRAINT fk_dsv_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dsv_emp FOREIGN KEY (with_emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_dsv_des FOREIGN KEY (with_designation_id) REFERENCES designation_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS dcr_client_treatment (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dcr_id         BIGINT UNSIGNED NOT NULL,
  client_kind    TINYINT      NOT NULL COMMENT '1 listed, 2 unlisted',
  client_ref_id  BIGINT UNSIGNED NOT NULL,
  treatment_id   INT UNSIGNED NOT NULL COMMENT '-> treatment_master.id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_dcr_treatment (dcr_id, client_kind, client_ref_id, treatment_id),
  KEY ix_dcr_treatment_t (treatment_id),
  CONSTRAINT fk_dct_dcr FOREIGN KEY (dcr_id) REFERENCES dcr_parent_summary (dcr_id) ON DELETE CASCADE,
  CONSTRAINT fk_dct_t FOREIGN KEY (treatment_id) REFERENCES treatment_master (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
  ('DCR_SAVE', 'DCR', 'Fill / submit my own DCR', 1, NOW(), 'seed'),
  ('DCR_APPROVE', 'DCR', 'Approve / reject the DCR of my team (immediate manager, or admin)', 1, NOW(), 'seed');

-- menus (added only when the parent menu exists in this company)
SET @sales := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                       (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @sales, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sales LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @sales IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
SET @entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs (who gets which tab)', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');
