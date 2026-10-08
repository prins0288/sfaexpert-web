-- =====================================================================
--  V2 : EMPLOYEE TABLES
--  Rule of thumb used here:
--    * emp_detail          = narrow, HOT table (login, DCR, hierarchy, lists)
--    * 1:1 tables          = PK is emp_id itself (VARCHAR business key,
--                            e.g. "EMP-001" — matches AppUser.emp_id / the
--                            rest of the app) -> no extra surrogate id
--    * 1:N tables          = own BIGINT PK + index on emp_id
--  NULL is allowed wherever legacy (2012+) data may be missing.
--  "Mandatory" from the Excel is enforced in the Spring DTO (@NotNull etc.)
--  for NEW employees, not by NOT NULL (otherwise old data can't be loaded).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. SAFETY BACKUP -- acme_db already has emp_detail (old wide/flat table,
--    currently read by AuthService.resolveEmpInfo) and employee (old
--    login-ish table). Renamed, NOT dropped, so the new tables below get
--    a clean name without losing the existing data/rollback path.
-- ---------------------------------------------------------------------
RENAME TABLE emp_detail TO emp_detail_legacy_backup;
RENAME TABLE employee   TO employee_legacy_backup;

-- ---------------------------------------------------------------------
-- 1. EMPLOYEE CORE  (Excel: Employee_Basic_Details + Mobile + Official Email)
-- ---------------------------------------------------------------------
CREATE TABLE emp_detail (
  emp_id             VARCHAR(40)  NOT NULL COMMENT 'Business key e.g. EMP-001 (matches AppUser.emp_id / JWT emp_id claim)',
  emp_code           VARCHAR(30)  NOT NULL COMMENT 'Employee Code e.g. 005418 (legacy/display code, distinct from emp_id)',
  emp_name           VARCHAR(150) NOT NULL COMMENT 'As per Aadhaar',
  division_id        BIGINT       NULL COMMENT '-> division_master.oid (NULL for a company-wide role like Super Admin)',
  state_id           BIGINT       NULL COMMENT '-> state_master.oid',
  hq_id              BIGINT       NULL COMMENT '-> hq_master.oid',
  designation        BIGINT       NOT NULL COMMENT '-> designation_master.oid',
  emp_level          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'copied from designation, for fast hierarchy filters',
  manager_id         VARCHAR(40)  NULL COMMENT 'Immediate Manager -> emp_detail.emp_id',
  gender             VARCHAR(10)  NULL,
  date_of_joining    DATE         NULL,
  reporting_date     DATE         NULL,
  mobile             CHAR(10)     NULL,
  official_email     VARCHAR(150) NULL,
  department         VARCHAR(100) NULL,
  is_office_staff    BOOLEAN      NOT NULL DEFAULT FALSE,
  is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
  is_confirmed       BOOLEAN      NOT NULL DEFAULT FALSE,
  confirmation_date  DATE         NULL,
  resignation_date   DATE         NULL,
  last_working_date  DATE         NULL,
  photo_path         VARCHAR(255) NULL,
  profile_complete   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'app sets TRUE when all mandatory sections are filled',
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by         VARCHAR(30)  NULL,
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by         VARCHAR(30)  NULL,
  version            INT          NOT NULL DEFAULT 0 COMMENT 'JPA @Version optimistic lock',
  PRIMARY KEY (emp_id),
  UNIQUE KEY uk_emp_code          (emp_code),
  UNIQUE KEY uk_emp_official_mail (official_email),
  KEY idx_emp_manager            (manager_id, is_active),
  KEY idx_emp_div_state_hq       (division_id, state_id, hq_id, is_active),
  KEY idx_emp_designation        (designation),
  KEY idx_emp_hq                 (hq_id),
  KEY idx_emp_state              (state_id),
  KEY idx_emp_active_level       (is_active, emp_level),
  KEY idx_emp_mobile             (mobile),
  KEY idx_emp_name               (emp_name),
  CONSTRAINT fk_emp_division    FOREIGN KEY (division_id)    REFERENCES division_master (oid),
  CONSTRAINT fk_emp_state       FOREIGN KEY (state_id)       REFERENCES state_master (oid),
  CONSTRAINT fk_emp_hq          FOREIGN KEY (hq_id)          REFERENCES hq_master (oid),
  CONSTRAINT fk_emp_designation FOREIGN KEY (designation) REFERENCES designation_master (oid),
  CONSTRAINT fk_emp_manager     FOREIGN KEY (manager_id)     REFERENCES emp_detail (emp_id),
  CONSTRAINT chk_emp_gender  CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER')),
  CONSTRAINT chk_emp_mobile  CHECK (mobile IS NULL OR REGEXP_LIKE(mobile, '^[6-9][0-9]{9}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ---------------------------------------------------------------------
-- 2. LOGIN (1:1)  -- separate so the password hash is never loaded with
--                    employee lists; login query hits only this table.
-- ---------------------------------------------------------------------
CREATE TABLE emp_login (
  emp_id                VARCHAR(40)  NOT NULL,
  login_id              VARCHAR(30)  NOT NULL COMMENT 'Auto generated',
  password_hash         VARCHAR(100) NULL     COMMENT 'BCrypt/Argon2 hash. NEVER plain text. NULL = must set password',
  must_change_password  BOOLEAN      NOT NULL DEFAULT TRUE,
  password_changed_at   DATETIME(3)  NULL,
  failed_attempts       TINYINT UNSIGNED NOT NULL DEFAULT 0,
  locked_until          DATETIME(3)  NULL,
  force_logout          BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'legacy isLogout',
  last_login_at         DATETIME(3)  NULL,
  app_version           INT          NOT NULL DEFAULT 0,
  created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version               INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (emp_id),
  UNIQUE KEY uk_login_id (login_id),
  CONSTRAINT fk_login_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 3. PERSONAL (1:1)  (Excel: Personal_Details + personal contact + experience summary)
-- ---------------------------------------------------------------------
CREATE TABLE emp_personal (
  emp_id                VARCHAR(40)  NOT NULL,
  dob                   DATE         NULL COMMENT 'As per Aadhaar',
  father_name           VARCHAR(150) NULL,
  mother_name           VARCHAR(150) NULL,
  marital_status        VARCHAR(10)  NULL,
  anniversary_date      DATE         NULL,
  spouse_name           VARCHAR(150) NULL,
  qualification_id      BIGINT UNSIGNED NULL COMMENT '-> degree_master.id',
  qualification_detail  VARCHAR(150) NULL COMMENT 'e.g. B.Pharm / free text when Others',
  blood_group           VARCHAR(3)   NULL,
  personal_email        VARCHAR(150) NULL,
  landline_no           VARCHAR(20)  NULL,
  total_experience_yrs  DECIMAL(4,1) NULL COMMENT 'Excel: Experience (years)',
  shirt_size            TINYINT UNSIGNED NULL,
  remarks               VARCHAR(1000) NULL,
  created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version               INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (emp_id),
  UNIQUE KEY uk_personal_email (personal_email),
  KEY idx_personal_dob (dob),
  KEY idx_personal_anniv (anniversary_date),
  KEY idx_personal_qual (qualification_id),
  CONSTRAINT fk_personal_emp  FOREIGN KEY (emp_id)           REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_personal_qual FOREIGN KEY (qualification_id) REFERENCES degree_master (id),
  CONSTRAINT chk_personal_marital CHECK (marital_status IS NULL OR marital_status IN ('SINGLE','MARRIED')),
  CONSTRAINT chk_personal_blood   CHECK (blood_group IS NULL OR blood_group IN ('A+','A-','B+','B-','AB+','AB-','O+','O-'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 4. CHILDREN (1:N)   Excel has Child 1 / Child 2 -> rows, so 3rd child needs no ALTER
-- ---------------------------------------------------------------------
CREATE TABLE emp_child (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  emp_id      VARCHAR(40)  NOT NULL,
  child_name  VARCHAR(150) NOT NULL,
  gender      VARCHAR(10)  NULL,
  dob         DATE         NULL,
  sort_order  TINYINT UNSIGNED NOT NULL DEFAULT 1,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_child_emp (emp_id, sort_order),
  CONSTRAINT fk_child_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT chk_child_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 5. ADDRESS (1:N, one row per type)  Present / Permanent
-- ---------------------------------------------------------------------
CREATE TABLE emp_address (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id        VARCHAR(40)   NOT NULL,
  address_type  VARCHAR(10)   NOT NULL,
  address_line  VARCHAR(500)  NOT NULL COMMENT 'Full address (free text, as in Excel)',
  city          VARCHAR(100)  NULL,
  district      VARCHAR(100)  NULL,
  state_id      BIGINT        NULL COMMENT '-> state_master.oid',
  pincode       CHAR(6)       NULL,
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_address_emp_type (emp_id, address_type),
  KEY idx_address_state (state_id),
  CONSTRAINT fk_address_emp   FOREIGN KEY (emp_id)   REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_address_state FOREIGN KEY (state_id) REFERENCES state_master (oid),
  CONSTRAINT chk_address_type CHECK (address_type IN ('PRESENT','PERMANENT')),
  CONSTRAINT chk_address_pin  CHECK (pincode IS NULL OR REGEXP_LIKE(pincode, '^[1-9][0-9]{5}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 6. PREVIOUS EMPLOYMENT (1:N)
-- ---------------------------------------------------------------------
CREATE TABLE emp_prev_employment (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id           VARCHAR(40)   NOT NULL,
  company_name     VARCHAR(200)  NOT NULL,
  designation      VARCHAR(100)  NULL,
  from_date        DATE          NULL,
  to_date          DATE          NULL,
  experience_yrs   DECIMAL(4,1)  NULL,
  last_ctc         DECIMAL(12,2) NULL COMMENT 'Annual CTC, numeric only',
  created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_prev_emp (emp_id),
  CONSTRAINT fk_prev_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT chk_prev_ctc   CHECK (last_ctc IS NULL OR last_ctc >= 0),
  CONSTRAINT chk_prev_dates CHECK (to_date IS NULL OR from_date IS NULL OR to_date >= from_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 7. STATUTORY (1:1)  -- sensitive. Aadhaar stored ENCRYPTED (app-side AES-GCM),
--    plus HMAC hash for duplicate check and last-4 for display.
-- ---------------------------------------------------------------------
CREATE TABLE emp_statutory (
  emp_id               VARCHAR(40)   NOT NULL,
  pan_no               CHAR(10)      NULL,
  pf_no                VARCHAR(30)   NULL,
  uan_no               CHAR(12)      NULL,
  esi_no               VARCHAR(20)   NULL,
  aadhaar_enc          VARBINARY(128) NULL COMMENT 'AES-256-GCM ciphertext (iv+data+tag)',
  aadhaar_hash         BINARY(32)    NULL COMMENT 'HMAC-SHA256(aadhaar) for uniqueness / search',
  aadhaar_last4        CHAR(4)       NULL COMMENT 'for masked display XXXX-XXXX-1234',
  mediclaim_policy_no  VARCHAR(50)   NULL COMMENT 'GP Mediclaim Insurance Number',
  created_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version              INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (emp_id),
  UNIQUE KEY uk_stat_pan     (pan_no),
  UNIQUE KEY uk_stat_uan     (uan_no),
  UNIQUE KEY uk_stat_pf      (pf_no),
  UNIQUE KEY uk_stat_esi     (esi_no),
  UNIQUE KEY uk_stat_aadhaar (aadhaar_hash),
  CONSTRAINT fk_stat_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT chk_stat_pan CHECK (pan_no IS NULL OR REGEXP_LIKE(pan_no, '^[A-Z]{5}[0-9]{4}[A-Z]$', 'c')),
  CONSTRAINT chk_stat_uan CHECK (uan_no IS NULL OR REGEXP_LIKE(uan_no, '^[0-9]{12}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 8. BANK ACCOUNTS (1:N)  Bank 1 = primary, Bank 2 = optional.
--    account_no is VARCHAR: BIGINT (old table) drops leading zeros.
--    Generated column trick => max ONE primary account per employee.
-- ---------------------------------------------------------------------
CREATE TABLE emp_bank_account (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id          VARCHAR(40)   NOT NULL,
  bank_id         BIGINT UNSIGNED NULL COMMENT '-> bank_master.id',
  ifsc_code       CHAR(11)      NULL,
  account_no      VARCHAR(20)   NOT NULL,
  branch_name     VARCHAR(150)  NULL,
  is_primary      BOOLEAN       NOT NULL DEFAULT FALSE,
  is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
  primary_marker  TINYINT       AS (IF(is_primary AND is_active, 1, NULL)) STORED COMMENT 'not mapped in JPA',
  created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_one_primary (emp_id, primary_marker),
  UNIQUE KEY uk_bank_emp_acc     (emp_id, account_no, ifsc_code),
  KEY idx_bank_bank (bank_id),
  CONSTRAINT fk_bankacc_emp  FOREIGN KEY (emp_id)  REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_bankacc_bank FOREIGN KEY (bank_id) REFERENCES bank_master (id),
  CONSTRAINT chk_bank_ifsc CHECK (ifsc_code IS NULL OR REGEXP_LIKE(ifsc_code, '^[A-Z]{4}0[A-Z0-9]{6}$', 'c')),
  CONSTRAINT chk_bank_acc  CHECK (REGEXP_LIKE(account_no, '^[0-9]{6,20}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 9. EMERGENCY CONTACTS (1:N)
-- ---------------------------------------------------------------------
CREATE TABLE emp_emergency_contact (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id           VARCHAR(40)   NOT NULL,
  contact_name     VARCHAR(150)  NULL     COMMENT 'NULL only for legacy rows',
  relationship_id  INT           NULL,
  contact_no_1     VARCHAR(15)   NOT NULL,
  contact_no_2     VARCHAR(15)   NULL,
  priority         TINYINT UNSIGNED NOT NULL DEFAULT 1,
  created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_emerg_emp (emp_id, priority),
  KEY idx_emerg_rel (relationship_id),
  CONSTRAINT fk_emerg_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id)
  -- no FK on relationship_id: no relationship master table exists yet in acme_db
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 10. NOMINEES (1:N)  ESI / PF / GRATUITY / MEDICLAIM in ONE table.
--     Multiple nominees per type allowed; SUM(share_pct)=100 per type
--     is validated in the service layer before commit.
-- ---------------------------------------------------------------------
CREATE TABLE emp_nominee (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id           VARCHAR(40)   NOT NULL,
  nominee_type     VARCHAR(12)   NOT NULL,
  nominee_name     VARCHAR(150)  NOT NULL,
  relationship_id  INT           NULL,
  nominee_dob      DATE          NULL,
  contact_no       CHAR(10)      NULL,
  share_pct        DECIMAL(5,2)  NOT NULL DEFAULT 100.00,
  created_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_nominee_emp_type (emp_id, nominee_type),
  KEY idx_nominee_rel (relationship_id),
  CONSTRAINT fk_nominee_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  -- no FK on relationship_id: no relationship master table exists yet in acme_db
  CONSTRAINT chk_nominee_type  CHECK (nominee_type IN ('ESI','PF','GRATUITY','MEDICLAIM')),
  CONSTRAINT chk_nominee_share CHECK (share_pct > 0 AND share_pct <= 100),
  CONSTRAINT chk_nominee_mob   CHECK (contact_no IS NULL OR REGEXP_LIKE(contact_no, '^[0-9]{10}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 11. APP & EXPENSE SETTINGS (1:1)
-- ---------------------------------------------------------------------
CREATE TABLE emp_app_setting (
  emp_id                VARCHAR(40)  NOT NULL,
  expense_group_id      INT          NULL,
  app_access_enabled    BOOLEAN      NOT NULL DEFAULT TRUE  COMMENT 'Reporting App Access Enable/Disable',
  stop_reporting        BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Start/Stop reporting: TRUE = cannot submit DCR',
  geo_fencing_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
  lock_period_days      SMALLINT UNSIGNED NOT NULL DEFAULT 20,
  lock_web_dcr          BOOLEAN      NOT NULL DEFAULT FALSE,
  all_day_working       BOOLEAN      NOT NULL DEFAULT FALSE,
  auto_mail             BOOLEAN      NOT NULL DEFAULT FALSE,
  is_starsfa            BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  updated_by            VARCHAR(30)  NULL,
  version               INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (emp_id),
  KEY idx_app_expense (expense_group_id),
  CONSTRAINT fk_app_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id)
  -- no FK on expense_group_id: no expense-group master table exists yet in acme_db
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 12. DOCUMENTS (1:N) -- file lives in S3/MinIO/disk; DB keeps only metadata.
-- ---------------------------------------------------------------------
CREATE TABLE emp_document (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id            VARCHAR(40)   NOT NULL,
  document_type_id  BIGINT UNSIGNED NOT NULL COMMENT '-> document_master.id',
  original_name     VARCHAR(255)  NOT NULL,
  storage_key       VARCHAR(500)  NOT NULL COMMENT 'object key / relative path',
  mime_type         VARCHAR(100)  NULL,
  file_size_bytes   INT UNSIGNED  NULL,
  sha256            CHAR(64)      NULL,
  is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
  uploaded_by       VARCHAR(30)   NULL,
  uploaded_at       DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_doc_emp_type (emp_id, document_type_id, is_active),
  KEY idx_doc_type (document_type_id),
  CONSTRAINT fk_doc_emp  FOREIGN KEY (emp_id)           REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES document_master (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 13. CREDENTIAL DISPATCH LOG (WhatsApp / Email after Emp ID creation)
--     Never store the password here; send a one-time set-password link/OTP.
-- ---------------------------------------------------------------------
CREATE TABLE emp_credential_dispatch (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  emp_id           VARCHAR(40)   NOT NULL,
  channel          VARCHAR(10)   NOT NULL,
  sent_to          VARCHAR(150)  NOT NULL,
  dispatch_status  VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
  provider_msg_id  VARCHAR(100)  NULL,
  error_message    VARCHAR(500)  NULL,
  retry_count      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  requested_by     VARCHAR(30)   NULL,
  requested_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  sent_at          DATETIME(3)   NULL,
  PRIMARY KEY (id),
  KEY idx_disp_emp (emp_id, requested_at),
  KEY idx_disp_status (dispatch_status, requested_at) COMMENT 'retry worker picks PENDING/FAILED',
  CONSTRAINT fk_disp_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),
  CONSTRAINT chk_disp_channel CHECK (channel IN ('WHATSAPP','EMAIL','SMS')),
  CONSTRAINT chk_disp_status  CHECK (dispatch_status IN ('PENDING','SENT','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 14. ASSIGNMENT HISTORY (1:N) -- transfers / promotions / manager change.
--     Answers "who was X's manager / HQ on date D" for old DCR reports.
-- ---------------------------------------------------------------------
CREATE TABLE emp_assignment_history (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  emp_id          VARCHAR(40)  NOT NULL,
  division_id     BIGINT       NOT NULL,
  state_id        BIGINT       NULL,
  hq_id           BIGINT       NULL,
  designation_id  BIGINT       NOT NULL,
  manager_id      VARCHAR(40)  NULL,
  effective_from  DATE         NOT NULL,
  effective_to    DATE         NULL COMMENT 'NULL = current',
  change_reason   VARCHAR(30)  NULL COMMENT 'JOINING / TRANSFER / PROMOTION / MANAGER_CHANGE / MIGRATION',
  created_by      VARCHAR(30)  NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_hist_emp_from (emp_id, effective_from),
  KEY idx_hist_mgr (manager_id, effective_from),
  KEY idx_hist_hq (hq_id, effective_from),
  CONSTRAINT fk_hist_emp FOREIGN KEY (emp_id)         REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_hist_mgr FOREIGN KEY (manager_id)     REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_hist_div FOREIGN KEY (division_id)    REFERENCES division_master (oid),
  CONSTRAINT fk_hist_st  FOREIGN KEY (state_id)       REFERENCES state_master (oid),
  CONSTRAINT fk_hist_hq  FOREIGN KEY (hq_id)          REFERENCES hq_master (oid),
  CONSTRAINT fk_hist_des FOREIGN KEY (designation_id) REFERENCES designation_master (oid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 15. HIERARCHY CLOSURE TABLE -- "all employees under manager M" in ONE
--     indexed query instead of recursive loops (SFA team reports).
--     Every employee has a self row (depth 0).
-- ---------------------------------------------------------------------
CREATE TABLE emp_hierarchy (
  ancestor_id    VARCHAR(40) NOT NULL,
  descendant_id  VARCHAR(40) NOT NULL,
  depth          TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (ancestor_id, descendant_id),
  KEY idx_hier_desc (descendant_id, depth),
  CONSTRAINT fk_hier_anc  FOREIGN KEY (ancestor_id)   REFERENCES emp_detail (emp_id),
  CONSTRAINT fk_hier_desc FOREIGN KEY (descendant_id) REFERENCES emp_detail (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
