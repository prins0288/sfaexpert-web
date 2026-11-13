-- 100: marquee message in the header (HTML, placeholders, per state / HQ / designation / employee) + its menu. Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS marquee_message (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  message_html TEXT         NOT NULL COMMENT 'the message, HTML allowed (b, i, u, a, span, br ...); placeholders {name} {empCode} {designation} {hq} {state} {manager} {date}',
  speed        SMALLINT UNSIGNED NOT NULL DEFAULT 60 COMMENT 'scroll speed in pixels per second',
  starts_at    DATETIME     NULL COMMENT 'show from (empty = now)',
  ends_at      DATETIME     NULL COMMENT 'show until (empty = no end)',
  sort_order   SMALLINT     NOT NULL DEFAULT 0,
  is_active    TINYINT(1)   NOT NULL DEFAULT 1,
  created_by   VARCHAR(40)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   VARCHAR(40)  NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  KEY ix_marquee_active (is_active, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'scrolling message in the page header; no row in marquee_target = shown to everyone';
CREATE TABLE IF NOT EXISTS marquee_target (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  marquee_id   INT UNSIGNED NOT NULL COMMENT '-> marquee_message.id',
  target_type  VARCHAR(12)  NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / designation id / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_marquee_target (marquee_id, target_type, target_value),
  KEY ix_marquee_target (target_type, target_value),
  CONSTRAINT fk_mt_marquee FOREIGN KEY (marquee_id) REFERENCES marquee_message (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'who a marquee message is for';

SET @mq_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @mq_parent, p.emp_id, 'WEB', 'Marquee Message', 'Scrolling message in the header', 'megaphone', 'marqueemessage', 'master/marquee.html', '_SELF', 26, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mq_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @mq_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'marqueemessage');

USE globex_db;
CREATE TABLE IF NOT EXISTS marquee_message (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  message_html TEXT         NOT NULL COMMENT 'the message, HTML allowed (b, i, u, a, span, br ...); placeholders {name} {empCode} {designation} {hq} {state} {manager} {date}',
  speed        SMALLINT UNSIGNED NOT NULL DEFAULT 60 COMMENT 'scroll speed in pixels per second',
  starts_at    DATETIME     NULL COMMENT 'show from (empty = now)',
  ends_at      DATETIME     NULL COMMENT 'show until (empty = no end)',
  sort_order   SMALLINT     NOT NULL DEFAULT 0,
  is_active    TINYINT(1)   NOT NULL DEFAULT 1,
  created_by   VARCHAR(40)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   VARCHAR(40)  NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  KEY ix_marquee_active (is_active, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'scrolling message in the page header; no row in marquee_target = shown to everyone';
CREATE TABLE IF NOT EXISTS marquee_target (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  marquee_id   INT UNSIGNED NOT NULL COMMENT '-> marquee_message.id',
  target_type  VARCHAR(12)  NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / designation id / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_marquee_target (marquee_id, target_type, target_value),
  KEY ix_marquee_target (target_type, target_value),
  CONSTRAINT fk_mt_marquee FOREIGN KEY (marquee_id) REFERENCES marquee_message (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'who a marquee message is for';

SET @mq_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @mq_parent, p.emp_id, 'WEB', 'Marquee Message', 'Scrolling message in the header', 'megaphone', 'marqueemessage', 'master/marquee.html', '_SELF', 26, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mq_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @mq_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'marqueemessage');

USE initech_db;
CREATE TABLE IF NOT EXISTS marquee_message (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  message_html TEXT         NOT NULL COMMENT 'the message, HTML allowed (b, i, u, a, span, br ...); placeholders {name} {empCode} {designation} {hq} {state} {manager} {date}',
  speed        SMALLINT UNSIGNED NOT NULL DEFAULT 60 COMMENT 'scroll speed in pixels per second',
  starts_at    DATETIME     NULL COMMENT 'show from (empty = now)',
  ends_at      DATETIME     NULL COMMENT 'show until (empty = no end)',
  sort_order   SMALLINT     NOT NULL DEFAULT 0,
  is_active    TINYINT(1)   NOT NULL DEFAULT 1,
  created_by   VARCHAR(40)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   VARCHAR(40)  NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  KEY ix_marquee_active (is_active, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'scrolling message in the page header; no row in marquee_target = shown to everyone';
CREATE TABLE IF NOT EXISTS marquee_target (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  marquee_id   INT UNSIGNED NOT NULL COMMENT '-> marquee_message.id',
  target_type  VARCHAR(12)  NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / designation id / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_marquee_target (marquee_id, target_type, target_value),
  KEY ix_marquee_target (target_type, target_value),
  CONSTRAINT fk_mt_marquee FOREIGN KEY (marquee_id) REFERENCES marquee_message (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'who a marquee message is for';

SET @mq_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @mq_parent, p.emp_id, 'WEB', 'Marquee Message', 'Scrolling message in the header', 'megaphone', 'marqueemessage', 'master/marquee.html', '_SELF', 26, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mq_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @mq_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'marqueemessage');

USE sfa_demo;
CREATE TABLE IF NOT EXISTS marquee_message (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  message_html TEXT         NOT NULL COMMENT 'the message, HTML allowed (b, i, u, a, span, br ...); placeholders {name} {empCode} {designation} {hq} {state} {manager} {date}',
  speed        SMALLINT UNSIGNED NOT NULL DEFAULT 60 COMMENT 'scroll speed in pixels per second',
  starts_at    DATETIME     NULL COMMENT 'show from (empty = now)',
  ends_at      DATETIME     NULL COMMENT 'show until (empty = no end)',
  sort_order   SMALLINT     NOT NULL DEFAULT 0,
  is_active    TINYINT(1)   NOT NULL DEFAULT 1,
  created_by   VARCHAR(40)  NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   VARCHAR(40)  NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  KEY ix_marquee_active (is_active, starts_at, ends_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'scrolling message in the page header; no row in marquee_target = shown to everyone';
CREATE TABLE IF NOT EXISTS marquee_target (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  marquee_id   INT UNSIGNED NOT NULL COMMENT '-> marquee_message.id',
  target_type  VARCHAR(12)  NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / designation id / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_marquee_target (marquee_id, target_type, target_value),
  KEY ix_marquee_target (target_type, target_value),
  CONSTRAINT fk_mt_marquee FOREIGN KEY (marquee_id) REFERENCES marquee_message (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'who a marquee message is for';

SET @mq_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @mq_parent, p.emp_id, 'WEB', 'Marquee Message', 'Scrolling message in the header', 'megaphone', 'marqueemessage', 'master/marquee.html', '_SELF', 26, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mq_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @mq_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'marqueemessage');
