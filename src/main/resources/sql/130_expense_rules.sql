-- 130: expense statement - day kind (absent / leave / ...), stay set by the manager / admin, last area visited, setting REQUIRE_ALL_DCR (the statement is submitted only when the DCR of the whole month is submitted). Safe to re-run.

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'row_kind') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN row_kind VARCHAR(10) NULL COMMENT ''WORK / ABSENT / LEAVE / OFF / NOTJOINED / TODAY / ATT: what the day was (absent = no submitted DCR)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_manual') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_manual TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'last_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN last_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('REQUIRE_ALL_DCR','Y');

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'row_kind') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN row_kind VARCHAR(10) NULL COMMENT ''WORK / ABSENT / LEAVE / OFF / NOTJOINED / TODAY / ATT: what the day was (absent = no submitted DCR)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_manual') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_manual TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'last_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN last_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('REQUIRE_ALL_DCR','Y');

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'row_kind') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN row_kind VARCHAR(10) NULL COMMENT ''WORK / ABSENT / LEAVE / OFF / NOTJOINED / TODAY / ATT: what the day was (absent = no submitted DCR)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_manual') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_manual TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'last_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN last_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('REQUIRE_ALL_DCR','Y');

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'row_kind') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN row_kind VARCHAR(10) NULL COMMENT ''WORK / ABSENT / LEAVE / OFF / NOTJOINED / TODAY / ATT: what the day was (absent = no submitted DCR)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'stay_manual') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN stay_manual TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_statement_day' AND column_name = 'last_area_id') = 0, 'ALTER TABLE expense_statement_day ADD COLUMN last_area_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('REQUIRE_ALL_DCR','Y');
