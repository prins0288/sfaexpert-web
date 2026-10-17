package in.opt.sfa.tenant.master.item.repository;

import in.opt.sfa.tenant.master.item.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface ItemMasterRepository extends JpaRepository<ItemMaster, Long> {
    List<ItemMaster> findAllByOrderByItemNameAsc();
    List<ItemMaster> findByStatusTrueOrderByItemNameAsc();
    Optional<ItemMaster> findByItemCode(String itemCode);
    List<ItemMaster> findByItemCodeIn(java.util.Collection<String> itemCodes);
}
