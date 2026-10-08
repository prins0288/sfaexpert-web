package in.opt.sfa.tenant.master.degree.repository;

import in.opt.sfa.tenant.master.degree.entity.DegreeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface DegreeMasterRepository extends JpaRepository<DegreeMaster, Long> {

    List<DegreeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<DegreeMaster> findByIsActiveOrderByDisplayOrderAscDegreeNameAsc(Boolean isActive);

    Optional<DegreeMaster> findByDegreeCode(String degreeCode);

    Optional<DegreeMaster> findByDegreeName(String degreeName);
}
