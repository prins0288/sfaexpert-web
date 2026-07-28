package in.opt.sfa.web.master;

import in.opt.sfa.tenant.entity.SpecialityMaster;
import in.opt.sfa.tenant.repository.SpecialityMasterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Speciality master — production-style CRUD with audit + soft-delete, mirroring
 * {@link CategoryMasterController}. Tenant-routed. On update the existing row is
 * loaded and only editable fields are copied, so audit created_* is preserved.
 */
@RestController
@RequestMapping("/api/master/speciality-master")
@Tag(name = "Speciality Master", description = "Specialities with icon, soft-delete status and audit trail")
public class SpecialityMasterController {

    private final SpecialityMasterRepository specialities;

    public SpecialityMasterController(SpecialityMasterRepository specialities) {
        this.specialities = specialities;
    }

    @GetMapping
    @Operation(summary = "List all specialities (active + inactive)")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<SpecialityMaster> list() {
        return specialities.findAllByOrderByOidAsc();
    }

    @GetMapping("/{oid}")
    @Operation(summary = "Get one speciality by id")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public SpecialityMaster get(@PathVariable Long oid) {
        return specialities.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Speciality not found: " + oid));
    }

    @PostMapping
    @Operation(summary = "Create or update a speciality",
            description = "Send oid to update, omit it to create. Audit fields are set by the server.")
    @Transactional(transactionManager = "tenantTransactionManager")
    public SpecialityMaster save(@RequestBody SpecialityMaster form) {
        SpecialityMaster target = form.getOid() != null
                ? specialities.findById(form.getOid())
                    .orElseThrow(() -> new IllegalStateException("Speciality not found: " + form.getOid()))
                : new SpecialityMaster();

        target.setSpecialityCode(trim(form.getSpecialityCode()));
        target.setSpecialityName(trim(form.getSpecialityName()));
        target.setDescription(form.getDescription());
        target.setIcon(form.getIcon());
        if (form.getStatus() != null) {
            target.setStatus(form.getStatus());
        }
        return specialities.save(target);   // @PrePersist / @PreUpdate fill the audit columns
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create specialities (blank / duplicate code rows are skipped)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(@RequestBody List<SpecialityMaster> forms) {
        int saved = 0, skipped = 0;
        for (SpecialityMaster f : forms) {
            String code = trim(f.getSpecialityCode());
            if (isBlank(code) || isBlank(f.getSpecialityName())
                    || specialities.findBySpecialityCode(code).isPresent()) {
                skipped++;
                continue;
            }
            SpecialityMaster s = new SpecialityMaster();
            s.setSpecialityCode(code);
            s.setSpecialityName(trim(f.getSpecialityName()));
            s.setDescription(f.getDescription());
            s.setIcon(f.getIcon());
            s.setStatus(Boolean.TRUE);
            specialities.save(s);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @PostMapping("/{oid}/status")
    @Operation(summary = "Activate / deactivate a speciality (status = true or false)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public SpecialityMaster status(@PathVariable Long oid, @RequestParam boolean status) {
        SpecialityMaster s = specialities.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Speciality not found: " + oid));
        s.setStatus(status);
        return specialities.save(s);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
}
