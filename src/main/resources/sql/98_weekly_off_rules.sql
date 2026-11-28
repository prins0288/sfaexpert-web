-- 98: weekly off RULES: weekday(s) + which week of the month, per state / HQ / employee (or everyone). Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS weekly_off_rule (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  rule_name  VARCHAR(80)  NULL COMMENT 'optional label; empty = built from the days, e.g. "3rd Saturday"',
  weekdays   VARCHAR(30)  NOT NULL COMMENT 'comma list of MON,TUE,WED,THU,FRI,SAT,SUN',
  weeks      VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL = every week; or a list of 1,2,3,4,5 (nth of the month) and L (last), e.g. "2,4"',
  remarks    VARCHAR(250) NULL,
  is_active  TINYINT(1)   NOT NULL DEFAULT 1,
  created_by VARCHAR(40)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(40)  NULL,
  updated_at DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'recurring weekly off (3rd Saturday, Saturday + Sunday ...); no row in weekly_off_assignment = everyone';
CREATE TABLE IF NOT EXISTS weekly_off_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id      INT UNSIGNED NOT NULL COMMENT '-> weekly_off_rule.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE / HQ / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_wo_target (rule_id, target_type, target_value),
  KEY ix_wo_target (target_type, target_value),
  CONSTRAINT fk_woa_rule FOREIGN KEY (rule_id) REFERENCES weekly_off_rule (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'weekly off rule -> state / HQ / employee';

USE globex_db;
CREATE TABLE IF NOT EXISTS weekly_off_rule (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  rule_name  VARCHAR(80)  NULL COMMENT 'optional label; empty = built from the days, e.g. "3rd Saturday"',
  weekdays   VARCHAR(30)  NOT NULL COMMENT 'comma list of MON,TUE,WED,THU,FRI,SAT,SUN',
  weeks      VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL = every week; or a list of 1,2,3,4,5 (nth of the month) and L (last), e.g. "2,4"',
  remarks    VARCHAR(250) NULL,
  is_active  TINYINT(1)   NOT NULL DEFAULT 1,
  created_by VARCHAR(40)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(40)  NULL,
  updated_at DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'recurring weekly off (3rd Saturday, Saturday + Sunday ...); no row in weekly_off_assignment = everyone';
CREATE TABLE IF NOT EXISTS weekly_off_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id      INT UNSIGNED NOT NULL COMMENT '-> weekly_off_rule.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE / HQ / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_wo_target (rule_id, target_type, target_value),
  KEY ix_wo_target (target_type, target_value),
  CONSTRAINT fk_woa_rule FOREIGN KEY (rule_id) REFERENCES weekly_off_rule (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'weekly off rule -> state / HQ / employee';

USE initech_db;
CREATE TABLE IF NOT EXISTS weekly_off_rule (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  rule_name  VARCHAR(80)  NULL COMMENT 'optional label; empty = built from the days, e.g. "3rd Saturday"',
  weekdays   VARCHAR(30)  NOT NULL COMMENT 'comma list of MON,TUE,WED,THU,FRI,SAT,SUN',
  weeks      VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL = every week; or a list of 1,2,3,4,5 (nth of the month) and L (last), e.g. "2,4"',
  remarks    VARCHAR(250) NULL,
  is_active  TINYINT(1)   NOT NULL DEFAULT 1,
  created_by VARCHAR(40)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(40)  NULL,
  updated_at DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'recurring weekly off (3rd Saturday, Saturday + Sunday ...); no row in weekly_off_assignment = everyone';
CREATE TABLE IF NOT EXISTS weekly_off_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id      INT UNSIGNED NOT NULL COMMENT '-> weekly_off_rule.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE / HQ / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_wo_target (rule_id, target_type, target_value),
  KEY ix_wo_target (target_type, target_value),
  CONSTRAINT fk_woa_rule FOREIGN KEY (rule_id) REFERENCES weekly_off_rule (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'weekly off rule -> state / HQ / employee';

USE sfa_demo;
CREATE TABLE IF NOT EXISTS weekly_off_rule (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  rule_name  VARCHAR(80)  NULL COMMENT 'optional label; empty = built from the days, e.g. "3rd Saturday"',
  weekdays   VARCHAR(30)  NOT NULL COMMENT 'comma list of MON,TUE,WED,THU,FRI,SAT,SUN',
  weeks      VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL = every week; or a list of 1,2,3,4,5 (nth of the month) and L (last), e.g. "2,4"',
  remarks    VARCHAR(250) NULL,
  is_active  TINYINT(1)   NOT NULL DEFAULT 1,
  created_by VARCHAR(40)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(40)  NULL,
  updated_at DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'recurring weekly off (3rd Saturday, Saturday + Sunday ...); no row in weekly_off_assignment = everyone';
CREATE TABLE IF NOT EXISTS weekly_off_assignment (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id      INT UNSIGNED NOT NULL COMMENT '-> weekly_off_rule.id',
  target_type  VARCHAR(5)   NOT NULL COMMENT 'STATE / HQ / EMP',
  target_value VARCHAR(40)  NOT NULL COMMENT 'state oid / hq oid / emp_id',
  PRIMARY KEY (id),
  UNIQUE KEY uq_wo_target (rule_id, target_type, target_value),
  KEY ix_wo_target (target_type, target_value),
  CONSTRAINT fk_woa_rule FOREIGN KEY (rule_id) REFERENCES weekly_off_rule (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'weekly off rule -> state / HQ / employee';
