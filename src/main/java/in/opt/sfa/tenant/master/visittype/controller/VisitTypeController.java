package in.opt.sfa.tenant.master.visittype.controller;

import in.opt.sfa.tenant.master.visittype.dto.VisitTypeDto;
import in.opt.sfa.tenant.master.visittype.service.VisitTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Visit Type Master master — same pattern as Client Type. Tenant-routed. */
@Tag(name = "Visit Type Master Master", description = "Visit Type Master lookup that drives the dynamic labels")
@RestController
@RequestMapping("/api/master/visit-type")
public class VisitTypeController {

    private final VisitTypeService service;

    public VisitTypeController(VisitTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<VisitTypeDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public VisitTypeDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public VisitTypeDto save(@RequestBody VisitTypeDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<VisitTypeDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public VisitTypeDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
