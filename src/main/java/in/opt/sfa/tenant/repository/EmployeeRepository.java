package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpId(String empId);
    List<Employee> findAllByOrderByOidAsc();
}
