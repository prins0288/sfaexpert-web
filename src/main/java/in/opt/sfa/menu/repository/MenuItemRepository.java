package in.opt.sfa.menu.repository;

import in.opt.sfa.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** All enabled items, already ordered for tree assembly. */
    List<MenuItem> findByEnabledTrueOrderBySortOrderAscIdAsc();
}
