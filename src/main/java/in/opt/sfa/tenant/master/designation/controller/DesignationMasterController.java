package in.opt.sfa.tenant.master.designation.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.designation.dto.DesignationMasterDto;
import in.opt.sfa.tenant.master.designation.service.DesignationMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Designation master — Masters > Master Entry > Create Designation. Tenant-routed. */
@RestController
@RequestMapping("/api/master/designation-master")
@Tag(name = "Designation Master", description = "Designations with their emp level (drives the employee's role)")
public class DesignationMasterController {

    private final DesignationMasterService service;

    public DesignationMasterController(DesignationMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all designations (active + inactive) with role and employee count")
    public List<DesignationMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    @Operation(summary = "Get one designation by id")
    public DesignationMasterDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @RequiresPermission("DESIGNATION_SAVE")
    @PostMapping
    @Operation(summary = "Create or update a designation",
            description = "Send oid to update, omit it to create. Changing empLevel re-stamps every employee of the designation.")
    public DesignationMasterDto save(@RequestBody DesignationMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("DESIGNATION_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create designations (invalid / duplicate rows are skipped with a reason)")
    public Map<String, Object> saveMultiple(@RequestBody List<DesignationMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("DESIGNATION_STATUS")
    @PostMapping("/{oid}/status")
    @Operation(summary = "Activate / deactivate a designation (status = true or false)")
    public DesignationMasterDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
