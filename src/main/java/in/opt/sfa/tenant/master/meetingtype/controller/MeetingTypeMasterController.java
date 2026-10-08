package in.opt.sfa.tenant.master.meetingtype.controller;

import in.opt.sfa.tenant.master.meetingtype.dto.MeetingTypeMasterDto;
import in.opt.sfa.tenant.master.meetingtype.service.MeetingTypeMasterService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Meeting-type master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/meeting-type-master")
@Tag(name = "Meeting Type Master", description = "Meeting classifications (General / Monthly / Sales Meeting...) with short name, display order, soft-delete status and audit trail")
public class MeetingTypeMasterController {

    private final MeetingTypeMasterService service;

    public MeetingTypeMasterController(MeetingTypeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all meeting types (active + inactive)")
    public List<MeetingTypeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one meeting type by id")
    public MeetingTypeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("MEETING_TYPE_SAVE")
    @PostMapping
    @Operation(summary = "Create or update a meeting type",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public MeetingTypeMasterDto save(@RequestBody MeetingTypeMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("MEETING_TYPE_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create meeting types (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<MeetingTypeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("MEETING_TYPE_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a meeting type (status = true or false)")
    public MeetingTypeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
