-- ============================================================================
-- 50_emp_detail.sql  —  Employee master (production/DBA-grade redesign)
--
-- Redesigned (not a 1:1 copy) of the legacy `emp_detail`:
--   * surrogate BIGINT auto-increment PK + UNIQUE emp_code (fast InnoDB clustered
--     index, small secondary indexes) instead of a VARCHAR(225) PK,
--   * utf8mb4 (was latin1), NULL for optional (no ''/'1900-01-01' sentinels),
--   * right-sized types (account no as VARCHAR, salary DECIMAL, ids/levels numeric,
--     flags TINYINT(1), gender/status ENUM),
--   * lean, purpose-built indexes (dropped the useless single-column ones),
--   * login PASSWORD intentionally NOT stored here (auth lives in the common
--     app_user table as a BCrypt hash — never plaintext).
--
-- Columns marked [VERIFY] carry my best-guess meaning of an ambiguous legacy
-- column; rename with a quick ALTER once confirmed.
-- ============================================================================

CREATE TABLE IF NOT EXISTS emp_detail (
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    emp_code                 VARCHAR(30)   NOT NULL,                       -- business key (was emp_id / PK)
    emp_name                 VARCHAR(150)  NOT NULL,
    father_name              VARCHAR(150)  NULL,

    -- ---- organisation / hierarchy ----
    division_id              VARCHAR(10)   NULL,                           -- division code
    division_name            VARCHAR(150)  NULL,
    designation_oid          BIGINT        NULL,                           -- -> designation_master.oid
    emp_level                TINYINT UNSIGNED NULL,                        -- hierarchy level 1..N
    reporting_manager_code   VARCHAR(30)   NULL,                           -- -> emp_detail.emp_code (manager)
    department               VARCHAR(120)  NULL,
    working_state_id         INT UNSIGNED  NULL,                           -- was state_main (assigned state id)
    working_district_id      INT UNSIGNED  NULL,                           -- was distrect_id (assigned district/HQ id)
    mapped_emp_code          VARCHAR(30)   NULL,                           -- was map_dis (holds an employee code)

    -- ---- personal ----
    gender                   ENUM('Male','Female','Other') NULL,
    marital_status           VARCHAR(20)   NULL,
    date_of_birth            DATE          NULL,
    anniversary_date         DATE          NULL,
    qualification            VARCHAR(150)  NULL,
    city                     VARCHAR(120)  NULL,
    correspondence_address   VARCHAR(500)  NULL,
    permanent_address        VARCHAR(500)  NULL,

    -- ---- contact ----
    mobile_number            VARCHAR(15)   NULL,
    phone_number             VARCHAR(20)   NULL,
    email                    VARCHAR(120)  NULL,
    emergency_contact_number VARCHAR(15)   NULL,
    nominee_name             VARCHAR(120)  NULL,
    nominee_relationship     VARCHAR(30)   NULL,

    -- ---- statutory / government ids ----
    pan_number               CHAR(10)      NULL,
    pf_number                VARCHAR(30)   NULL,
    esi_number               VARCHAR(30)   NULL,
    uan_number               VARCHAR(20)   NULL,
    other_id_card_no         VARCHAR(50)   NULL,                           -- [VERIFY] was other_card_no

    -- ---- bank ----
    bank_account_number      VARCHAR(20)   NULL,                           -- string (leading zeros); was BIGINT
    bank_name                VARCHAR(120)  NULL,
    bank_branch              VARCHAR(120)  NULL,
    ifsc_code                CHAR(11)      NULL,

    -- ---- employment ----
    date_of_joining          DATE          NULL,
    experience               VARCHAR(100)  NULL,
    previous_experience      VARCHAR(500)  NULL,
    salary                   DECIMAL(12,2) NULL,
    confirmation_status      TINYINT       NOT NULL DEFAULT 0,             -- [VERIFY] was emp_confirm_status
    reporting_date           DATE          NULL,                          -- was previous_ass
    photo_path               VARCHAR(255)  NULL,                          -- was imagee_path
    shirt_size               TINYINT UNSIGNED NULL,
    dcr_lock_period_days      SMALLINT UNSIGNED NULL,                      -- [VERIFY] was lock_period

    -- ---- flags ----
    status                   ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
    is_office_staff          TINYINT(1)    NOT NULL DEFAULT 0,
    is_resigned              TINYINT(1)    NOT NULL DEFAULT 0,
    all_day_working          TINYINT(1)    NOT NULL DEFAULT 0,
    stop_reporting           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '1 = user cannot submit DCR',
    auto_mail_enabled        TINYINT(1)    NOT NULL DEFAULT 0,
    is_logged_out            TINYINT(1)    NOT NULL DEFAULT 0,
    app_version              INT           NULL,

    remarks                  TEXT          NULL,

    -- ---- audit ----
    created_by               VARCHAR(30)   NULL,
    updated_by               VARCHAR(30)   NULL,
    created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_emp_detail_code (emp_code),
    KEY idx_emp_detail_manager (reporting_manager_code),
    KEY idx_emp_detail_designation (designation_oid),
    KEY idx_emp_detail_state (working_state_id),
    KEY idx_emp_detail_district (working_district_id),
    KEY idx_emp_detail_status_level (status, emp_level),
    KEY idx_emp_detail_mobile (mobile_number),
    KEY idx_emp_detail_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
