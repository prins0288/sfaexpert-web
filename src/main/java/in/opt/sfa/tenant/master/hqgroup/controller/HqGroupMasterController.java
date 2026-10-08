package in.opt.sfa.tenant.master.hqgroup.controller;

import in.opt.sfa.tenant.master.hqgroup.dto.HqGroupMasterDto;
import in.opt.sfa.tenant.master.hqgroup.service.HqGroupMasterService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** HQ-group master — a wider region grouping several HQs. Tenant-routed. */
@RestController
@RequestMapping("/api/master/hq-group-master")
@Tag(name = "HQ Group Master", description = "HQ groups (regions/clusters) with short name, display order, soft-delete status and audit trail; optionally referenced from hq_master.hq_group_id")
public class HqGroupMasterController {

    private final HqGroupMasterService service;

    public HqGroupMasterController(HqGroupMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all HQ groups (active + inactive)")
    public List<HqGroupMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one HQ group by id")
    public HqGroupMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("HQ_GROUP_SAVE")
    @PostMapping
    @Operation(summary = "Create or update an HQ group",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public HqGroupMasterDto save(@RequestBody HqGroupMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("HQ_GROUP_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create HQ groups (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<HqGroupMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("HQ_GROUP_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate an HQ group (status = true or false)")
    public HqGroupMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
