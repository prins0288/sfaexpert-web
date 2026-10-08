package in.opt.sfa.tenant.menu;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.menu.dto.MenuMasterDto;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import in.opt.sfa.tenant.menu.entity.MenuAuditLog;
import in.opt.sfa.tenant.menu.mapper.MenuMasterMapper;
import in.opt.sfa.tenant.menu.repository.CompanyMenuSelfRepository;
import in.opt.sfa.tenant.menu.repository.MenuAuditLogRepository;
import in.opt.sfa.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Backs the "Menu Master" admin page — create / edit / activate / deactivate /
 * delete ANY menu item (WEB, APP or BOTH), anywhere in the tree. Unlike Menu
 * Visibility (which only edits an EXISTING item's show/hide + layout), this
 * is the page that actually adds a brand-new entry or removes one.
 *
 * company_menu_self carries no created_by/updated_by of its own, so every
 * write here also appends a row to menu_audit_log — that table is what the
 * Menu Audit Report reads.
 */
@Service
public class MenuMasterService {

    private final CompanyMenuSelfRepository items;
    private final MenuMasterMapper mapper;
    private final MenuAuditLogRepository audit;

    public MenuMasterService(CompanyMenuSelfRepository items, MenuMasterMapper mapper, MenuAuditLogRepository audit) {
        this.items = items;
        this.mapper = mapper;
        this.audit = audit;
    }

    /** Every menu item (active + inactive, so nothing is hidden from the admin),
     *  flattened in tree order with depth + a full breadcrumb — also what the
     *  frontend uses to build the indented "Parent" picker. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MenuMasterDto> list() {
        List<CompanyMenuSelf> all = items.findAll();
        Map<Long, List<CompanyMenuSelf>> byParent = new LinkedHashMap<>();
        Map<Long, String> labelById = new LinkedHashMap<>();
        for (CompanyMenuSelf m : all) {
            byParent.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
            labelById.put(m.getId(), m.getLabel());
        }
        byParent.values().forEach(list -> list.sort(
                java.util.Comparator.comparingInt((CompanyMenuSelf m) -> m.getSortOrder() == null ? 0 : m.getSortOrder())
                        .thenComparingLong(CompanyMenuSelf::getId)));

        List<MenuMasterDto> out = new ArrayList<>();
        walk(null, 0, "", byParent, labelById, out);
        return out;
    }

    private void walk(Long parentId, int depth, String parentPath, Map<Long, List<CompanyMenuSelf>> byParent,
                      Map<Long, String> labelById, List<MenuMasterDto> out) {
        for (CompanyMenuSelf m : byParent.getOrDefault(parentId, List.of())) {
            MenuMasterDto d = mapper.toDto(m);
            d.setDepth(depth);
            d.setParentLabel(parentId == null ? null : labelById.get(parentId));
            String path = parentPath.isEmpty() ? m.getLabel() : parentPath + " > " + m.getLabel();
            d.setPathLabel(path);
            out.add(d);
            walk(m.getId(), depth + 1, path, byParent, labelById, out);
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public MenuMasterDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuMasterDto create(MenuMasterDto form) {
        if (Strings.isBlank(form.getLabel())) throw new IllegalStateException("Label is required.");
        CompanyMenuSelf m = new CompanyMenuSelf();
        mapper.applyTo(m, form);
        m.setEmpId(baseEmpId());
        m.setIsVisible(true);
        m.setIsActive(true);
        if (m.getSortOrder() == null) m.setSortOrder(0);
        m = items.save(m);
        log(m.getId(), m.getLabel(), typeOf(m), "CREATE", null);
        return mapper.toDto(m);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuMasterDto update(MenuMasterDto form) {
        CompanyMenuSelf before = find(form.getId());
        String diff = diff(before, form);
        mapper.applyTo(before, form);
        CompanyMenuSelf saved = items.save(before);
        if (diff != null) log(saved.getId(), saved.getLabel(), typeOf(saved), "UPDATE", diff);
        return mapper.toDto(saved);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuMasterDto updateStatus(Long id, boolean isActive) {
        CompanyMenuSelf m = find(id);
        m.setIsActive(isActive);
        m = items.save(m);
        log(m.getId(), m.getLabel(), typeOf(m), isActive ? "ACTIVATE" : "DEACTIVATE", null);
        return mapper.toDto(m);
    }

    /** Hard delete — blocked while the item still has children (reparent or delete them first). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete(Long id) {
        CompanyMenuSelf m = find(id);
        boolean hasChildren = !items.findAll().stream().filter(x -> id.equals(x.getParentId())).toList().isEmpty();
        if (hasChildren) {
            throw new IllegalStateException("This item still has sub-items — move or delete them first.");
        }
        String label = m.getLabel();
        String type = typeOf(m);
        items.delete(m);
        log(id, label, type, "DELETE", null);
    }

    // ---- helpers -------------------------------------------------------

    private CompanyMenuSelf find(Long id) {
        return items.findById(id).orElseThrow(() -> new IllegalStateException("Menu item not found: " + id));
    }

    private static String typeOf(CompanyMenuSelf m) {
        return m.getMenuType() == null ? "WEB" : m.getMenuType().name();
    }

    /** Every menu row for a tenant shares the same owner emp_id marker — copy it from any
     *  existing row (there's always at least one) so a brand-new row matches the rest. */
    private String baseEmpId() {
        return items.findAll().stream().map(CompanyMenuSelf::getEmpId)
                .filter(e -> !Strings.isBlank(e)).findFirst().orElse(null);
    }

    /** "field: old -> new" for each field that actually changed; null if nothing did. */
    private String diff(CompanyMenuSelf before, MenuMasterDto after) {
        List<String> changes = new ArrayList<>();
        addIfChanged(changes, "parentId", before.getParentId(), after.getParentId());
        addIfChanged(changes, "menuType", typeOf(before), after.getMenuType());
        addIfChanged(changes, "label", before.getLabel(), after.getLabel());
        addIfChanged(changes, "title", before.getTitle(), after.getTitle());
        addIfChanged(changes, "icon", before.getIcon(), after.getIcon());
        addIfChanged(changes, "page", before.getPage(), after.getPage());
        addIfChanged(changes, "href", before.getHref(), after.getHref());
        addIfChanged(changes, "target", before.getTarget() == null ? "_SELF" : before.getTarget().name(), after.getTarget());
        addIfChanged(changes, "sortOrder", before.getSortOrder(), after.getSortOrder());
        return changes.isEmpty() ? null : String.join("; ", changes);
    }

    private void addIfChanged(List<String> changes, String field, Object oldV, Object newV) {
        if (!Objects.equals(normalize(oldV), normalize(newV))) {
            changes.add(field + ": '" + oldV + "' -> '" + newV + "'");
        }
    }

    private Object normalize(Object v) { return (v instanceof String s && s.isBlank()) ? null : v; }

    private void log(Long menuItemId, String label, String menuType, String action, String details) {
        MenuAuditLog a = new MenuAuditLog();
        a.setMenuItemId(menuItemId);
        a.setLabel(label);
        a.setMenuType(menuType);
        a.setAction(action);
        a.setChangedBy(currentEmpId());
        a.setChangedAt(LocalDateTime.now());
        a.setDetails(details);
        audit.save(a);
    }

    private static String currentEmpId() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
