package in.opt.sfa.tenant.master.zone.repository;

import in.opt.sfa.tenant.master.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findAllByOrderByOidAsc();
    List<Zone> findByStatusOrderByZoneNameAsc(String status);
    Optional<Zone> findByZoneCode(String zoneCode);
}
