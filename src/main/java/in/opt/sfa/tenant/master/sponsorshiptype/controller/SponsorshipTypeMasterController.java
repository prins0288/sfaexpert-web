package in.opt.sfa.tenant.master.sponsorshiptype.controller;

import in.opt.sfa.tenant.master.sponsorshiptype.dto.SponsorshipTypeMasterDto;
import in.opt.sfa.tenant.master.sponsorshiptype.service.SponsorshipTypeMasterService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Sponsorship Type master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/sponsorship-type-master")
@Tag(name = "Sponsorship Type Master", description = "Sponsorship Type lookup with short name, display order, soft-delete status and audit trail")
public class SponsorshipTypeMasterController {

    private final SponsorshipTypeMasterService service;

    public SponsorshipTypeMasterController(SponsorshipTypeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all (active + inactive)")
    public List<SponsorshipTypeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one by id")
    public SponsorshipTypeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("SPONSORSHIP_TYPE_SAVE")
    @PostMapping
    @Operation(summary = "Create or update",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public SponsorshipTypeMasterDto save(@RequestBody SponsorshipTypeMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("SPONSORSHIP_TYPE_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<SponsorshipTypeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("SPONSORSHIP_TYPE_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate (status = true or false)")
    public SponsorshipTypeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
