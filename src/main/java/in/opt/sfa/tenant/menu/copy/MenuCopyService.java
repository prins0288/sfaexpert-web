package in.opt.sfa.tenant.menu.copy;

import in.opt.sfa.common.entity.TenantConfig;
import in.opt.sfa.common.repository.TenantConfigRepository;
import in.opt.sfa.tenant.menu.copy.dto.MenuCopyNodeDto;
import in.opt.sfa.tenant.menu.copy.dto.MenuCopyRequest;
import in.opt.sfa.tenant.menu.copy.dto.MenuCopyTargetResultDto;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies one or more menu items — each with its FULL subtree, any depth —
 * from one tenant into one or more other tenants. Deliberately NOT
 * tenant-routed the normal way: source and target(s) are explicit request
 * parameters, not implied by the caller's own login tenant.
 *
 * {@link in.opt.sfa.tenant.datasource.TenantRoutingDataSource} re-reads
 * TenantContext on every connection acquisition (not cached), so flipping it
 * between separate transactional calls safely switches which tenant DB the
 * next read/write goes against. The caller's own tenant is always restored
 * before returning, in a finally block — this method never leaves the
 * request's TenantContext pointed at someone else's tenant.
 */
@Service
public class MenuCopyService {

    private final TenantConfigRepository tenantConfigs;
    private final MenuCopyTenantOps ops;

    public MenuCopyService(TenantConfigRepository tenantConfigs, MenuCopyTenantOps ops) {
        this.tenantConfigs = tenantConfigs;
        this.ops = ops;
    }

    /** Every configured tenant id, for the source/target pickers. */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public List<String> tenants() {
        return tenantConfigs.findAll().stream().map(TenantConfig::getCompanyCode).sorted().toList();
    }

    /** The full current menu tree of ANY tenant (not just the caller's own), flattened with
     *  depth, for the "pick items to copy" checkbox tree. */
    public List<MenuCopyNodeDto> tree(String companyCode) {
        String prev = TenantContext.getCompanyCode();
        try {
            TenantContext.setCompanyCode(companyCode);
            List<CompanyMenuSelf> all = ops.allRows();
            Map<Long, List<CompanyMenuSelf>> byParent = new LinkedHashMap<>();
            for (CompanyMenuSelf m : all) byParent.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
            byParent.values().forEach(list -> list.sort(
                    Comparator.comparingInt((CompanyMenuSelf m) -> m.getSortOrder() == null ? 0 : m.getSortOrder())
                            .thenComparingLong(CompanyMenuSelf::getId)));
            List<MenuCopyNodeDto> out = new ArrayList<>();
            walk(null, 0, byParent, out);
            return out;
        } finally {
            restore(prev);
        }
    }

    private void walk(Long parentId, int depth, Map<Long, List<CompanyMenuSelf>> byParent, List<MenuCopyNodeDto> out) {
        for (CompanyMenuSelf m : byParent.getOrDefault(parentId, List.of())) {
            MenuCopyNodeDto d = new MenuCopyNodeDto();
            d.setId(m.getId());
            d.setParentId(m.getParentId());
            d.setDepth(depth);
            d.setLabel(m.getLabel());
            d.setIcon(m.getIcon());
            d.setMenuType(m.getMenuType() == null ? "WEB" : m.getMenuType().name());
            d.setHref(m.getHref());
            d.setPage(m.getPage());
            d.setIsActive(m.getIsActive());
            out.add(d);
            walk(m.getId(), depth + 1, byParent, out);
        }
    }

    /** Copies the requested items into every target tenant, one isolated transaction per
     *  target — a failure copying into one target never blocks or rolls back the others. */
    public List<MenuCopyTargetResultDto> copy(MenuCopyRequest req) {
        if (req.getSourceCompanyCode() == null || req.getItemIds() == null || req.getItemIds().isEmpty()
                || req.getTargetCompanyCodes() == null || req.getTargetCompanyCodes().isEmpty()) {
            throw new IllegalStateException("Choose a source tenant, at least one item, and at least one target tenant.");
        }
        String prev = TenantContext.getCompanyCode();
        try {
            TenantContext.setCompanyCode(req.getSourceCompanyCode());
            List<CompanyMenuSelf> sourceAll = ops.allRows();

            List<MenuCopyTargetResultDto> results = new ArrayList<>();
            for (String target : req.getTargetCompanyCodes().stream().distinct().toList()) {
                if (target.equals(req.getSourceCompanyCode())) continue; // copying a tenant onto itself is a no-op
                try {
                    TenantContext.setCompanyCode(target);
                    results.add(ops.copyInto(target, req.getSourceCompanyCode(), req.getItemIds(), sourceAll));
                } catch (Exception e) {
                    results.add(new MenuCopyTargetResultDto(target, 0, 0, e.getMessage()));
                }
            }
            return results;
        } finally {
            restore(prev);
        }
    }

    private void restore(String prev) {
        if (prev != null) TenantContext.setCompanyCode(prev); else TenantContext.clear();
    }
}
