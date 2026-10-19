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
@Tag(name = "Designation Master", description = "Designations: hierarchy level (drives the employee's role), display order, grade and category")
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

    @GetMapping("/options")
    @Operation(summary = "Category choices and the company's role ranges (role by hierarchy level)")
    public Map<String, Object> options() {
        return service.options();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one designation by id")
    public DesignationMasterDto get(@PathVariable Integer id) {
        return service.get(id);
    }

    @RequiresPermission("DESIGNATION_SAVE")
    @PostMapping
    @Operation(summary = "Create or update a designation",
            description = "Send id to update, omit it to create. Designation and hierarchy level must be unique.")
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
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a designation (status = true or false); one still held by employees cannot be deactivated")
    public DesignationMasterDto status(@PathVariable Integer id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
