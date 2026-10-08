package in.opt.sfa.tenant.menu;

import in.opt.sfa.common.enums.MenuTypeEnum;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import in.opt.sfa.tenant.menu.entity.MenuVisibility;
import in.opt.sfa.tenant.menu.repository.CompanyMenuSelfRepository;
import in.opt.sfa.tenant.menu.repository.MenuVisibilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backs the "Menu Visibility" management page.
 *
 * Scope of an edit:
 *   - ALL           -> edits each item's GLOBAL company_menu_self.is_visible
 *                      (everyone who has no per-employee override follows it);
 *   - a specific emp -> upserts sparse menu_visibility overrides for that emp;
 *   - SELF           -> same, for the caller's own emp id.
 *
 * The management tree returns EVERY active item (even hidden ones, so they can
 * be turned back on) with the effective visible flag for the chosen scope.
 * Per-employee saves stay sparse: an override equal to the global flag is
 * deleted rather than stored, so "no row = follow global".
 */
@Service
public class MenuVisibilityService {

    private final CompanyMenuSelfRepository items;
    private final MenuVisibilityRepository overrides;

    public MenuVisibilityService(CompanyMenuSelfRepository items, MenuVisibilityRepository overrides) {
        this.items = items;
        this.overrides = overrides;
    }

    /** A management node: item + effective visibility for the scope + its menu_type
     *  (WEB/APP/BOTH). parentId lets the flat APP cards send reorder moves without
     *  changing an item's parent. */
    public record VisNode(Long id, String label, String icon, String href, boolean visible,
                          String menuType, Long parentId, List<VisNode> children) {}

    /** A move produced by drag-drop: item id, its new parent (null = top level), new order. */
    public record Move(Long id, Long parentId, Integer sortOrder) {}

