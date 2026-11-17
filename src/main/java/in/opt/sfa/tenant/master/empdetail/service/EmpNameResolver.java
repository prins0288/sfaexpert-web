package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves an emp_id (as now stamped into every master's created_by /
 * updated_by — see the entities' currentUser(), which reads the verified
 * JWT's emp_id claim) to that employee's display name, for report/listing
 * pages. One bean shared by every master service instead of each one
 * building its own emp_id -> name map.
 *
 * A value that ISN'T a known emp_id (e.g. "system" for non-request writes,
 * or a stamp from before this conversion) is left as-is rather than blanked,
 * so old data still shows something meaningful.
 */
@Component
public class EmpNameResolver {

    private final EmpDetailRepository employees;

    public EmpNameResolver(EmpDetailRepository employees) {
        this.employees = employees;
    }

    /** emp_id -> employee display name, for every employee in the current tenant. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, String> nameMap() {
        return employees.findAll().stream()
                .filter(e -> e.getEmpId() != null && e.getEmpName() != null)
                .collect(Collectors.toMap(EmpDetail::getEmpId, EmpDetail::getEmpName, (a, b) -> a));
    }

    /** Look up one emp_id in an already-fetched map; unknown values pass through unchanged. */
    public String resolve(Map<String, String> names, String empId) {
        return empId == null ? null : names.getOrDefault(empId, empId);
    }
}
