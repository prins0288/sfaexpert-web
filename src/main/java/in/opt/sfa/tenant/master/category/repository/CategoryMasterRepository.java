package in.opt.sfa.tenant.master.category.repository;

import in.opt.sfa.tenant.master.category.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface CategoryMasterRepository extends JpaRepository<CategoryMaster, Long> {

    List<CategoryMaster> findAllByOrderByOidAsc();

    List<CategoryMaster> findByStatusOrderByCategoryNameAsc(Boolean status);

    Optional<CategoryMaster> findByCategoryCode(String categoryCode);
}
