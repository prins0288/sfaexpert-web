-- 134: mail module core - mail_admin_setting, mail_account (password encrypted), mail_folder, mail_message (cache), mail_attachment, mail_outbox. Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS mail_admin_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, company_domain VARCHAR(120) NULL,
  default_smtp_host VARCHAR(120) NULL, default_smtp_port INT NULL, default_smtp_security VARCHAR(8) NULL,
  default_imap_host VARCHAR(120) NULL, default_imap_port INT NULL, default_imap_security VARCHAR(8) NULL,
  internal_hosts VARCHAR(500) NULL COMMENT 'comma separated hosts that may be on the internal network (the company mail server) - only the admin sets this',
  sync_interval_min INT NOT NULL DEFAULT 10, initial_sync_days INT NOT NULL DEFAULT 30, max_attachment_mb INT NOT NULL DEFAULT 20, intramail_for_all TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mail module: company defaults and limits (one row)';
CREATE TABLE IF NOT EXISTS mail_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, email VARCHAR(150) NOT NULL, display_name VARCHAR(100) NULL, username VARCHAR(150) NOT NULL,
  auth_type VARCHAR(10) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD now; OAUTH2_GOOGLE / OAUTH2_MS later (oauth_enc)',
  password_enc VARCHAR(700) NULL COMMENT 'AES-GCM, key from the environment (SFA_MAIL_KEY) - never the plain password', oauth_enc VARCHAR(2000) NULL,
  smtp_host VARCHAR(150) NOT NULL, smtp_port INT NOT NULL DEFAULT 587, smtp_security VARCHAR(8) NOT NULL DEFAULT 'STARTTLS',
  imap_host VARCHAR(150) NOT NULL, imap_port INT NOT NULL DEFAULT 993, imap_security VARCHAR(8) NOT NULL DEFAULT 'SSL',
  signature TEXT NULL, is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  sync_state VARCHAR(10) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE / RUNNING', sync_started_at DATETIME NULL, last_sync_at DATETIME NULL, last_sync_error VARCHAR(300) NULL, last_seen_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_account_emp (emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the external mail account (SMTP + IMAP) of an employee; one per employee';
