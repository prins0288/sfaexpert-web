-- 97: tour_emp_delete_log (every deleted plan: who, when, why, full content) and tour_emp_change_log (what an approver changed on a date: who, when, before / after). Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS tour_emp_delete_log (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main              BIGINT UNSIGNED NOT NULL COMMENT 'id of the deleted plan (tour_emp_parent.tour_main) - no foreign key, the plan is gone',
  emp_id                 VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan it was',
  emp_name               VARCHAR(150) NULL,
  tour_year              SMALLINT UNSIGNED NOT NULL,
  tour_month             TINYINT UNSIGNED NOT NULL,
  status_at_delete       VARCHAR(20) NOT NULL COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED when it was deleted',
  planned_days           INT NOT NULL DEFAULT 0,
  deleted_by             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'manager / admin who deleted it',
  deleted_by_name        VARCHAR(150) NULL,
  deleted_by_designation VARCHAR(100) NULL,
  deleted_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when',
  reason                 VARCHAR(255) NOT NULL COMMENT 'why (required)',
  snapshot               JSON NULL COMMENT 'the whole plan as it was: days, areas, clients, visit with, approval trail',
  PRIMARY KEY (id),
  KEY ix_tdl_emp (emp_id, tour_year, tour_month),
  KEY ix_tdl_when (deleted_at),
  KEY ix_tdl_by (deleted_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'record of every tour plan that was deleted: who, when, why, and what it contained';

CREATE TABLE IF NOT EXISTS tour_emp_change_log (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main            BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date            DATE         NOT NULL COMMENT 'the planned date that was changed',
  change_action        VARCHAR(10)  NOT NULL COMMENT 'ADDED (day was empty) / CHANGED / REMOVED',
  changed_by           VARCHAR(40)  NOT NULL COMMENT 'manager / admin who changed it while approving',
  changed_by_name      VARCHAR(150) NULL,
  changed_by_designation VARCHAR(100) NULL,
  changed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  summary              JSON         NULL COMMENT 'lines: + added, - removed, ~ changed (work type, areas, clients, visit with, stay, remark)',
  before_day           JSON         NULL COMMENT 'the day before the change',
  after_day            JSON         NULL COMMENT 'the day after the change',
  PRIMARY KEY (id),
  KEY ix_tcl_plan_date (tour_main, tour_date),
  CONSTRAINT fk_tcg_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'what a manager / admin changed on a date of a submitted plan: who, when, before and after';

USE globex_db;
CREATE TABLE IF NOT EXISTS tour_emp_delete_log (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main              BIGINT UNSIGNED NOT NULL COMMENT 'id of the deleted plan (tour_emp_parent.tour_main) - no foreign key, the plan is gone',
  emp_id                 VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan it was',
  emp_name               VARCHAR(150) NULL,
  tour_year              SMALLINT UNSIGNED NOT NULL,
  tour_month             TINYINT UNSIGNED NOT NULL,
  status_at_delete       VARCHAR(20) NOT NULL COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED when it was deleted',
  planned_days           INT NOT NULL DEFAULT 0,
  deleted_by             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'manager / admin who deleted it',
  deleted_by_name        VARCHAR(150) NULL,
  deleted_by_designation VARCHAR(100) NULL,
  deleted_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when',
  reason                 VARCHAR(255) NOT NULL COMMENT 'why (required)',
  snapshot               JSON NULL COMMENT 'the whole plan as it was: days, areas, clients, visit with, approval trail',
  PRIMARY KEY (id),
  KEY ix_tdl_emp (emp_id, tour_year, tour_month),
  KEY ix_tdl_when (deleted_at),
  KEY ix_tdl_by (deleted_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'record of every tour plan that was deleted: who, when, why, and what it contained';

CREATE TABLE IF NOT EXISTS tour_emp_change_log (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main            BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date            DATE         NOT NULL COMMENT 'the planned date that was changed',
  change_action        VARCHAR(10)  NOT NULL COMMENT 'ADDED (day was empty) / CHANGED / REMOVED',
  changed_by           VARCHAR(40)  NOT NULL COMMENT 'manager / admin who changed it while approving',
  changed_by_name      VARCHAR(150) NULL,
  changed_by_designation VARCHAR(100) NULL,
  changed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  summary              JSON         NULL COMMENT 'lines: + added, - removed, ~ changed (work type, areas, clients, visit with, stay, remark)',
  before_day           JSON         NULL COMMENT 'the day before the change',
  after_day            JSON         NULL COMMENT 'the day after the change',
  PRIMARY KEY (id),
  KEY ix_tcl_plan_date (tour_main, tour_date),
  CONSTRAINT fk_tcg_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'what a manager / admin changed on a date of a submitted plan: who, when, before and after';

USE initech_db;
CREATE TABLE IF NOT EXISTS tour_emp_delete_log (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main              BIGINT UNSIGNED NOT NULL COMMENT 'id of the deleted plan (tour_emp_parent.tour_main) - no foreign key, the plan is gone',
  emp_id                 VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan it was',
  emp_name               VARCHAR(150) NULL,
  tour_year              SMALLINT UNSIGNED NOT NULL,
  tour_month             TINYINT UNSIGNED NOT NULL,
  status_at_delete       VARCHAR(20) NOT NULL COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED when it was deleted',
  planned_days           INT NOT NULL DEFAULT 0,
  deleted_by             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'manager / admin who deleted it',
  deleted_by_name        VARCHAR(150) NULL,
  deleted_by_designation VARCHAR(100) NULL,
  deleted_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when',
  reason                 VARCHAR(255) NOT NULL COMMENT 'why (required)',
  snapshot               JSON NULL COMMENT 'the whole plan as it was: days, areas, clients, visit with, approval trail',
  PRIMARY KEY (id),
  KEY ix_tdl_emp (emp_id, tour_year, tour_month),
  KEY ix_tdl_when (deleted_at),
  KEY ix_tdl_by (deleted_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'record of every tour plan that was deleted: who, when, why, and what it contained';

CREATE TABLE IF NOT EXISTS tour_emp_change_log (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main            BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date            DATE         NOT NULL COMMENT 'the planned date that was changed',
  change_action        VARCHAR(10)  NOT NULL COMMENT 'ADDED (day was empty) / CHANGED / REMOVED',
  changed_by           VARCHAR(40)  NOT NULL COMMENT 'manager / admin who changed it while approving',
  changed_by_name      VARCHAR(150) NULL,
  changed_by_designation VARCHAR(100) NULL,
  changed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  summary              JSON         NULL COMMENT 'lines: + added, - removed, ~ changed (work type, areas, clients, visit with, stay, remark)',
  before_day           JSON         NULL COMMENT 'the day before the change',
  after_day            JSON         NULL COMMENT 'the day after the change',
  PRIMARY KEY (id),
  KEY ix_tcl_plan_date (tour_main, tour_date),
  CONSTRAINT fk_tcg_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'what a manager / admin changed on a date of a submitted plan: who, when, before and after';

USE sfa_demo;
CREATE TABLE IF NOT EXISTS tour_emp_delete_log (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main              BIGINT UNSIGNED NOT NULL COMMENT 'id of the deleted plan (tour_emp_parent.tour_main) - no foreign key, the plan is gone',
  emp_id                 VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'whose plan it was',
  emp_name               VARCHAR(150) NULL,
  tour_year              SMALLINT UNSIGNED NOT NULL,
  tour_month             TINYINT UNSIGNED NOT NULL,
  status_at_delete       VARCHAR(20) NOT NULL COMMENT 'DRAFT / PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED when it was deleted',
  planned_days           INT NOT NULL DEFAULT 0,
  deleted_by             VARCHAR(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'manager / admin who deleted it',
  deleted_by_name        VARCHAR(150) NULL,
  deleted_by_designation VARCHAR(100) NULL,
  deleted_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when',
  reason                 VARCHAR(255) NOT NULL COMMENT 'why (required)',
  snapshot               JSON NULL COMMENT 'the whole plan as it was: days, areas, clients, visit with, approval trail',
  PRIMARY KEY (id),
  KEY ix_tdl_emp (emp_id, tour_year, tour_month),
  KEY ix_tdl_when (deleted_at),
  KEY ix_tdl_by (deleted_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'record of every tour plan that was deleted: who, when, why, and what it contained';

CREATE TABLE IF NOT EXISTS tour_emp_change_log (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tour_main            BIGINT UNSIGNED NOT NULL COMMENT '-> tour_emp_parent',
  tour_date            DATE         NOT NULL COMMENT 'the planned date that was changed',
  change_action        VARCHAR(10)  NOT NULL COMMENT 'ADDED (day was empty) / CHANGED / REMOVED',
  changed_by           VARCHAR(40)  NOT NULL COMMENT 'manager / admin who changed it while approving',
  changed_by_name      VARCHAR(150) NULL,
  changed_by_designation VARCHAR(100) NULL,
  changed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  summary              JSON         NULL COMMENT 'lines: + added, - removed, ~ changed (work type, areas, clients, visit with, stay, remark)',
  before_day           JSON         NULL COMMENT 'the day before the change',
  after_day            JSON         NULL COMMENT 'the day after the change',
  PRIMARY KEY (id),
  KEY ix_tcl_plan_date (tour_main, tour_date),
  CONSTRAINT fk_tcg_parent FOREIGN KEY (tour_main) REFERENCES tour_emp_parent (tour_main) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'what a manager / admin changed on a date of a submitted plan: who, when, before and after';
