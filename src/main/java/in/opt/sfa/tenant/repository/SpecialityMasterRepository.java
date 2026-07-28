package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.SpecialityMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface SpecialityMasterRepository extends JpaRepository<SpecialityMaster, Long> {

    List<SpecialityMaster> findAllByOrderByOidAsc();

    List<SpecialityMaster> findByStatusOrderBySpecialityNameAsc(Boolean status);

    Optional<SpecialityMaster> findBySpecialityCode(String specialityCode);
}
