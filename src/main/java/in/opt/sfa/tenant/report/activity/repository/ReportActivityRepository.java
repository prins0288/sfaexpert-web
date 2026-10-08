package in.opt.sfa.tenant.report.activity.repository;

import in.opt.sfa.tenant.report.activity.entity.ReportActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportActivityRepository extends JpaRepository<ReportActivity, Long> {
    List<ReportActivity> findTop10ByUsernameAndReportKeyOrderByCreatedAtDesc(String username, String reportKey);

    /** Same user + same report + IDENTICAL filter selection already saved — used to
     *  bump/update that row instead of inserting a duplicate for a repeated search. */
    Optional<ReportActivity> findFirstByUsernameAndReportKeyAndFiltersJson(String username, String reportKey, String filtersJson);
}