CREATE TABLE IF NOT EXISTS mail_folder (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, name VARCHAR(150) NOT NULL, path VARCHAR(300) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'CUSTOM' COMMENT 'INBOX / SENT / DRAFTS / TRASH / SPAM / ARCHIVE / CUSTOM', uidvalidity BIGINT NULL, last_uid BIGINT NOT NULL DEFAULT 0, lowest_uid BIGINT NULL,
  unread_count INT NOT NULL DEFAULT 0, total_count INT NOT NULL DEFAULT 0, sort_order INT NOT NULL DEFAULT 100,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_folder (account_id, path), CONSTRAINT fk_mail_folder_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mail_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, folder_id BIGINT UNSIGNED NOT NULL, uid BIGINT NOT NULL,
  message_id VARCHAR(300) NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL,
  from_addr VARCHAR(200) NULL, from_name VARCHAR(200) NULL, to_addrs TEXT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  sent_at DATETIME NULL, size_bytes INT NULL, has_attach TINYINT(1) NOT NULL DEFAULT 0, is_read TINYINT(1) NOT NULL DEFAULT 0, is_starred TINYINT(1) NOT NULL DEFAULT 0,
  is_draft TINYINT(1) NOT NULL DEFAULT 0, snippet VARCHAR(220) NULL, body_text MEDIUMTEXT NULL, body_html MEDIUMTEXT NULL, body_fetched TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_msg_uid (folder_id, uid), KEY ix_mail_msg_list (account_id, folder_id, sent_at), KEY ix_mail_msg_mid (account_id, message_id(120)),
  CONSTRAINT fk_mail_msg_folder FOREIGN KEY (folder_id) REFERENCES mail_folder (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cache of the server mails: the list comes from here, the body is fetched on the first open';
CREATE TABLE IF NOT EXISTS mail_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, part_no INT NOT NULL, filename VARCHAR(255) NULL, content_type VARCHAR(120) NULL, size_bytes INT NULL, content_id VARCHAR(200) NULL, is_inline TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_mail_att_msg (message_id), CONSTRAINT fk_mail_att_msg FOREIGN KEY (message_id) REFERENCES mail_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='attachment list only - the file is fetched from the server when it is downloaded';
CREATE TABLE IF NOT EXISTS mail_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / SENDING / SENT / FAILED / UNKNOWN (the server may have taken it - checked by the user)',
  message_id VARCHAR(300) NOT NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL, to_addrs TEXT NOT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL, attachments_json TEXT NULL, attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME NULL, last_error VARCHAR(400) NULL,
  claimed_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL, copy_saved TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_outbox_mid (account_id, message_id(150)), KEY ix_mail_outbox_st (status, next_attempt_at),
  CONSTRAINT fk_mail_outbox_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mails to send: a worker sends them; the generated Message-ID makes a retry safe';
INSERT IGNORE INTO mail_admin_setting (id) VALUES (1);

USE globex_db;
CREATE TABLE IF NOT EXISTS mail_admin_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, company_domain VARCHAR(120) NULL,
  default_smtp_host VARCHAR(120) NULL, default_smtp_port INT NULL, default_smtp_security VARCHAR(8) NULL,
  default_imap_host VARCHAR(120) NULL, default_imap_port INT NULL, default_imap_security VARCHAR(8) NULL,
  internal_hosts VARCHAR(500) NULL COMMENT 'comma separated hosts that may be on the internal network (the company mail server) - only the admin sets this',
  sync_interval_min INT NOT NULL DEFAULT 10, initial_sync_days INT NOT NULL DEFAULT 30, max_attachment_mb INT NOT NULL DEFAULT 20, intramail_for_all TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mail module: company defaults and limits (one row)';
CREATE TABLE IF NOT EXISTS mail_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, email VARCHAR(150) NOT NULL, display_name VARCHAR(100) NULL, username VARCHAR(150) NOT NULL,
  auth_type VARCHAR(10) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD now; OAUTH2_GOOGLE / OAUTH2_MS later (oauth_enc)',
  password_enc VARCHAR(700) NULL COMMENT 'AES-GCM, key from the environment (SFA_MAIL_KEY) - never the plain password', oauth_enc VARCHAR(2000) NULL,
  smtp_host VARCHAR(150) NOT NULL, smtp_port INT NOT NULL DEFAULT 587, smtp_security VARCHAR(8) NOT NULL DEFAULT 'STARTTLS',
  imap_host VARCHAR(150) NOT NULL, imap_port INT NOT NULL DEFAULT 993, imap_security VARCHAR(8) NOT NULL DEFAULT 'SSL',
  signature TEXT NULL, is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  sync_state VARCHAR(10) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE / RUNNING', sync_started_at DATETIME NULL, last_sync_at DATETIME NULL, last_sync_error VARCHAR(300) NULL, last_seen_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_account_emp (emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the external mail account (SMTP + IMAP) of an employee; one per employee';
CREATE TABLE IF NOT EXISTS mail_folder (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, name VARCHAR(150) NOT NULL, path VARCHAR(300) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'CUSTOM' COMMENT 'INBOX / SENT / DRAFTS / TRASH / SPAM / ARCHIVE / CUSTOM', uidvalidity BIGINT NULL, last_uid BIGINT NOT NULL DEFAULT 0, lowest_uid BIGINT NULL,
  unread_count INT NOT NULL DEFAULT 0, total_count INT NOT NULL DEFAULT 0, sort_order INT NOT NULL DEFAULT 100,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_folder (account_id, path), CONSTRAINT fk_mail_folder_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mail_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, folder_id BIGINT UNSIGNED NOT NULL, uid BIGINT NOT NULL,
  message_id VARCHAR(300) NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL,
  from_addr VARCHAR(200) NULL, from_name VARCHAR(200) NULL, to_addrs TEXT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  sent_at DATETIME NULL, size_bytes INT NULL, has_attach TINYINT(1) NOT NULL DEFAULT 0, is_read TINYINT(1) NOT NULL DEFAULT 0, is_starred TINYINT(1) NOT NULL DEFAULT 0,
  is_draft TINYINT(1) NOT NULL DEFAULT 0, snippet VARCHAR(220) NULL, body_text MEDIUMTEXT NULL, body_html MEDIUMTEXT NULL, body_fetched TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_msg_uid (folder_id, uid), KEY ix_mail_msg_list (account_id, folder_id, sent_at), KEY ix_mail_msg_mid (account_id, message_id(120)),
  CONSTRAINT fk_mail_msg_folder FOREIGN KEY (folder_id) REFERENCES mail_folder (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cache of the server mails: the list comes from here, the body is fetched on the first open';
CREATE TABLE IF NOT EXISTS mail_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, part_no INT NOT NULL, filename VARCHAR(255) NULL, content_type VARCHAR(120) NULL, size_bytes INT NULL, content_id VARCHAR(200) NULL, is_inline TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_mail_att_msg (message_id), CONSTRAINT fk_mail_att_msg FOREIGN KEY (message_id) REFERENCES mail_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='attachment list only - the file is fetched from the server when it is downloaded';
CREATE TABLE IF NOT EXISTS mail_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / SENDING / SENT / FAILED / UNKNOWN (the server may have taken it - checked by the user)',
  message_id VARCHAR(300) NOT NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL, to_addrs TEXT NOT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL, attachments_json TEXT NULL, attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME NULL, last_error VARCHAR(400) NULL,
  claimed_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL, copy_saved TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_outbox_mid (account_id, message_id(150)), KEY ix_mail_outbox_st (status, next_attempt_at),
  CONSTRAINT fk_mail_outbox_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mails to send: a worker sends them; the generated Message-ID makes a retry safe';
INSERT IGNORE INTO mail_admin_setting (id) VALUES (1);

USE initech_db;
CREATE TABLE IF NOT EXISTS mail_admin_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, company_domain VARCHAR(120) NULL,
  default_smtp_host VARCHAR(120) NULL, default_smtp_port INT NULL, default_smtp_security VARCHAR(8) NULL,
  default_imap_host VARCHAR(120) NULL, default_imap_port INT NULL, default_imap_security VARCHAR(8) NULL,
  internal_hosts VARCHAR(500) NULL COMMENT 'comma separated hosts that may be on the internal network (the company mail server) - only the admin sets this',
  sync_interval_min INT NOT NULL DEFAULT 10, initial_sync_days INT NOT NULL DEFAULT 30, max_attachment_mb INT NOT NULL DEFAULT 20, intramail_for_all TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mail module: company defaults and limits (one row)';
CREATE TABLE IF NOT EXISTS mail_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, email VARCHAR(150) NOT NULL, display_name VARCHAR(100) NULL, username VARCHAR(150) NOT NULL,
  auth_type VARCHAR(10) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD now; OAUTH2_GOOGLE / OAUTH2_MS later (oauth_enc)',
  password_enc VARCHAR(700) NULL COMMENT 'AES-GCM, key from the environment (SFA_MAIL_KEY) - never the plain password', oauth_enc VARCHAR(2000) NULL,
  smtp_host VARCHAR(150) NOT NULL, smtp_port INT NOT NULL DEFAULT 587, smtp_security VARCHAR(8) NOT NULL DEFAULT 'STARTTLS',
  imap_host VARCHAR(150) NOT NULL, imap_port INT NOT NULL DEFAULT 993, imap_security VARCHAR(8) NOT NULL DEFAULT 'SSL',
  signature TEXT NULL, is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  sync_state VARCHAR(10) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE / RUNNING', sync_started_at DATETIME NULL, last_sync_at DATETIME NULL, last_sync_error VARCHAR(300) NULL, last_seen_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_account_emp (emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the external mail account (SMTP + IMAP) of an employee; one per employee';
CREATE TABLE IF NOT EXISTS mail_folder (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, name VARCHAR(150) NOT NULL, path VARCHAR(300) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'CUSTOM' COMMENT 'INBOX / SENT / DRAFTS / TRASH / SPAM / ARCHIVE / CUSTOM', uidvalidity BIGINT NULL, last_uid BIGINT NOT NULL DEFAULT 0, lowest_uid BIGINT NULL,
  unread_count INT NOT NULL DEFAULT 0, total_count INT NOT NULL DEFAULT 0, sort_order INT NOT NULL DEFAULT 100,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_folder (account_id, path), CONSTRAINT fk_mail_folder_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mail_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, folder_id BIGINT UNSIGNED NOT NULL, uid BIGINT NOT NULL,
  message_id VARCHAR(300) NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL,
  from_addr VARCHAR(200) NULL, from_name VARCHAR(200) NULL, to_addrs TEXT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  sent_at DATETIME NULL, size_bytes INT NULL, has_attach TINYINT(1) NOT NULL DEFAULT 0, is_read TINYINT(1) NOT NULL DEFAULT 0, is_starred TINYINT(1) NOT NULL DEFAULT 0,
  is_draft TINYINT(1) NOT NULL DEFAULT 0, snippet VARCHAR(220) NULL, body_text MEDIUMTEXT NULL, body_html MEDIUMTEXT NULL, body_fetched TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_msg_uid (folder_id, uid), KEY ix_mail_msg_list (account_id, folder_id, sent_at), KEY ix_mail_msg_mid (account_id, message_id(120)),
  CONSTRAINT fk_mail_msg_folder FOREIGN KEY (folder_id) REFERENCES mail_folder (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cache of the server mails: the list comes from here, the body is fetched on the first open';
CREATE TABLE IF NOT EXISTS mail_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, part_no INT NOT NULL, filename VARCHAR(255) NULL, content_type VARCHAR(120) NULL, size_bytes INT NULL, content_id VARCHAR(200) NULL, is_inline TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_mail_att_msg (message_id), CONSTRAINT fk_mail_att_msg FOREIGN KEY (message_id) REFERENCES mail_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='attachment list only - the file is fetched from the server when it is downloaded';
CREATE TABLE IF NOT EXISTS mail_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / SENDING / SENT / FAILED / UNKNOWN (the server may have taken it - checked by the user)',
  message_id VARCHAR(300) NOT NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL, to_addrs TEXT NOT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL, attachments_json TEXT NULL, attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME NULL, last_error VARCHAR(400) NULL,
  claimed_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL, copy_saved TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_outbox_mid (account_id, message_id(150)), KEY ix_mail_outbox_st (status, next_attempt_at),
  CONSTRAINT fk_mail_outbox_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mails to send: a worker sends them; the generated Message-ID makes a retry safe';
INSERT IGNORE INTO mail_admin_setting (id) VALUES (1);

USE sfa_demo;
CREATE TABLE IF NOT EXISTS mail_admin_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, company_domain VARCHAR(120) NULL,
  default_smtp_host VARCHAR(120) NULL, default_smtp_port INT NULL, default_smtp_security VARCHAR(8) NULL,
  default_imap_host VARCHAR(120) NULL, default_imap_port INT NULL, default_imap_security VARCHAR(8) NULL,
  internal_hosts VARCHAR(500) NULL COMMENT 'comma separated hosts that may be on the internal network (the company mail server) - only the admin sets this',
  sync_interval_min INT NOT NULL DEFAULT 10, initial_sync_days INT NOT NULL DEFAULT 30, max_attachment_mb INT NOT NULL DEFAULT 20, intramail_for_all TINYINT(1) NOT NULL DEFAULT 1,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mail module: company defaults and limits (one row)';
CREATE TABLE IF NOT EXISTS mail_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, email VARCHAR(150) NOT NULL, display_name VARCHAR(100) NULL, username VARCHAR(150) NOT NULL,
  auth_type VARCHAR(10) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD now; OAUTH2_GOOGLE / OAUTH2_MS later (oauth_enc)',
  password_enc VARCHAR(700) NULL COMMENT 'AES-GCM, key from the environment (SFA_MAIL_KEY) - never the plain password', oauth_enc VARCHAR(2000) NULL,
  smtp_host VARCHAR(150) NOT NULL, smtp_port INT NOT NULL DEFAULT 587, smtp_security VARCHAR(8) NOT NULL DEFAULT 'STARTTLS',
  imap_host VARCHAR(150) NOT NULL, imap_port INT NOT NULL DEFAULT 993, imap_security VARCHAR(8) NOT NULL DEFAULT 'SSL',
  signature TEXT NULL, is_enabled TINYINT(1) NOT NULL DEFAULT 1,
  sync_state VARCHAR(10) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE / RUNNING', sync_started_at DATETIME NULL, last_sync_at DATETIME NULL, last_sync_error VARCHAR(300) NULL, last_seen_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_account_emp (emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the external mail account (SMTP + IMAP) of an employee; one per employee';
CREATE TABLE IF NOT EXISTS mail_folder (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, name VARCHAR(150) NOT NULL, path VARCHAR(300) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'CUSTOM' COMMENT 'INBOX / SENT / DRAFTS / TRASH / SPAM / ARCHIVE / CUSTOM', uidvalidity BIGINT NULL, last_uid BIGINT NOT NULL DEFAULT 0, lowest_uid BIGINT NULL,
  unread_count INT NOT NULL DEFAULT 0, total_count INT NOT NULL DEFAULT 0, sort_order INT NOT NULL DEFAULT 100,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_folder (account_id, path), CONSTRAINT fk_mail_folder_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS mail_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, folder_id BIGINT UNSIGNED NOT NULL, uid BIGINT NOT NULL,
  message_id VARCHAR(300) NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL,
  from_addr VARCHAR(200) NULL, from_name VARCHAR(200) NULL, to_addrs TEXT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  sent_at DATETIME NULL, size_bytes INT NULL, has_attach TINYINT(1) NOT NULL DEFAULT 0, is_read TINYINT(1) NOT NULL DEFAULT 0, is_starred TINYINT(1) NOT NULL DEFAULT 0,
  is_draft TINYINT(1) NOT NULL DEFAULT 0, snippet VARCHAR(220) NULL, body_text MEDIUMTEXT NULL, body_html MEDIUMTEXT NULL, body_fetched TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_msg_uid (folder_id, uid), KEY ix_mail_msg_list (account_id, folder_id, sent_at), KEY ix_mail_msg_mid (account_id, message_id(120)),
  CONSTRAINT fk_mail_msg_folder FOREIGN KEY (folder_id) REFERENCES mail_folder (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cache of the server mails: the list comes from here, the body is fetched on the first open';
CREATE TABLE IF NOT EXISTS mail_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, part_no INT NOT NULL, filename VARCHAR(255) NULL, content_type VARCHAR(120) NULL, size_bytes INT NULL, content_id VARCHAR(200) NULL, is_inline TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_mail_att_msg (message_id), CONSTRAINT fk_mail_att_msg FOREIGN KEY (message_id) REFERENCES mail_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='attachment list only - the file is fetched from the server when it is downloaded';
CREATE TABLE IF NOT EXISTS mail_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, account_id BIGINT UNSIGNED NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / SENDING / SENT / FAILED / UNKNOWN (the server may have taken it - checked by the user)',
  message_id VARCHAR(300) NOT NULL, in_reply_to VARCHAR(300) NULL, refs TEXT NULL, to_addrs TEXT NOT NULL, cc_addrs TEXT NULL, bcc_addrs TEXT NULL, subject VARCHAR(500) NULL,
  body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL, attachments_json TEXT NULL, attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME NULL, last_error VARCHAR(400) NULL,
  claimed_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL, copy_saved TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uq_mail_outbox_mid (account_id, message_id(150)), KEY ix_mail_outbox_st (status, next_attempt_at),
  CONSTRAINT fk_mail_outbox_acc FOREIGN KEY (account_id) REFERENCES mail_account (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mails to send: a worker sends them; the generated Message-ID makes a retry safe';
INSERT IGNORE INTO mail_admin_setting (id) VALUES (1);
