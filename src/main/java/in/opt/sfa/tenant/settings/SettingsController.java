package in.opt.sfa.tenant.settings;

import in.opt.sfa.common.service.CompanySettingStore;
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
            "contentProtection", "N"
    );

    private final CompanySettingStore settings;

    public SettingsController(CompanySettingStore settings) {
        this.settings = settings;
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
        body.forEach(settings::put);
        return get();
    }
}
