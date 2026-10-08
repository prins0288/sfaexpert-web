package in.opt.sfa.tenant.master.degree.controller;

import in.opt.sfa.tenant.master.degree.dto.DegreeMasterDto;
import in.opt.sfa.tenant.master.degree.service.DegreeMasterService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Degree master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/degree-master")
@Tag(name = "Degree Master", description = "Qualifications (degrees) with short name, display order, soft-delete status and audit trail")
public class DegreeMasterController {

    private final DegreeMasterService service;

    public DegreeMasterController(DegreeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all degrees (active + inactive)")
    public List<DegreeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one degree by id")
    public DegreeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("DEGREE_SAVE")
    @PostMapping
    @Operation(summary = "Create or update a degree",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public DegreeMasterDto save(@RequestBody DegreeMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("DEGREE_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create degrees (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<DegreeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("DEGREE_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a degree (status = true or false)")
    public DegreeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
