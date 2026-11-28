-- 137: mail_admin_setting.mail_mode (INTRA / EXTERNAL / BOTH) - which mail system the company uses; only the super admin changes it. Safe to re-run.

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mail_admin_setting' AND column_name = 'mail_mode') = 0, 'ALTER TABLE mail_admin_setting ADD COLUMN mail_mode VARCHAR(8) NOT NULL DEFAULT ''BOTH'' COMMENT ''set by the super admin: INTRA = only intramail, EXTERNAL = only SMTP + IMAP mail, BOTH = both (intramail in its own tab)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mail_admin_setting' AND column_name = 'mail_mode') = 0, 'ALTER TABLE mail_admin_setting ADD COLUMN mail_mode VARCHAR(8) NOT NULL DEFAULT ''BOTH'' COMMENT ''set by the super admin: INTRA = only intramail, EXTERNAL = only SMTP + IMAP mail, BOTH = both (intramail in its own tab)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mail_admin_setting' AND column_name = 'mail_mode') = 0, 'ALTER TABLE mail_admin_setting ADD COLUMN mail_mode VARCHAR(8) NOT NULL DEFAULT ''BOTH'' COMMENT ''set by the super admin: INTRA = only intramail, EXTERNAL = only SMTP + IMAP mail, BOTH = both (intramail in its own tab)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mail_admin_setting' AND column_name = 'mail_mode') = 0, 'ALTER TABLE mail_admin_setting ADD COLUMN mail_mode VARCHAR(8) NOT NULL DEFAULT ''BOTH'' COMMENT ''set by the super admin: INTRA = only intramail, EXTERNAL = only SMTP + IMAP mail, BOTH = both (intramail in its own tab)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
