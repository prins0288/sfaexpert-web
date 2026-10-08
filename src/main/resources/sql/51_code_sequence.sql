-- ============================================================================
-- 51_code_sequence.sql  —  race-free running-number generator (per tenant DB)
--
-- code_sequence hands out unique, gap-tolerant running numbers. Generation is
-- atomic (a single UPDATE takes a row lock and uses LAST_INSERT_ID), so even
-- concurrent inserts can NEVER get the same number -> no duplicate emp_code.
-- The emp_code format (prefix / width / suffix) is per-company, in
-- company_setting_master, so each tenant can have its own (e.g. AWT201, AWT202).
--
-- Run on each tenant DB. (This file seeds acme's example: prefix AWT, start 201.)
-- ============================================================================

CREATE TABLE IF NOT EXISTS code_sequence (
    seq_name  VARCHAR(50)     NOT NULL,
    next_val  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'last handed-out value',
    PRIMARY KEY (seq_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Start emp_code at 201 (next handed out = last(200) + 1). Change per tenant.
INSERT INTO code_sequence (seq_name, next_val) VALUES ('emp_code', 200)
    ON DUPLICATE KEY UPDATE next_val = next_val;

-- Per-company emp_code format: prefix + number (+ optional suffix). width 0 = no zero-padding.
INSERT INTO company_setting_master (setting_key, setting_value, updated_at, updated_by) VALUES
    ('empcode.prefix', 'AWT', NOW(), 'system'),
    ('empcode.width',  '0',   NOW(), 'system'),
    ('empcode.suffix', '',    NOW(), 'system')
    ON DUPLICATE KEY UPDATE setting_value = setting_value;
