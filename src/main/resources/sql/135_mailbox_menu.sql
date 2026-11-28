-- 135: menu Mail (Mailbox, Intramail, Mail Settings). Safe to re-run.

USE acme_db;
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Mail', 'Mail and intramail', 'envelope', NULL, NULL, '_SELF', 9, 1, 1
FROM DUAL WHERE @mroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @mroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mailbox', 'Read and send mail with your own mail account', 'envelope-at', 'mail', 'mail/mail.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mail');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mail Settings', 'Connect your mail account (SMTP + IMAP)', 'gear', 'mailsettings', 'mail/mail-settings.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mailsettings');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Intramail', 'Messages between the users of the company', 'building', 'intramail', 'mail/mail.html?tab=intra', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'intramail');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('mail','mailsettings','intramail');

USE globex_db;
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Mail', 'Mail and intramail', 'envelope', NULL, NULL, '_SELF', 9, 1, 1
FROM DUAL WHERE @mroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @mroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mailbox', 'Read and send mail with your own mail account', 'envelope-at', 'mail', 'mail/mail.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mail');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mail Settings', 'Connect your mail account (SMTP + IMAP)', 'gear', 'mailsettings', 'mail/mail-settings.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mailsettings');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Intramail', 'Messages between the users of the company', 'building', 'intramail', 'mail/mail.html?tab=intra', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'intramail');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('mail','mailsettings','intramail');

USE initech_db;
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Mail', 'Mail and intramail', 'envelope', NULL, NULL, '_SELF', 9, 1, 1
FROM DUAL WHERE @mroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @mroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mailbox', 'Read and send mail with your own mail account', 'envelope-at', 'mail', 'mail/mail.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mail');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mail Settings', 'Connect your mail account (SMTP + IMAP)', 'gear', 'mailsettings', 'mail/mail-settings.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mailsettings');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Intramail', 'Messages between the users of the company', 'building', 'intramail', 'mail/mail.html?tab=intra', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'intramail');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('mail','mailsettings','intramail');

USE sfa_demo;
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Mail', 'Mail and intramail', 'envelope', NULL, NULL, '_SELF', 9, 1, 1
FROM DUAL WHERE @mroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @mroot := (SELECT id FROM company_menu_self WHERE label = 'Mail' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @mroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mailbox', 'Read and send mail with your own mail account', 'envelope-at', 'mail', 'mail/mail.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mail');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Mail Settings', 'Connect your mail account (SMTP + IMAP)', 'gear', 'mailsettings', 'mail/mail-settings.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'mailsettings');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mroot, p.emp_id, 'WEB', 'Intramail', 'Messages between the users of the company', 'building', 'intramail', 'mail/mail.html?tab=intra', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mroot LIMIT 1) p WHERE @mroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'intramail');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('mail','mailsettings','intramail');
