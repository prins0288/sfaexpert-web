-- ============================================================================
-- 60_ai_chat.sql   (run against sfa_central)
-- ----------------------------------------------------------------------------
-- Logs every AI assistant turn: the user's prompt and the AI's HTML response,
-- stamped with who asked (emp_id) and which company (tenant_id). Lives in the
-- common db so all tenants' AI history is in one place (queryable centrally).
-- ============================================================================

CREATE TABLE IF NOT EXISTS ai_chat (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(64)  NOT NULL,
    emp_id      VARCHAR(40)  NULL,
    prompt      TEXT         NULL,
    response    MEDIUMTEXT   NULL,
    model       VARCHAR(64)  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_chat_tenant_emp (tenant_id, emp_id),
    KEY idx_ai_chat_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
