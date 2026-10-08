package in.opt.sfa.tenant.report.activity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per report search a user actually ran — "activity history" so they
 * can replay a past search with one click (see ReportActivityController +
 * static/js/core/activity.js). Scoped per-user (username) AND per-report
 * (reportKey, e.g. "dcrreport" — matches the page's menu_item.page key), so
 * this ONE table + ONE frontend module works for every report, not just DCR
 * — hence living under its own "report.activity" domain rather than nested
 * inside any one report's package.
 *
 * filtersJson is intentionally opaque/schema-less TEXT — each report has a
 * different filter shape; the server never needs to understand it, only store
 * and hand it back for the report page's own JS to re-apply.
 */
@Entity
@Table(name = "report_activity")
@Getter
@Setter
public class ReportActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "report_key", nullable = false, length = 64)
    private String reportKey;

    @Column(name = "report_label", length = 160)
    private String reportLabel;

    @Column(name = "filters_json", columnDefinition = "TEXT")
    private String filtersJson;

    @Column(length = 500)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
