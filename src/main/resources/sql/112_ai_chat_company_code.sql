-- 112: ai_chat.tenant_id -> company_code (run against sfa_central).
-- Script 60 created the column as tenant_id but the app (AiChatLog entity) writes company_code, so every AI turn failed to log:
--   "Unknown column 'company_code' in 'field list'". This renames the column (keeping the data). Safe to re-run.

USE sfa_central;

SET @has_old := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_chat' AND column_name = 'tenant_id');
SET @has_new := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_chat' AND column_name = 'company_code');

-- tenant_id only: rename it
SET @s := IF(@has_old > 0 AND @has_new = 0, 'ALTER TABLE ai_chat CHANGE COLUMN tenant_id company_code VARCHAR(64) NOT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- both exist (company_code was added by hand): keep the data in company_code, drop the old column
SET @s := IF(@has_old > 0 AND @has_new > 0, 'UPDATE ai_chat SET company_code = tenant_id WHERE company_code IS NULL OR company_code = \'\'', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF(@has_old > 0 AND @has_new > 0, 'ALTER TABLE ai_chat DROP COLUMN tenant_id', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- no table at all: create it as the app expects
CREATE TABLE IF NOT EXISTS ai_chat (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(64)  NOT NULL,
    emp_id       VARCHAR(40)  NULL,
    prompt       TEXT         NULL,
    response     MEDIUMTEXT   NULL,
    model        VARCHAR(64)  NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_chat_company_emp (company_code, emp_id),
    KEY idx_ai_chat_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
