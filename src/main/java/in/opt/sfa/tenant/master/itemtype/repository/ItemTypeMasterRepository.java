package in.opt.sfa.tenant.master.itemtype.repository;

import in.opt.sfa.tenant.master.itemtype.entity.ItemTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface ItemTypeMasterRepository extends JpaRepository<ItemTypeMaster, Long> {

    List<ItemTypeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<ItemTypeMaster> findByIsActiveOrderByDisplayOrderAscItemTypeNameAsc(Boolean isActive);

    Optional<ItemTypeMaster> findByItemTypeCode(String itemTypeCode);

    Optional<ItemTypeMaster> findByItemTypeName(String itemTypeName);
}
