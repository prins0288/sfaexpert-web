package in.opt.sfa.tenant.master.employee.repository;

import in.opt.sfa.tenant.master.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpId(String empId);
    Optional<Employee> findByUsername(String username);
    List<Employee> findAllByOrderByOidAsc();
    List<Employee> findByStatusOrderByEmpNameAsc(String status);
}
