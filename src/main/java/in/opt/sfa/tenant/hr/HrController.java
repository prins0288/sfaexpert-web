package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.hr.org.HrEmploymentService;
import in.opt.sfa.tenant.hr.org.HrMasterService;
import in.opt.sfa.tenant.hr.setting.HrSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Tag(name = "HR", description = "HR foundation: company set-up, settings, roles, employee HR facts, audit log")
@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final HrMasterService masters;
    private final HrSettingService settings;
    private final HrAccessService access;
    private final HrEmploymentService employment;
    private final AuditLogService audit;

    public HrController(HrMasterService masters, HrSettingService settings, HrAccessService access, HrEmploymentService employment, AuditLogService audit) {
        this.masters = masters; this.settings = settings; this.access = access; this.employment = employment; this.audit = audit;
    }

    private static String s(Map<String, Object> b, String k) { Object v = b.get(k); return v == null ? null : v.toString(); }

    // ---- who am I ---------------------------------------------------------------------------------------
    @GetMapping("/me") public Map<String, Object> me() { return access.me(); }

    // ---- company + masters -------------------------------------------------------------------------------
    @GetMapping("/company") public Map<String, Object> company() { return masters.company(); }
    @PostMapping("/company") public Map<String, Object> saveCompany(@RequestBody Map<String, Object> b) { masters.saveCompany(b, s(b, "reason")); return Map.of("saved", true); }
    @GetMapping("/states") public List<Map<String, Object>> states() { return masters.states(); }
    @GetMapping("/{kind:branches|departments|grades|cost-centers|employee-types}") public List<Map<String, Object>> list(@PathVariable String kind) { return masters.list(kind); }
    @PostMapping("/{kind:branches|departments|grades|cost-centers|employee-types}") public Map<String, Object> save(@PathVariable String kind, @RequestBody Map<String, Object> b) { return masters.save(kind, b); }
    @PostMapping("/{kind:branches|departments|grades|cost-centers|employee-types}/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@PathVariable String kind, @RequestBody Map<String, Object> b) {
        List<Long> ids = ((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList();
        return Map.of("deleted", masters.delete(kind, ids, s(b, "reason")));
    }

    // ---- settings ----------------------------------------------------------------------------------------
    @GetMapping("/settings") public List<Map<String, Object>> settingList() { return settings.catalogue(); }
    @GetMapping("/settings/history") public List<Map<String, Object>> settingHistory(@RequestParam String key) { return settings.history(key); }
    @PostMapping("/settings")
    public Map<String, Object> settingSave(@RequestBody Map<String, Object> b) {
        settings.save(s(b, "key"), s(b, "value"), LocalDate.parse(s(b, "effectiveFrom")), s(b, "reason"));
        return Map.of("saved", true);
    }

    // ---- roles ---------------------------------------------------------------------------------------------
    @GetMapping("/roles/matrix") public Map<String, Object> matrix() { return access.matrix(); }
    @PostMapping("/roles/{id}/permissions")
    @SuppressWarnings("unchecked")
    public Map<String, Object> rolePerms(@PathVariable long id, @RequestBody Map<String, Object> b) {
        access.setRolePermissions(id, ((List<Object>) b.getOrDefault("permissions", List.of())).stream().map(Object::toString).toList(), s(b, "reason"));
        return Map.of("saved", true);
    }
    @GetMapping("/roles/assignments") public List<Map<String, Object>> assignments() { return access.assignments(); }
    @PostMapping("/employees/{empId}/roles")
    @SuppressWarnings("unchecked")
    public Map<String, Object> empRoles(@PathVariable String empId, @RequestBody Map<String, Object> b) {
        access.setEmployeeRoles(empId, ((List<Object>) b.getOrDefault("roles", List.of())).stream().map(Object::toString).toList(), s(b, "reason"));
        return Map.of("saved", true);
    }

    // ---- employee HR facts ---------------------------------------------------------------------------------
    @GetMapping("/employees") public List<Map<String, Object>> employees(@RequestParam(required = false) String q) { return employment.list(q); }
    @GetMapping("/employees/{empId}") public Map<String, Object> employee(@PathVariable String empId) { return employment.get(empId); }
    @PostMapping("/employees/{empId}") public Map<String, Object> saveEmployee(@PathVariable String empId, @RequestBody Map<String, Object> b) { employment.save(empId, b, s(b, "reason")); return Map.of("saved", true); }

    // ---- audit -----------------------------------------------------------------------------------------------
    @GetMapping("/audit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> audit(@RequestParam(required = false) String entity, @RequestParam(required = false) String empId, @RequestParam(required = false) String by,
                                           @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(defaultValue = "200") int limit) {
        access.require("HR_AUDIT_VIEW");
        return audit.search(entity, empId, by, from, to, limit);
    }
}
