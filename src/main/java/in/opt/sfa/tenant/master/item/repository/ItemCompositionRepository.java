package in.opt.sfa.tenant.master.item.repository;

import in.opt.sfa.tenant.master.item.entity.ItemComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** TENANT-db repository. */
public interface ItemCompositionRepository extends JpaRepository<ItemComposition, Long> {
    List<ItemComposition> findByItemIdOrderBySortOrderAscIdAsc(Long itemId);

    @Modifying
    @Query("delete from ItemComposition c where c.itemId = :itemId")
    void deleteByItemId(@org.springframework.data.repository.query.Param("itemId") Long itemId);

    /** Salt names already used in any item — suggestions while typing (not a master). */
    @Query("select distinct c.moleculeName from ItemComposition c order by c.moleculeName")
    List<String> findDistinctMoleculeNames();
}
