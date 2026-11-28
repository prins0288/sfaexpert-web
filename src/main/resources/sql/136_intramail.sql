-- 136: intramail - intra_message, intra_recipient (read / star / delete per receiver), intra_attachment, intra_group (company and personal), intra_group_member. Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS intra_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, sender_emp_id VARCHAR(40) NOT NULL, subject VARCHAR(400) NULL, body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL,
  parent_id BIGINT UNSIGNED NULL COMMENT 'the message this one answers (reply / reply all / forward)', is_draft TINYINT(1) NOT NULL DEFAULT 0, draft_json TEXT NULL COMMENT 'the recipients chosen so far (draft only)',
  has_attach TINYINT(1) NOT NULL DEFAULT 0, sender_deleted TINYINT(1) NOT NULL DEFAULT 0, to_label VARCHAR(500) NULL COMMENT 'who it was sent to, as shown in Sent (names / groups / All employees)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_intra_sender (sender_emp_id, is_draft, sent_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='intramail: messages inside the application';
CREATE TABLE IF NOT EXISTS intra_recipient (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, kind VARCHAR(3) NOT NULL DEFAULT 'TO' COMMENT 'TO / CC / BCC', via VARCHAR(8) NOT NULL DEFAULT 'USER' COMMENT 'USER / GROUP / ALL',
  is_read TINYINT(1) NOT NULL DEFAULT 0, read_at DATETIME NULL, is_starred TINYINT(1) NOT NULL DEFAULT 0, deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 inbox / 1 trash / 2 deleted for good',
  PRIMARY KEY (id), UNIQUE KEY uq_intra_rcpt (message_id, emp_id), KEY ix_intra_rcpt_emp (emp_id, deleted, is_read), CONSTRAINT fk_intra_rcpt_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every receiver has his own read / star / delete state';
CREATE TABLE IF NOT EXISTS intra_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, filename VARCHAR(255) NOT NULL, content_type VARCHAR(120) NULL, size_bytes BIGINT NULL, path VARCHAR(400) NOT NULL,
  PRIMARY KEY (id), KEY ix_intra_att_msg (message_id), CONSTRAINT fk_intra_att_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner_emp_id VARCHAR(40) NULL COMMENT 'NULL = company group (set by the admin), else the personal group of that employee', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_intra_group (owner_emp_id, name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group_member (
  group_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, PRIMARY KEY (group_id, emp_id), CONSTRAINT fk_intra_gm_group FOREIGN KEY (group_id) REFERENCES intra_group (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE globex_db;
CREATE TABLE IF NOT EXISTS intra_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, sender_emp_id VARCHAR(40) NOT NULL, subject VARCHAR(400) NULL, body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL,
  parent_id BIGINT UNSIGNED NULL COMMENT 'the message this one answers (reply / reply all / forward)', is_draft TINYINT(1) NOT NULL DEFAULT 0, draft_json TEXT NULL COMMENT 'the recipients chosen so far (draft only)',
  has_attach TINYINT(1) NOT NULL DEFAULT 0, sender_deleted TINYINT(1) NOT NULL DEFAULT 0, to_label VARCHAR(500) NULL COMMENT 'who it was sent to, as shown in Sent (names / groups / All employees)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_intra_sender (sender_emp_id, is_draft, sent_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='intramail: messages inside the application';
CREATE TABLE IF NOT EXISTS intra_recipient (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, kind VARCHAR(3) NOT NULL DEFAULT 'TO' COMMENT 'TO / CC / BCC', via VARCHAR(8) NOT NULL DEFAULT 'USER' COMMENT 'USER / GROUP / ALL',
  is_read TINYINT(1) NOT NULL DEFAULT 0, read_at DATETIME NULL, is_starred TINYINT(1) NOT NULL DEFAULT 0, deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 inbox / 1 trash / 2 deleted for good',
  PRIMARY KEY (id), UNIQUE KEY uq_intra_rcpt (message_id, emp_id), KEY ix_intra_rcpt_emp (emp_id, deleted, is_read), CONSTRAINT fk_intra_rcpt_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every receiver has his own read / star / delete state';
CREATE TABLE IF NOT EXISTS intra_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, filename VARCHAR(255) NOT NULL, content_type VARCHAR(120) NULL, size_bytes BIGINT NULL, path VARCHAR(400) NOT NULL,
  PRIMARY KEY (id), KEY ix_intra_att_msg (message_id), CONSTRAINT fk_intra_att_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner_emp_id VARCHAR(40) NULL COMMENT 'NULL = company group (set by the admin), else the personal group of that employee', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_intra_group (owner_emp_id, name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group_member (
  group_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, PRIMARY KEY (group_id, emp_id), CONSTRAINT fk_intra_gm_group FOREIGN KEY (group_id) REFERENCES intra_group (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE initech_db;
CREATE TABLE IF NOT EXISTS intra_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, sender_emp_id VARCHAR(40) NOT NULL, subject VARCHAR(400) NULL, body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL,
  parent_id BIGINT UNSIGNED NULL COMMENT 'the message this one answers (reply / reply all / forward)', is_draft TINYINT(1) NOT NULL DEFAULT 0, draft_json TEXT NULL COMMENT 'the recipients chosen so far (draft only)',
  has_attach TINYINT(1) NOT NULL DEFAULT 0, sender_deleted TINYINT(1) NOT NULL DEFAULT 0, to_label VARCHAR(500) NULL COMMENT 'who it was sent to, as shown in Sent (names / groups / All employees)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_intra_sender (sender_emp_id, is_draft, sent_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='intramail: messages inside the application';
CREATE TABLE IF NOT EXISTS intra_recipient (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, kind VARCHAR(3) NOT NULL DEFAULT 'TO' COMMENT 'TO / CC / BCC', via VARCHAR(8) NOT NULL DEFAULT 'USER' COMMENT 'USER / GROUP / ALL',
  is_read TINYINT(1) NOT NULL DEFAULT 0, read_at DATETIME NULL, is_starred TINYINT(1) NOT NULL DEFAULT 0, deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 inbox / 1 trash / 2 deleted for good',
  PRIMARY KEY (id), UNIQUE KEY uq_intra_rcpt (message_id, emp_id), KEY ix_intra_rcpt_emp (emp_id, deleted, is_read), CONSTRAINT fk_intra_rcpt_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every receiver has his own read / star / delete state';
CREATE TABLE IF NOT EXISTS intra_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, filename VARCHAR(255) NOT NULL, content_type VARCHAR(120) NULL, size_bytes BIGINT NULL, path VARCHAR(400) NOT NULL,
  PRIMARY KEY (id), KEY ix_intra_att_msg (message_id), CONSTRAINT fk_intra_att_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner_emp_id VARCHAR(40) NULL COMMENT 'NULL = company group (set by the admin), else the personal group of that employee', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_intra_group (owner_emp_id, name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group_member (
  group_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, PRIMARY KEY (group_id, emp_id), CONSTRAINT fk_intra_gm_group FOREIGN KEY (group_id) REFERENCES intra_group (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE sfa_demo;
CREATE TABLE IF NOT EXISTS intra_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, sender_emp_id VARCHAR(40) NOT NULL, subject VARCHAR(400) NULL, body_html MEDIUMTEXT NULL, body_text MEDIUMTEXT NULL,
  parent_id BIGINT UNSIGNED NULL COMMENT 'the message this one answers (reply / reply all / forward)', is_draft TINYINT(1) NOT NULL DEFAULT 0, draft_json TEXT NULL COMMENT 'the recipients chosen so far (draft only)',
  has_attach TINYINT(1) NOT NULL DEFAULT 0, sender_deleted TINYINT(1) NOT NULL DEFAULT 0, to_label VARCHAR(500) NULL COMMENT 'who it was sent to, as shown in Sent (names / groups / All employees)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_intra_sender (sender_emp_id, is_draft, sent_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='intramail: messages inside the application';
CREATE TABLE IF NOT EXISTS intra_recipient (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, kind VARCHAR(3) NOT NULL DEFAULT 'TO' COMMENT 'TO / CC / BCC', via VARCHAR(8) NOT NULL DEFAULT 'USER' COMMENT 'USER / GROUP / ALL',
  is_read TINYINT(1) NOT NULL DEFAULT 0, read_at DATETIME NULL, is_starred TINYINT(1) NOT NULL DEFAULT 0, deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 inbox / 1 trash / 2 deleted for good',
  PRIMARY KEY (id), UNIQUE KEY uq_intra_rcpt (message_id, emp_id), KEY ix_intra_rcpt_emp (emp_id, deleted, is_read), CONSTRAINT fk_intra_rcpt_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every receiver has his own read / star / delete state';
CREATE TABLE IF NOT EXISTS intra_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, message_id BIGINT UNSIGNED NOT NULL, filename VARCHAR(255) NOT NULL, content_type VARCHAR(120) NULL, size_bytes BIGINT NULL, path VARCHAR(400) NOT NULL,
  PRIMARY KEY (id), KEY ix_intra_att_msg (message_id), CONSTRAINT fk_intra_att_msg FOREIGN KEY (message_id) REFERENCES intra_message (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner_emp_id VARCHAR(40) NULL COMMENT 'NULL = company group (set by the admin), else the personal group of that employee', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_intra_group (owner_emp_id, name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS intra_group_member (
  group_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, PRIMARY KEY (group_id, emp_id), CONSTRAINT fk_intra_gm_group FOREIGN KEY (group_id) REFERENCES intra_group (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
