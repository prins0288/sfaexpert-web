package in.opt.sfa.tenant.master.empdetail.repository;

import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpDetailRepository extends JpaRepository<EmpDetail, String> {
    List<EmpDetail> findAllByOrderByEmpNameAsc();
    Optional<EmpDetail> findByEmpCode(String empCode);
}
