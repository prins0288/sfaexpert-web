package in.opt.sfa.tenant.menu;

import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import in.opt.sfa.tenant.menu.dto.MenuAuditLogDto;
import in.opt.sfa.tenant.menu.entity.MenuAuditLog;
import in.opt.sfa.tenant.menu.repository.MenuAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Backs the "Menu Audit Report" page — who added/edited/(de)activated/deleted which menu item, and when. */
@Service
public class MenuAuditLogService {

    private final MenuAuditLogRepository repo;
    private final EmployeeNameResolver employeeNames;

    public MenuAuditLogService(MenuAuditLogRepository repo, EmployeeNameResolver employeeNames) {
        this.repo = repo;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MenuAuditLogDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return repo.findAllByOrderByChangedAtDesc().stream().map(e -> toDto(e, names)).collect(Collectors.toList());
    }

    private MenuAuditLogDto toDto(MenuAuditLog e, Map<String, String> names) {
        MenuAuditLogDto d = new MenuAuditLogDto();
        d.setId(e.getId());
        d.setMenuItemId(e.getMenuItemId());
        d.setLabel(e.getLabel());
        d.setMenuType(e.getMenuType());
        d.setAction(e.getAction());
        d.setChangedBy(e.getChangedBy());
        d.setChangedByName(employeeNames.resolve(names, e.getChangedBy()));
        d.setChangedAt(e.getChangedAt());
        d.setDetails(e.getDetails());
        return d;
    }
}
