package in.opt.sfa.tenant.menu;

import in.opt.sfa.tenant.menu.dto.MenuAuditLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Menu Audit Report — every add / edit / activate / deactivate / delete made via Menu Master. Tenant-routed. */
@RestController
@RequestMapping("/api/menu-audit-log")
@Tag(name = "Menu Audit Report", description = "Who changed the menu, what changed, and when")
public class MenuAuditLogController {

    private final MenuAuditLogService service;

    public MenuAuditLogController(MenuAuditLogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List every menu change, newest first")
    public List<MenuAuditLogDto> list() {
        return service.list();
    }
}
