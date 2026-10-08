package in.opt.sfa.tenant.master.traveltype.controller;

import in.opt.sfa.tenant.master.traveltype.dto.TravelTypeMasterDto;
import in.opt.sfa.tenant.master.traveltype.service.TravelTypeMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Travel-type master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/travel-type-master")
@Tag(name = "Travel Type Master", description = "Travel / tour classifications (Tour, Headquarter, Honeymoon Tour, Sale Tour...) with short name, display order, soft-delete status and audit trail")
public class TravelTypeMasterController {

    private final TravelTypeMasterService service;

    public TravelTypeMasterController(TravelTypeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all travel types (active + inactive)")
    public List<TravelTypeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one travel type by id")
    public TravelTypeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create or update a travel type",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public TravelTypeMasterDto save(@RequestBody TravelTypeMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create travel types (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<TravelTypeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a travel type (status = true or false)")
    public TravelTypeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
