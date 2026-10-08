package in.opt.sfa.tenant.menu.copy;

import in.opt.sfa.tenant.menu.copy.dto.MenuCopyTargetResultDto;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import in.opt.sfa.tenant.menu.entity.MenuAuditLog;
import in.opt.sfa.tenant.menu.repository.CompanyMenuSelfRepository;
import in.opt.sfa.tenant.menu.repository.MenuAuditLogRepository;
import in.opt.sfa.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every method here runs against WHICHEVER tenant TenantContext is currently
 * set to — the orchestrator ({@link MenuCopyService}) flips it before each
 * call. Split into its own bean (rather than methods on the orchestrator)
 * because @Transactional only takes effect through the Spring proxy, i.e. on
 * an external call from a DIFFERENT bean — a self-call would silently run
 * without a transaction/connection at all.
 */
@Service
public class MenuCopyTenantOps {

    private final CompanyMenuSelfRepository items;
    private final MenuAuditLogRepository auditLog;

    public MenuCopyTenantOps(CompanyMenuSelfRepository items, MenuAuditLogRepository auditLog) {
        this.items = items;
        this.auditLog = auditLog;
    }

    /** Every row for the CURRENT tenant — used both to gather the requested
     *  subtree(s) and to walk UP each root's ancestor path. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<CompanyMenuSelf> allRows() {
        return items.findAll();
    }

    /**
     * Copies the given root ids — each with its FULL subtree, any depth —
     * from {@code sourceAll} (already fetched from the source tenant) into
     * the CURRENT (target) tenant.
     *
     * The root's ancestor path (Masters > Catalog > ...) is auto-matched by
     * label under the target's existing tree, creating any missing ancestor
     * "folder" along the way, so a deep item lands in the same conceptual
     * place even if that branch doesn't fully exist yet in the target.
     *
     * Idempotent: a node that already exists in the target (same parent +
     * page, or same parent + label when page is blank) is reused, never
     * duplicated — so re-running a copy only fills in what's missing.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuCopyTargetResultDto copyInto(String targetCompanyCode, String sourceCompanyCode, List<Long> rootIds, List<CompanyMenuSelf> sourceAll) {
        Map<Long, CompanyMenuSelf> sourceById = sourceAll.stream()
                .collect(Collectors.toMap(CompanyMenuSelf::getId, m -> m));
        Map<Long, List<CompanyMenuSelf>> sourceByParent = new HashMap<>();
        for (CompanyMenuSelf m : sourceAll) sourceByParent.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);

        List<CompanyMenuSelf> targetAll = items.findAll();
        String baseEmpId = targetAll.stream().map(CompanyMenuSelf::getEmpId)
                .filter(e -> e != null && !e.isBlank()).findFirst().orElse(null);

        Map<String, Long> existing = new HashMap<>();
        for (CompanyMenuSelf m : targetAll) existing.put(key(m.getParentId(), m.getPage(), m.getLabel()), m.getId());

        int[] counts = {0, 0}; // {created, reused}
        for (Long rootId : rootIds) {
            CompanyMenuSelf srcRoot = sourceById.get(rootId);
            if (srcRoot == null) continue; // stale id from the client — skip rather than fail the whole batch
            Long targetParentId = resolveAncestorPath(srcRoot.getParentId(), sourceById, existing, baseEmpId, counts, sourceCompanyCode);
            copyNodeRecursive(srcRoot, targetParentId, sourceByParent, existing, baseEmpId, counts, sourceCompanyCode);
        }
        return new MenuCopyTargetResultDto(targetCompanyCode, counts[0], counts[1], null);
    }

    private Long resolveAncestorPath(Long sourceParentId, Map<Long, CompanyMenuSelf> sourceById, Map<String, Long> existing,
                                      String baseEmpId, int[] counts, String sourceCompanyCode) {
        if (sourceParentId == null) return null;
        CompanyMenuSelf sourceParent = sourceById.get(sourceParentId);
        if (sourceParent == null) return null; // orphaned reference — treat as top level
        Long grandTargetId = resolveAncestorPath(sourceParent.getParentId(), sourceById, existing, baseEmpId, counts, sourceCompanyCode);
        String k = key(grandTargetId, sourceParent.getPage(), sourceParent.getLabel());
        Long found = existing.get(k);
        if (found != null) { counts[1]++; return found; }
        CompanyMenuSelf created = persistCopy(sourceParent, grandTargetId, baseEmpId);
        existing.put(k, created.getId());
        counts[0]++;
        log(created, sourceCompanyCode);
        return created.getId();
    }

    private void copyNodeRecursive(CompanyMenuSelf src, Long targetParentId, Map<Long, List<CompanyMenuSelf>> sourceByParent,
                                    Map<String, Long> existing, String baseEmpId, int[] counts, String sourceCompanyCode) {
        String k = key(targetParentId, src.getPage(), src.getLabel());
        Long targetId = existing.get(k);
        if (targetId != null) {
            counts[1]++;
        } else {
            CompanyMenuSelf created = persistCopy(src, targetParentId, baseEmpId);
            existing.put(k, created.getId());
            targetId = created.getId();
            counts[0]++;
            log(created, sourceCompanyCode);
        }
        for (CompanyMenuSelf child : sourceByParent.getOrDefault(src.getId(), List.of())) {
            copyNodeRecursive(child, targetId, sourceByParent, existing, baseEmpId, counts, sourceCompanyCode);
        }
    }

    /** A duplicate-detection key: same parent + (page if set, else label — page is the more
     *  stable identifier across tenants since it drives the frontend's data-page match). */
    private String key(Long parentId, String page, String label) {
        String disambig = (page != null && !page.isBlank())
                ? "page:" + page.trim().toLowerCase()
                : "label:" + (label == null ? "" : label.trim().toLowerCase());
        return (parentId == null ? "null" : parentId) + "|" + disambig;
    }

    private CompanyMenuSelf persistCopy(CompanyMenuSelf src, Long targetParentId, String baseEmpId) {
        CompanyMenuSelf m = new CompanyMenuSelf();
        m.setParentId(targetParentId);
        m.setEmpId(baseEmpId);
        m.setBaseUrl(src.getBaseUrl());
        m.setMenuType(src.getMenuType());
        m.setLabel(src.getLabel());
        m.setTitle(src.getTitle());
        m.setIcon(src.getIcon());
        m.setPage(src.getPage());
        m.setHref(src.getHref());
        m.setTarget(src.getTarget());
        m.setBadge(src.getBadge());
        m.setBadgeColor(src.getBadgeColor());
        m.setSortOrder(src.getSortOrder());
        m.setIsVisible(src.getIsVisible());
        m.setIsActive(src.getIsActive());
        m.setDescription(src.getDescription());
        return items.save(m);
    }

    private void log(CompanyMenuSelf m, String sourceCompanyCode) {
        MenuAuditLog a = new MenuAuditLog();
        a.setMenuItemId(m.getId());
        a.setLabel(m.getLabel());
        a.setMenuType(m.getMenuType() == null ? "WEB" : m.getMenuType().name());
        a.setAction("CREATE");
        a.setChangedBy(currentEmpId());
        a.setChangedAt(LocalDateTime.now());
        a.setDetails("Copied from tenant '" + sourceCompanyCode + "'");
        auditLog.save(a);
    }

    private static String currentEmpId() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
