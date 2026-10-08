package in.opt.sfa.tenant.menu.repository;

import in.opt.sfa.common.enums.MenuTypeEnum;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TENANT-db repository (auto-routed to the active tenant via package location).
 */
public interface CompanyMenuSelfRepository extends JpaRepository<CompanyMenuSelf, Long> {

    /**
     * The BASE menu: all active rows, ordered for tree assembly. emp_id on
     * menu_item is a company/owner marker in this per-tenant DB (uniform across
     * the tenant), NOT a per-employee filter — per-employee hide/show is stored
     * separately in menu_visibility. So the base selection ignores emp_id.
     */
    List<CompanyMenuSelf> findByIsActiveTrueOrderBySortOrderAscIdAsc();

    List<CompanyMenuSelf> findByIsActiveTrueAndMenuTypeOrderBySortOrderAscIdAsc(MenuTypeEnum menuType);

    /**
     * Company/owner-scoped base menu: active rows for one emp_id marker. In this
     * per-tenant DB every menu row carries the same owner emp_id, so this narrows
     * the scan to this company's rows instead of the whole table (the requested
     * "employee-wise, not all" fetch). Per-employee hide/show still lives in
     * menu_visibility, applied on top of this base.
     */
    List<CompanyMenuSelf> findByIsActiveTrueAndEmpIdOrderBySortOrderAscIdAsc(String empId);
}
