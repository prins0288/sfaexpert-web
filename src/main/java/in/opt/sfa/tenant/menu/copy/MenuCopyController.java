package in.opt.sfa.tenant.menu.copy;

import in.opt.sfa.tenant.menu.copy.dto.MenuCopyNodeDto;
import in.opt.sfa.tenant.menu.copy.dto.MenuCopyRequest;
import in.opt.sfa.tenant.menu.copy.dto.MenuCopyTargetResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Menu Copy (multi-tenant) — copy one or more menu items, with their full
 * subtree at any depth, from one tenant into one or more other tenants.
 * Deliberately cross-tenant: source and targets are chosen explicitly, not
 * implied by the caller's own login.
 */
@RestController
@RequestMapping("/api/menu-copy")
@Tag(name = "Menu Copy", description = "Copy menu items (with full subtree) from one tenant into others")
public class MenuCopyController {

    private final MenuCopyService service;

    public MenuCopyController(MenuCopyService service) {
        this.service = service;
    }

    @GetMapping("/tenants")
    @Operation(summary = "Every configured tenant id")
    public List<String> tenants() {
        return service.tenants();
    }

    @GetMapping("/tree")
    @Operation(summary = "The full menu tree of the given tenant, for picking items to copy")
    public List<MenuCopyNodeDto> tree(@RequestParam String companyCode) {
        return service.tree(companyCode);
    }

    @PostMapping
    @Operation(summary = "Copy the given item ids (each with its full subtree) from sourceTenant into every targetTenants entry")
    public List<MenuCopyTargetResultDto> copy(@RequestBody MenuCopyRequest req) {
        return service.copy(req);
    }
}
