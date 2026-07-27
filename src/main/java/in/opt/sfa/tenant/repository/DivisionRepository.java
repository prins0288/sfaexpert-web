package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Division;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DivisionRepository extends JpaRepository<Division, Long> {
    List<Division> findByStatusOrderByDivisionNameAsc(String status);
}
