package in.opt.sfa.tenant.settings;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.common.service.LevelRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Company-wide settings as a flat key/value map — e.g. dateFormat,
 * contentProtection. Stored per tenant in the COMMON db (company_setting_master,
 * scoped by CompanySettingStore). One page (settings/general.html) reads/writes
 * this whole map, so adding a new setting later is just a new key — no migration
 * and no new endpoint.
 */
@Tag(name = "Settings", description = "Company-wide settings (date format, content protection, etc.) as a key/value map")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    /** Applied when a key has never been saved for this tenant. */
    private static final Map<String, String> DEFAULTS = Map.of(
            "dateFormat", "dd-MM-yyyy",
            "contentProtection", "N",
            in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService.FLOW_KEY,
            in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService.DEFAULT_FLOW,
            LevelRoleService.MANAGER_KEY, LevelRoleService.DEFAULTS.get(LevelRoleService.MANAGER_KEY),
            LevelRoleService.ADMIN_KEY, LevelRoleService.DEFAULTS.get(LevelRoleService.ADMIN_KEY),
            LevelRoleService.SUPER_ADMIN_KEY, LevelRoleService.DEFAULTS.get(LevelRoleService.SUPER_ADMIN_KEY)
    );

    private final CompanySettingStore settings;
    private final LevelRoleService levelRoles;

    public SettingsController(CompanySettingStore settings, LevelRoleService levelRoles) {
        this.settings = settings;
        this.levelRoles = levelRoles;
    }

    @GetMapping
    @Operation(summary = "All settings, defaults filled in for anything never saved")
    public Map<String, String> get() {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        result.putAll(settings.all());
        return result;
    }

    @PostMapping
    @Operation(summary = "Upsert one or more settings; returns the full merged map")
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, String> save(@RequestBody Map<String, String> body) {
        String flowKey = in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService.FLOW_KEY;
        if (body.containsKey(flowKey)) {
            // who approves clients is a company policy: admins only, and only a known flow
            in.opt.sfa.security.Authz.requireRole("ADMIN");
            if (!in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService.FLOWS.contains(body.get(flowKey))) {
                throw new IllegalStateException("Client approval must be one of " + in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService.FLOWS);
            }
        }
        if (LevelRoleService.KEYS.stream().anyMatch(body::containsKey)) {
            // the level ranges decide everyone's role: super admin only
            in.opt.sfa.security.Authz.requireRole("SUPER_ADMIN");
            levelRoles.validate(body);
            LevelRoleService.KEYS.forEach(k -> { if (body.containsKey(k)) body.put(k, body.get(k).trim()); });
        }
        body.forEach(settings::put);
        return get();
    }
}
