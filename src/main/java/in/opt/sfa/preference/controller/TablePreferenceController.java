package in.opt.sfa.preference.controller;

import in.opt.sfa.preference.dto.ColumnConfig;
import in.opt.sfa.preference.dto.TablePreferenceDto;
import in.opt.sfa.preference.service.TablePreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Column Settings for ANY registered grid (see DefaultColumnRegistry) — one
 * controller for every report. The layout belongs to the logged-in user in
 * their company; both come from the JWT, nothing identifying is sent by the client.
 */
@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Table Preferences", description = "Per-user column order / visibility for report grids")
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
    @Operation(summary = "Save the column layout for a screen (upsert)",
            description = "Body: [{field, order, visible, width?, extraSettings?}]. Unknown fields are ignored.")
    public TablePreferenceDto save(@PathVariable String screenKey, @RequestBody List<ColumnConfig> columns) {
        return service.save(screenKey, columns);
    }

    @DeleteMapping("/{screenKey}")
    @Operation(summary = "Reset a screen to its default columns")
    public TablePreferenceDto reset(@PathVariable String screenKey) {
        return service.reset(screenKey);
    }
}
