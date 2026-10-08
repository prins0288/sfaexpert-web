package in.opt.sfa.tenant.master.traveltype.repository;

import in.opt.sfa.tenant.master.traveltype.entity.TravelTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface TravelTypeMasterRepository extends JpaRepository<TravelTypeMaster, Long> {

    List<TravelTypeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<TravelTypeMaster> findByIsActiveOrderByDisplayOrderAscTravelTypeNameAsc(Boolean isActive);

    Optional<TravelTypeMaster> findByTravelTypeCode(String travelTypeCode);

    Optional<TravelTypeMaster> findByTravelTypeName(String travelTypeName);
}
