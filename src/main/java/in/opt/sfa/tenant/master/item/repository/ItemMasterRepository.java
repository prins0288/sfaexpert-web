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

    /**
     * Highest number after the prefix among codes like PD01, PD02 ... (null = none yet).
     * A locking read, so it sees codes committed after this transaction started.
     */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value =
            "SELECT MAX(CAST(SUBSTRING(item_code, :start) AS UNSIGNED)) FROM item_master WHERE item_code REGEXP :pattern FOR UPDATE")
    Long maxCodeNumber(@org.springframework.data.repository.query.Param("start") int start,
                       @org.springframework.data.repository.query.Param("pattern") String pattern);

    /** Locks the item type row so two saves of the same type cannot take the same new code. */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT id FROM item_type_master WHERE id = :id FOR UPDATE")
    Long lockItemType(@org.springframework.data.repository.query.Param("id") Long id);
}
