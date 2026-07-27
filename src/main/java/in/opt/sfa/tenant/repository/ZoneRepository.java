package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByStatusOrderByZoneNameAsc(String status);
}
