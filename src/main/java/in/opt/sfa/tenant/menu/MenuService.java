package in.opt.sfa.tenant.menu;

import in.opt.sfa.common.enums.MenuTypeEnum;
import in.opt.sfa.tenant.menu.dto.MenuNode;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import in.opt.sfa.tenant.menu.entity.MenuVisibility;
import in.opt.sfa.tenant.menu.entity.UserMenuFavorite;
import in.opt.sfa.tenant.menu.repository.CompanyMenuSelfRepository;
import in.opt.sfa.tenant.menu.repository.MenuVisibilityRepository;
import in.opt.sfa.tenant.menu.repository.UserMenuFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the navigation tree from the {@code menu_item} table and filters it to
 * what the CURRENT user should see.
 *
 * Visibility (applied to every node, branch or leaf):
 *   - start from the BASE menu (all active rows);
 *   - effective visible = the user's per-employee override (menu_visibility for
 *     their emp_id) if one exists, else the item's global is_visible;
 *   - a branch (no href) whose children are all hidden is pruned, so the menu
 *     never shows an empty parent.
 *
 * Role-based gating was removed with the roles column; endpoints stay role-gated
 * server-side, so hiding a menu item is a navigation concern only.
 */
@Service
public class MenuService {

    private final CompanyMenuSelfRepository repo;
    private final MenuVisibilityRepository visibility;
    private final UserMenuFavoriteRepository favorites;

    public MenuService(CompanyMenuSelfRepository repo, MenuVisibilityRepository visibility,
                       UserMenuFavoriteRepository favorites) {
        this.repo = repo;
        this.visibility = visibility;
        this.favorites = favorites;
    }

    /** The ids this user has starred. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Long> favoritesFor(String username) {
        return favorites.findByUsername(username).stream()
                .map(UserMenuFavorite::getMenuItemId).toList();
    }

    /** Star a menu item (idempotent); returns the updated favorites list. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public List<Long> addFavorite(String username, Long menuItemId) {
        if (favorites.findByUsernameAndMenuItemId(username, menuItemId).isEmpty()) {
            UserMenuFavorite f = new UserMenuFavorite();
            f.setUsername(username);
            f.setMenuItemId(menuItemId);
            favorites.save(f);
        }
        return favoritesFor(username);
    }

    /** Un-star a menu item (idempotent); returns the updated favorites list. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public List<Long> removeFavorite(String username, Long menuItemId) {
        favorites.deleteByUsernameAndMenuItemId(username, menuItemId);
        return favoritesFor(username);
    }

    /** The visible menu tree for the signed-in employee, keyed by their emp_id
     *  (the verified JWT identity: user_login_master.emp_id -> emp_detail.emp_id),
     *  honouring that employee's per-item VISIBILITY and LAYOUT (parent+order)
     *  overrides so their sidebar can differ from the shared base menu. This is
     *  the WEB sidebar, so only WEB / BOTH items are included (APP-only items
     *  belong to the mobile app menu, not this shell). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MenuNode> menuFor(String empId) {
        Map<Long, MenuVisibility> ov = overrideRows(empId);

        // Employee-wise fetch: pull only THIS employee's menu rows (indexed on
        // emp_id) instead of scanning the whole table. If the employee has no own
        // rows yet (the shared company menu still lives under the owner emp_id),
        // fall back to the full company base menu so the sidebar is never blank.
        List<CompanyMenuSelf> base = (empId == null) ? List.of()
                : repo.findByIsActiveTrueAndEmpIdOrderBySortOrderAscIdAsc(empId);
        if (base.isEmpty()) base = repo.findByIsActiveTrueOrderBySortOrderAscIdAsc();

        List<CompanyMenuSelf> all = base.stream()
                .filter(m -> onPlatform(m, MenuTypeEnum.WEB)).toList();

        // group by EFFECTIVE parent (override.parent_id ?? base parent_id)
        Map<Long, List<CompanyMenuSelf>> byParent = new LinkedHashMap<>();
        Map<Long, Integer> effSort = new java.util.HashMap<>();
        for (CompanyMenuSelf m : all) {
            MenuVisibility o = ov.get(m.getId());
            Long parent = (o != null && o.getParentId() != null) ? o.getParentId() : m.getParentId();
            int sort = (o != null && o.getSortOrder() != null) ? o.getSortOrder()
                    : (m.getSortOrder() == null ? 0 : m.getSortOrder());
            effSort.put(m.getId(), sort);
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(m);
        }
        // order each sibling group by EFFECTIVE sort, then id (stable tie-break)
        byParent.values().forEach(list -> list.sort(
                java.util.Comparator.comparingInt((CompanyMenuSelf m) -> effSort.get(m.getId())).thenComparingLong(CompanyMenuSelf::getId)));

        List<MenuNode> roots = new ArrayList<>();
        for (CompanyMenuSelf top : byParent.getOrDefault(null, List.of())) {
            MenuNode node = build(top, byParent, ov);
            if (node != null) roots.add(node);
        }
        return roots;
    }

    private MenuNode build(CompanyMenuSelf item, Map<Long, List<CompanyMenuSelf>> byParent, Map<Long, MenuVisibility> ov) {
        if (!visible(item, ov)) return null;   // hides the node and its whole subtree

        List<MenuNode> children = new ArrayList<>();
        for (CompanyMenuSelf child : byParent.getOrDefault(item.getId(), List.of())) {
            MenuNode c = build(child, byParent, ov);
            if (c != null) children.add(c);
        }

        boolean isBranch = byParent.containsKey(item.getId());
        boolean hasHref = item.getHref() != null && !item.getHref().isBlank();
        if (isBranch && children.isEmpty() && !hasHref) return null;   // prune empty branch

        return new MenuNode(
                item.getId(), item.getLabel(), item.getTitle(), item.getDescription(),
                item.getIcon(), null /* logo dropped from schema */, item.getPage(), item.getHref(),
                item.getTarget(), item.getBaseUrl(),
                children.isEmpty() ? List.of() : children);
    }

    /** Effective visibility: per-employee override row wins, else the global flag. */
    private boolean visible(CompanyMenuSelf item, Map<Long, MenuVisibility> ov) {
        MenuVisibility o = ov.get(item.getId());
        if (o != null) return Boolean.TRUE.equals(o.getIsVisible());
        return !Boolean.FALSE.equals(item.getIsVisible());
    }

    /** True if a menu item belongs on the given platform ("WEB" | "APP").
     *  BOTH (and a null/blank type, treated as WEB) matches accordingly. */
    public static boolean onPlatform(CompanyMenuSelf m, MenuTypeEnum platform) {
        String t = (m.getMenuType() == null) ? "WEB" : m.getMenuType().name();
        if ("BOTH".equals(t)) return true;
        return t.equals(platform.name());
    }

    /** menu_item_id -> override row for one employee (empty if empId null). */
    private Map<Long, MenuVisibility> overrideRows(String empId) {
        Map<Long, MenuVisibility> map = new LinkedHashMap<>();
        if (empId == null) return map;
        for (MenuVisibility v : visibility.findByEmpId(empId)) map.put(v.getMenuItemId(), v);
        return map;
    }
}
