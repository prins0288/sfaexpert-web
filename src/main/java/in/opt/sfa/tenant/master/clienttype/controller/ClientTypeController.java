package in.opt.sfa.tenant.master.clienttype.controller;

import in.opt.sfa.tenant.master.clienttype.dto.ClientTypeDto;
import in.opt.sfa.tenant.master.clienttype.service.ClientTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Client Type master — drives the dynamic Doctor/Chemist/Stockist label. Tenant-routed. */
@Tag(name = "Client Type Master", description = "Client types that drive the dynamic labels")
@RestController
@RequestMapping("/api/master/client-type")
public class ClientTypeController {

    private final ClientTypeService service;

    public ClientTypeController(ClientTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClientTypeDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public ClientTypeDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public ClientTypeDto save(@RequestBody ClientTypeDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<ClientTypeDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public ClientTypeDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
