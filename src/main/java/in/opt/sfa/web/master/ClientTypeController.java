package in.opt.sfa.web.master;

import in.opt.sfa.tenant.entity.ClientType;
import in.opt.sfa.tenant.repository.ClientTypeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Client Type master — drives the dynamic Doctor/Chemist/Stockist label. Tenant-routed. */
@Tag(name = "Client Type Master", description = "Client types that drive the dynamic labels")
@RestController
@RequestMapping("/api/master/client-type")
public class ClientTypeController {

    private final ClientTypeRepository clientTypes;

    public ClientTypeController(ClientTypeRepository clientTypes) {
        this.clientTypes = clientTypes;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ClientType> list() {
        return clientTypes.findAllByOrderByOidAsc();
    }

    @GetMapping("/{oid}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ClientType get(@PathVariable Long oid) {
        return clientTypes.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Client type not found: " + oid));
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientType save(@RequestBody ClientType form) {
        // Sensible defaults, mirroring the websfa controller.
        if (form.getSingularLabel() == null || form.getSingularLabel().isBlank()) {
            form.setSingularLabel(form.getTypeName());
        }
        if (form.getPluralLabel() == null || form.getPluralLabel().isBlank()) {
            form.setPluralLabel(form.getTypeName() == null ? null : form.getTypeName() + "s");
        }
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("Y");
        }
        return clientTypes.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(@RequestBody List<ClientType> forms) {
        int saved = 0, skipped = 0;
        for (ClientType f : forms) {
            if (RouteController.isBlank(f.getTypeCode()) || RouteController.isBlank(f.getTypeName())
                    || clientTypes.findByTypeCode(f.getTypeCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setTypeCode(f.getTypeCode().trim());
            f.setTypeName(f.getTypeName().trim());
            if (RouteController.isBlank(f.getSingularLabel())) f.setSingularLabel(f.getTypeName());
            if (RouteController.isBlank(f.getPluralLabel())) f.setPluralLabel(f.getTypeName() + "s");
            f.setStatus("Y");
            clientTypes.save(f);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientType status(@PathVariable Long oid, @RequestParam String status) {
        ClientType t = clientTypes.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Client type not found: " + oid));
        t.setStatus(status);
        return clientTypes.save(t);
    }
}
