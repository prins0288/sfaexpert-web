-- ===========================================================================
-- report_activity — generic per-user, per-report search history (TENANT db).
-- One row per search a user actually ran on any report; the report page's own
-- JS (js/core/activity.js) can replay one by re-applying filters_json and
-- re-running the search — same mechanism for every report, keyed by
-- report_key. Always scoped server-side to the caller's own username, so a
-- user only ever sees their own past searches.
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/23_report_activity.sql
-- ===========================================================================

USE acme_db;

CREATE TABLE IF NOT EXISTS report_activity (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(128) NOT NULL,
    report_key    VARCHAR(64)  NOT NULL,
    report_label  VARCHAR(160) NULL,
    filters_json  TEXT         NULL,
    summary       VARCHAR(500) NULL,
    created_at    DATETIME     NOT NULL,
    KEY idx_report_activity_user_report (username, report_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
