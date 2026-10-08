package in.opt.sfa.tenant.menu;

import in.opt.sfa.tenant.menu.dto.MenuMasterDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Menu Master — create/edit/activate/deactivate/delete ANY menu item (WEB, APP or BOTH). Tenant-routed. */
@RestController
@RequestMapping("/api/menu-master")
@Tag(name = "Menu Master", description = "Full CRUD over the menu tree — add/update/activate/deactivate/delete any WEB or APP item")
public class MenuMasterController {

    private final MenuMasterService service;

    public MenuMasterController(MenuMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Every menu item, flattened in tree order with depth + breadcrumb")
    public List<MenuMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one menu item by id")
    public MenuMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a new menu item")
    public MenuMasterDto create(@RequestBody MenuMasterDto form) {
        return service.create(form);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing menu item")
    public MenuMasterDto update(@PathVariable Long id, @RequestBody MenuMasterDto form) {
        form.setId(id);
        return service.update(form);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a menu item (status = true or false)")
    public MenuMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a menu item (blocked while it still has sub-items)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
