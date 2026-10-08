package in.opt.sfa.tenant.master.speciality.controller;

import in.opt.sfa.tenant.master.speciality.dto.SpecialityMasterDto;
import in.opt.sfa.tenant.master.speciality.service.SpecialityMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Speciality master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/speciality-master")
@Tag(name = "Speciality Master", description = "Specialities with icon, soft-delete status and audit trail")
public class SpecialityMasterController {

    private final SpecialityMasterService service;

    public SpecialityMasterController(SpecialityMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all specialities (active + inactive)")
    public List<SpecialityMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    @Operation(summary = "Get one speciality by id")
    public SpecialityMasterDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    @Operation(summary = "Create or update a speciality",
            description = "Send oid to update, omit it to create. Audit fields are set by the server.")
    public SpecialityMasterDto save(@RequestBody SpecialityMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create specialities (blank / duplicate code rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<SpecialityMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    @Operation(summary = "Activate / deactivate a speciality (status = true or false)")
    public SpecialityMasterDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
