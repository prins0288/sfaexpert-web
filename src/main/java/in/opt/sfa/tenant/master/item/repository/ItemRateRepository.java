package in.opt.sfa.tenant.master.item.repository;

import in.opt.sfa.tenant.master.item.entity.ItemRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** TENANT-db repository. */
public interface ItemRateRepository extends JpaRepository<ItemRate, Long> {
    List<ItemRate> findByItemIdOrderByEffectiveFromAscIdAsc(Long itemId);
    List<ItemRate> findAllByOrderByItemIdAscEffectiveFromAsc();

    /** Active rates valid on a date for this state or All India — the state's own rate first. */
    @Query("""
            select r from ItemRate r
            where r.itemId = :itemId and r.status = true
              and r.effectiveFrom <= :onDate and (r.effectiveTo is null or r.effectiveTo >= :onDate)
              and (r.stateId = :stateId or r.stateId is null)
            order by case when r.stateId is null then 1 else 0 end, r.effectiveFrom desc""")
    List<ItemRate> findValid(@Param("itemId") Long itemId, @Param("stateId") Long stateId, @Param("onDate") LocalDate onDate);
}
