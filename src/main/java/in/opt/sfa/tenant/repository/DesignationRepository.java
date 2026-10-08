package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation> findByStatusTrueOrderByDesignationNameAsc();
    List<Designation> findAllByOrderByEmpLevelDescDesignationNameAsc();
    Optional<Designation> findByDesignationCode(String designationCode);
}
