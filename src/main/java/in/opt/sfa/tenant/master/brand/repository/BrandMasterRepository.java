package in.opt.sfa.tenant.master.brand.repository;

import in.opt.sfa.tenant.master.brand.entity.BrandMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface BrandMasterRepository extends JpaRepository<BrandMaster, Long> {
    List<BrandMaster> findAllByOrderByBrandNameAsc();
    List<BrandMaster> findByStatusTrueOrderByBrandNameAsc();
    Optional<BrandMaster> findByBrandCode(String brandCode);
    Optional<BrandMaster> findByBrandName(String brandName);
}
