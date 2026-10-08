package in.opt.sfa.tenant.master.imagetype.repository;

import in.opt.sfa.tenant.master.imagetype.entity.ImageTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface ImageTypeMasterRepository extends JpaRepository<ImageTypeMaster, Long> {

    List<ImageTypeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<ImageTypeMaster> findByIsActiveOrderByDisplayOrderAscImageTypeNameAsc(Boolean isActive);

    Optional<ImageTypeMaster> findByImageTypeCode(String imageTypeCode);

    Optional<ImageTypeMaster> findByImageTypeName(String imageTypeName);
}
