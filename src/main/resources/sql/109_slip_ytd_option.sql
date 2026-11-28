-- 109: slip design option: show the year-to-date column on the salary slip (off by default). Safe to re-run.

USE acme_db;
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_slip_config' AND column_name = 'show_ytd');
SET @s := IF(@c = 0, 'ALTER TABLE hr_slip_config ADD COLUMN show_ytd TINYINT(1) NOT NULL DEFAULT 0 AFTER show_net_in_words', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE globex_db;
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_slip_config' AND column_name = 'show_ytd');
SET @s := IF(@c = 0, 'ALTER TABLE hr_slip_config ADD COLUMN show_ytd TINYINT(1) NOT NULL DEFAULT 0 AFTER show_net_in_words', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE initech_db;
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_slip_config' AND column_name = 'show_ytd');
SET @s := IF(@c = 0, 'ALTER TABLE hr_slip_config ADD COLUMN show_ytd TINYINT(1) NOT NULL DEFAULT 0 AFTER show_net_in_words', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE sfa_demo;
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_slip_config' AND column_name = 'show_ytd');
SET @s := IF(@c = 0, 'ALTER TABLE hr_slip_config ADD COLUMN show_ytd TINYINT(1) NOT NULL DEFAULT 0 AFTER show_net_in_words', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
