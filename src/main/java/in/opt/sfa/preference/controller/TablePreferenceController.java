package in.opt.sfa.preference.controller;

import in.opt.sfa.preference.dto.ColumnConfig;
import in.opt.sfa.preference.dto.TablePreferenceDto;
import in.opt.sfa.preference.service.TablePreferenceService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Column Settings for ANY registered grid (see DefaultColumnRegistry) — one
 * controller for every report. The layout is COMPANY-WIDE: every user of the
 * company reads the same one; changing or resetting it needs the COLUMN_SETTINGS
 * permission (denied by default — granted per emp_id / designation / emp_level).
 * The company comes from the JWT; nothing identifying is sent by the client.
 */
@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Table Preferences", description = "Company-wide column order / visibility for report grids")
public class TablePreferenceController {

    private final TablePreferenceService service;

    public TablePreferenceController(TablePreferenceService service) {
        this.service = service;
    }

    @GetMapping("/{screenKey}")
    @Operation(summary = "Effective column layout for a screen",
            description = "The saved layout merged with the current defaults (new columns added, stale ones dropped); " +
                    "the defaults if nothing was saved (customized=false).")
    public TablePreferenceDto get(@PathVariable String screenKey) {
        return service.get(screenKey);
    }

    @PostMapping("/{screenKey}")
    @Operation(summary = "Save the company's column layout for a screen (upsert, needs COLUMN_SETTINGS)",
            description = "Body: [{field, order, visible, width?, extraSettings?}]. Unknown fields are ignored.")
    @RequiresPermission("COLUMN_SETTINGS")
    public TablePreferenceDto save(@PathVariable String screenKey, @RequestBody List<ColumnConfig> columns) {
        return service.save(screenKey, columns);
    }

    @DeleteMapping("/{screenKey}")
    @Operation(summary = "Reset a screen to its default columns for the whole company (needs COLUMN_SETTINGS)")
    @RequiresPermission("COLUMN_SETTINGS")
    public TablePreferenceDto reset(@PathVariable String screenKey) {
        return service.reset(screenKey);
    }
}
