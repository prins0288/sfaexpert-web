package in.opt.sfa.tenant.report.activity.service;

import in.opt.sfa.tenant.report.activity.dto.ActivityRow;
import in.opt.sfa.tenant.report.activity.dto.ActivitySaveRequest;
import in.opt.sfa.tenant.report.activity.entity.ReportActivity;
import in.opt.sfa.tenant.report.activity.mapper.ReportActivityMapper;
import in.opt.sfa.tenant.report.activity.repository.ReportActivityRepository;
import in.opt.sfa.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic per-report "activity history" — works for ANY report page, keyed by
 * reportKey (e.g. "dcrreport"). Always scoped to the CALLER's own username
 * (from the verified JWT, never client-supplied) so a user only ever sees
 * their own searches, never anyone else's. See static/js/core/activity.js for
 * the reusable frontend half.
 */
@Service
public class ReportActivityService {

    private final ReportActivityRepository activities;
    private final ReportActivityMapper mapper;

    public ReportActivityService(ReportActivityRepository activities, ReportActivityMapper mapper) {
        this.activities = activities;
        this.mapper = mapper;
    }

    /** Repeating the exact same filter selection for the same report UPDATES that
     *  existing row's timestamp instead of inserting a duplicate — it just bumps
     *  back to the top of the recent list. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(ActivitySaveRequest req) {
        String username = currentUser();
        ReportActivity a = activities
                .findFirstByUsernameAndReportKeyAndFiltersJson(username, req.reportKey(), req.filters())
                .orElseGet(ReportActivity::new);
        a.setUsername(username);
        a.setReportKey(req.reportKey());
        a.setReportLabel(req.reportLabel());
        a.setFiltersJson(req.filters());
        a.setSummary(req.summary());
        a.setCreatedAt(LocalDateTime.now());
        activities.save(a);
    }

    /** The CALLER's own last 10 searches for one report — never another user's. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ActivityRow> list(String reportKey) {
        return activities.findTop10ByUsernameAndReportKeyOrderByCreatedAtDesc(currentUser(), reportKey).stream()
                .map(mapper::toRow)
                .toList();
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return u.username();
    }
}
