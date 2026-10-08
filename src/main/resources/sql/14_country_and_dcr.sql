-- ===========================================================================
-- country, state.country_oid, dcr — TENANT db, acme_db ONLY (per request).
--
-- country       -> new master; states link to it via state.country_oid.
-- state         -> existing table, gets ONE new nullable FK column
--                  (idempotent ALTER, safe to re-run).
-- dcr           -> Daily Call Report entries (one row per employee+client
--                  visit/day). dcr_date is the real DATE column used for every
--                  normal (India) client; dcr_date_np is a VARCHAR column used
--                  ONLY for Nepal clients (Nepali/Bikram Sambat calendar,
--                  stored as text since it isn't a Gregorian date). Which
--                  column a report reads is decided by the client's country
--                  (client -> route -> state -> country), not stored per-row.
--
-- Also registers "Country" (Masters > Geography) and "DCR Report" (new
-- Reports top-level menu) menu items.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/14_country_and_dcr.sql
-- ===========================================================================

USE acme_db;

CREATE TABLE IF NOT EXISTS country (
    oid          BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code VARCHAR(8)   NOT NULL,
    country_name VARCHAR(64)  NOT NULL,
    status       TINYINT(1)   NOT NULL DEFAULT 1,        -- 1 = active (true), 0 = inactive (false)
    UNIQUE KEY uq_country_code (country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO country (country_code, country_name, status) VALUES
    ('IN', 'India', 1),
    ('NP', 'Nepal', 1)
ON DUPLICATE KEY UPDATE country_name = VALUES(country_name);

-- add state.country_oid if this table pre-dates the column (idempotent, like
-- the menu_item.title pattern in 09_menu.sql)
SET @has_country_oid := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'state' AND column_name = 'country_oid');
SET @ddl := IF(@has_country_oid = 0,
    'ALTER TABLE state ADD COLUMN country_oid BIGINT NULL AFTER state_name', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- default every existing state to India; admin can move any to Nepal via the Country field later
UPDATE state SET country_oid = (SELECT oid FROM country WHERE country_code = 'IN')
WHERE country_oid IS NULL;

CREATE TABLE IF NOT EXISTS dcr (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_oid  BIGINT       NOT NULL,
    client_oid    BIGINT       NOT NULL,
    dcr_date      DATE         NULL,             -- normal (India) clients
    dcr_date_np   VARCHAR(20)  NULL,             -- Nepal clients only (Bikram Sambat text)
    work_type     VARCHAR(32)  NULL,             -- Working / Leave / Holiday
    remarks       VARCHAR(255) NULL,
    time_in       TIME         NULL,
    time_out      TIME         NULL,
    status        TINYINT(1)   NOT NULL DEFAULT 1,        -- 1 = active (true), 0 = soft-deleted (false)
    created_at    DATETIME     NULL,
    created_by    VARCHAR(128) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    KEY idx_dcr_employee (employee_oid),
    KEY idx_dcr_client (client_oid),
    KEY idx_dcr_date (dcr_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- a few sample rows so the report has something to show, using whatever
-- employees/clients already exist in this tenant (safe if either is empty)
INSERT INTO dcr (employee_oid, client_oid, dcr_date, work_type, remarks, time_in, time_out, status, created_at, created_by, updated_at, updated_by)
SELECT e.oid, c.oid, CURDATE(), 'Working', 'Sample seed visit', '09:30:00', '18:00:00', 1, NOW(), 'system', NOW(), 'system'
FROM employee e JOIN client c ON 1=1
LIMIT 5;

-- "Country" under Masters > Geography (10), before Area (10)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (103, 10, 'Country', 'Country Master', 'flag', 'country', 'master/country.html', 5, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- new top-level "Reports" parent + "DCR Report" child
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (4, NULL, 'Reports', NULL, 'bar-chart-line', NULL, NULL, 25, NULL, 1),
    (400, 4, 'DCR Report', 'DCR Report', 'clipboard-data', 'dcrreport', 'report/dcr-report.html', 10, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);
