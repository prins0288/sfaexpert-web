package in.opt.sfa.tenant.master.division.repository;

import in.opt.sfa.tenant.master.division.entity.Division;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DivisionRepository extends JpaRepository<Division, Long> {
    List<Division> findAllByOrderByOidAsc();
    List<Division> findByStatusOrderByDivisionNameAsc(String status);
    Optional<Division> findByDivisionCode(String divisionCode);
    Optional<Division> findFirstByDivisionCodeOrDivisionName(String divisionCode, String divisionName);
}
