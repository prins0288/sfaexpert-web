package in.opt.sfa.tenant.dcr.view.service;

import in.opt.sfa.tenant.dcr.view.repository.DcrReportRow;
import in.opt.sfa.tenant.dcr.view.repository.DcrRepository;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** DCR report: a user sees his own calls, a manager his + his team's, an admin everyone's (TeamScope). */
@Service
public class DcrReportService {

    private final DcrRepository dcrs;
    private final TeamScope scope;

    public DcrReportService(DcrRepository dcrs, TeamScope scope) {
        this.dcrs = dcrs;
        this.scope = scope;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DcrReportRow> report(Long divisionOid, Long areaOid, String empId, LocalDate fromDate, LocalDate toDate) {
        String emp = (empId == null || empId.isBlank()) ? null : empId.trim();
        if (emp != null) scope.require(TeamScope.Scope.TEAM_SELF, emp, "This employee is not in your team");
        List<DcrReportRow> rows = dcrs.report(divisionOid, areaOid, emp, fromDate, toDate);
        return scope.filter(TeamScope.Scope.TEAM_SELF, rows, DcrReportRow::getEmpId);
    }
}