    /** WEB side: the nested menu tree (WEB + BOTH) with the scope's effective
     *  visibility + layout, so the draggable tree shows the employee's arrangement. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<VisNode> tree(String empId) {
        Map<Long, MenuVisibility> ov = overridesMap(empId);
        List<CompanyMenuSelf> web = active().stream().filter(m -> MenuService.onPlatform(m, MenuTypeEnum.WEB)).toList();

        Map<Long, List<CompanyMenuSelf>> byParent = new LinkedHashMap<>();
        Map<Long, Integer> effSort = new java.util.HashMap<>();
        java.util.Set<Long> present = new java.util.HashSet<>();
        web.forEach(m -> present.add(m.getId()));
        for (CompanyMenuSelf m : web) {
            MenuVisibility o = ov.get(m.getId());
            Long parent = (o != null && o.getParentId() != null) ? o.getParentId() : m.getParentId();
            if (parent != null && !present.contains(parent)) parent = null;   // orphan -> promote to root
            int sort = (o != null && o.getSortOrder() != null) ? o.getSortOrder() : safeSort(m);
            effSort.put(m.getId(), sort);
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(m);
        }
        byParent.values().forEach(list -> list.sort(
                java.util.Comparator.comparingInt((CompanyMenuSelf m) -> effSort.get(m.getId())).thenComparingLong(CompanyMenuSelf::getId)));

        List<VisNode> roots = new ArrayList<>();
        for (CompanyMenuSelf top : byParent.getOrDefault(null, List.of())) roots.add(node(top, byParent, ov));
        return roots;
    }

    /** APP side: a FLAT, ordered list of the mobile menu (APP + BOTH) as cards. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<VisNode> appList(String empId) {
        Map<Long, MenuVisibility> ov = overridesMap(empId);
        List<CompanyMenuSelf> app = new ArrayList<>(active().stream().filter(m -> MenuService.onPlatform(m, MenuTypeEnum.APP)).toList());
        app.sort(java.util.Comparator.comparingInt((CompanyMenuSelf m) -> {
            MenuVisibility o = ov.get(m.getId());
            return (o != null && o.getSortOrder() != null) ? o.getSortOrder() : safeSort(m);
        }).thenComparingLong(CompanyMenuSelf::getId));
        List<VisNode> out = new ArrayList<>();
        for (CompanyMenuSelf m : app) {
            MenuVisibility o = ov.get(m.getId());
            boolean visible = o != null ? Boolean.TRUE.equals(o.getIsVisible()) : !Boolean.FALSE.equals(m.getIsVisible());
            out.add(new VisNode(m.getId(), m.getLabel(), m.getIcon(), m.getHref(), visible, typeOf(m), m.getParentId(), List.of()));
        }
        return out;
    }

    private VisNode node(CompanyMenuSelf item, Map<Long, List<CompanyMenuSelf>> byParent, Map<Long, MenuVisibility> ov) {
        List<VisNode> children = new ArrayList<>();
        for (CompanyMenuSelf c : byParent.getOrDefault(item.getId(), List.of())) children.add(node(c, byParent, ov));
        MenuVisibility o = ov.get(item.getId());
        boolean visible = o != null ? Boolean.TRUE.equals(o.getIsVisible()) : !Boolean.FALSE.equals(item.getIsVisible());
        return new VisNode(item.getId(), item.getLabel(), item.getIcon(), item.getHref(), visible,
                typeOf(item), item.getParentId(), children.isEmpty() ? List.of() : children);
    }

    /** Set an item's platform (WEB | APP | BOTH) — global, Admin/Manager only. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void setMenuType(Long id, String menuType) {
        MenuTypeEnum type;
        try { type = MenuTypeEnum.valueOf(menuType == null ? "WEB" : menuType.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { type = MenuTypeEnum.WEB; }
        final MenuTypeEnum t = type;
        items.findById(id).ifPresent(m -> { m.setMenuType(t); items.save(m); });
    }

    private List<CompanyMenuSelf> active() { return items.findByIsActiveTrueOrderBySortOrderAscIdAsc(); }
    private static int safeSort(CompanyMenuSelf m) { return m.getSortOrder() == null ? 0 : m.getSortOrder(); }
    private static String typeOf(CompanyMenuSelf m) { return m.getMenuType() == null ? "WEB" : m.getMenuType().name(); }
    private Map<Long, MenuVisibility> overridesMap(String empId) {
        Map<Long, MenuVisibility> ov = new LinkedHashMap<>();
        if (empId != null) for (MenuVisibility v : overrides.findByEmpId(empId)) ov.put(v.getMenuItemId(), v);
        return ov;
    }

    // ---- visibility save ---------------------------------------------------

    /** Save GLOBAL visibility (the ALL scope): company_menu_self.is_visible per item. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveGlobal(Map<Long, Boolean> values) {
        values.forEach((id, vis) -> items.findById(id).ifPresent(m -> {
            m.setIsVisible(Boolean.TRUE.equals(vis));
            items.save(m);
        }));
    }

    /** Save per-employee VISIBILITY sparsely: keep a row only where it differs from
     *  the global flag OR carries a layout override; else drop it. Preserves any
     *  parent/sort layout already on the row. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveForEmp(String empId, Map<Long, Boolean> values) {
        values.forEach((id, vis) -> {
            boolean want = Boolean.TRUE.equals(vis);
            boolean global = items.findById(id).map(m -> !Boolean.FALSE.equals(m.getIsVisible())).orElse(true);
            MenuVisibility row = overrides.findByEmpIdAndMenuItemId(empId, id).orElse(null);
            boolean hasLayout = row != null && (row.getParentId() != null || row.getSortOrder() != null);
            if (want == global && !hasLayout) {          // nothing to override
                if (row != null) overrides.delete(row);
                return;
            }
            if (row == null) { row = newRow(empId, id); }
            row.setIsVisible(want);
            row.setUpdatedAt(LocalDateTime.now());
            overrides.save(row);
        });
    }

    // ---- layout (reorder / re-parent) save ---------------------------------

    /** Save GLOBAL layout (ALL scope): update company_menu_self.parent_id + sort_order. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveGlobalLayout(List<Move> moves) {
        for (Move mv : moves) {
            items.findById(mv.id()).ifPresent(m -> {
                m.setParentId(mv.parentId());
                if (mv.sortOrder() != null) m.setSortOrder(mv.sortOrder());
                items.save(m);
            });
        }
    }

    /** Save an employee's arrangement SPARSELY: the client sends the whole tree as
     *  a dense index order, but we only keep a layout override for items whose
     *  (parent, position) differs from the BASE menu's own (parent, position).
     *  Items left where the base puts them keep NO layout override, so later
     *  global reorders still flow through to this employee. A row that ends up
     *  with neither a layout nor a visibility difference is dropped entirely. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveEmpLayout(String empId, List<Move> moves) {
        // base position of every active item: parent + dense index among siblings
        List<CompanyMenuSelf> all = items.findByIsActiveTrueOrderBySortOrderAscIdAsc();
        Map<Long, Long> baseParent = new LinkedHashMap<>();
        Map<Long, Integer> baseIndex = new LinkedHashMap<>();
        Map<Long, List<CompanyMenuSelf>> byParent = new LinkedHashMap<>();
        for (CompanyMenuSelf m : all) byParent.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
        byParent.forEach((p, list) -> {
            for (int i = 0; i < list.size(); i++) { baseParent.put(list.get(i).getId(), p); baseIndex.put(list.get(i).getId(), i); }
        });

        for (Move mv : moves) {
            if (!baseParent.containsKey(mv.id())) continue;               // unknown / inactive item
            boolean sameParent = java.util.Objects.equals(mv.parentId(), baseParent.get(mv.id()));
            boolean samePos = mv.sortOrder() == null || mv.sortOrder().equals(baseIndex.get(mv.id()));
            MenuVisibility row = overrides.findByEmpIdAndMenuItemId(empId, mv.id()).orElse(null);

            if (sameParent && samePos) {                                 // sits at its base spot -> drop the layout override
                if (row == null) continue;
                row.setParentId(null);
                row.setSortOrder(null);
                boolean visDiffers = row.getIsVisible() != null && !row.getIsVisible().equals(itemVisible(mv.id()));
                if (visDiffers) overrides.save(touch(row));              // keep only for a visibility difference
                else overrides.delete(row);
                continue;
            }
            if (row == null) row = newRow(empId, mv.id());
            row.setParentId(sameParent ? null : mv.parentId());
            row.setSortOrder(samePos ? null : mv.sortOrder());
            overrides.save(touch(row));
        }
    }

    private boolean itemVisible(Long id) { return items.findById(id).map(m -> !Boolean.FALSE.equals(m.getIsVisible())).orElse(true); }
    private MenuVisibility touch(MenuVisibility r) { r.setUpdatedAt(LocalDateTime.now()); return r; }

    /** A fresh override row seeded with the item's current global visibility. */
    private MenuVisibility newRow(String empId, Long menuItemId) {
        MenuVisibility r = new MenuVisibility();
        r.setEmpId(empId);
        r.setMenuItemId(menuItemId);
        boolean global = items.findById(menuItemId).map(m -> !Boolean.FALSE.equals(m.getIsVisible())).orElse(true);
        r.setIsVisible(global);
        return r;
    }
}
