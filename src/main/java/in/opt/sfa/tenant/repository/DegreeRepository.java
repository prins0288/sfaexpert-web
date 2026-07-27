package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Degree;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DegreeRepository extends JpaRepository<Degree, Long> {
    List<Degree> findByStatusOrderByDegreeNameAsc(String status);
}
