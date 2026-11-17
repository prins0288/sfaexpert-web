package in.opt.sfa.tenant.master.empdetail.repository;

import in.opt.sfa.tenant.master.empdetail.entity.EmpAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpAssignmentHistoryRepository extends JpaRepository<EmpAssignmentHistory, Long> {
    List<EmpAssignmentHistory> findByEmpIdOrderByEffectiveFromAscIdAsc(String empId);
    Optional<EmpAssignmentHistory> findFirstByEmpIdAndEffectiveToIsNullOrderByEffectiveFromDescIdDesc(String empId);
}
