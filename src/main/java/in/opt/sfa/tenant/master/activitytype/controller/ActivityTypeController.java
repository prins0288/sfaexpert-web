package in.opt.sfa.tenant.master.activitytype.controller;

import in.opt.sfa.tenant.master.activitytype.dto.ActivityTypeDto;
import in.opt.sfa.tenant.master.activitytype.service.ActivityTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Activity Type Master master — same pattern as Client Type. Tenant-routed. */
@Tag(name = "Activity Type Master Master", description = "Activity Type Master lookup that drives the dynamic labels")
@RestController
@RequestMapping("/api/master/activity-type")
public class ActivityTypeController {

    private final ActivityTypeService service;

    public ActivityTypeController(ActivityTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityTypeDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public ActivityTypeDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public ActivityTypeDto save(@RequestBody ActivityTypeDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<ActivityTypeDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public ActivityTypeDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
