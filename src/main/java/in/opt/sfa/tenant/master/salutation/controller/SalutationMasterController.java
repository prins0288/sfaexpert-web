package in.opt.sfa.tenant.master.salutation.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.salutation.entity.SalutationMaster;
import in.opt.sfa.tenant.master.salutation.service.SalutationMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Master Entry > Salutation (Dr., Mr., Mrs. ...). */
@RestController
@RequestMapping("/api/master/salutation")
@Tag(name = "Salutation Master", description = "Titles before a name; forms store the chosen text, not the id")
public class SalutationMasterController {

    private final SalutationMasterService service;

    public SalutationMasterController(SalutationMasterService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalutationMaster> list() {
        return service.list();
    }

    @RequiresPermission("SALUTATION_SAVE")
    @PostMapping
    @Operation(summary = "Create (no id) or update; body {id, salutation, displayOrder}")
    public SalutationMaster save(@RequestBody Map<String, Object> body) {
        Object id = body.get("id"), order = body.get("displayOrder");
        return service.save(id == null || id.toString().isBlank() ? null : Long.valueOf(id.toString()),
                (String) body.get("salutation"),
                order == null || order.toString().isBlank() ? null : Integer.valueOf(order.toString()));
    }

    @RequiresPermission("SALUTATION_SAVE")
    @PostMapping("/{id}/status")
    public SalutationMaster status(@PathVariable Long id, @RequestParam boolean active) {
        return service.setActive(id, active);
    }
}
