package in.opt.sfa.tenant.master.itemtype.controller;

import in.opt.sfa.tenant.master.itemtype.dto.ItemTypeMasterDto;
import in.opt.sfa.tenant.master.itemtype.service.ItemTypeMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Item-type master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/item-type-master")
@Tag(name = "Item Type Master", description = "Product dosage forms (Tablet, Capsule, Syrup...) with short name, display order, soft-delete status and audit trail")
public class ItemTypeMasterController {

    private final ItemTypeMasterService service;

    public ItemTypeMasterController(ItemTypeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all item types (active + inactive)")
    public List<ItemTypeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one item type by id")
    public ItemTypeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create or update an item type",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public ItemTypeMasterDto save(@RequestBody ItemTypeMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create item types (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<ItemTypeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate an item type (status = true or false)")
    public ItemTypeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
