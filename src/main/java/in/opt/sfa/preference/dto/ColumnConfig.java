package in.opt.sfa.preference.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One column of a configurable grid. {@code field} is the stable key — it
 * matches the {@code data-field} attribute on the page's {@code <th>} — and is
 * the only thing a saved layout is matched on; everything else can change
 * between releases without breaking saved rows.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // a field dropped in a later release must not break old JSON
public class ColumnConfig {

    private String field;
    /** i18n key for the header text (lang_*.properties / label_master). */
    private String labelKey;
    /** 0-based display position. */
    private Integer order;
    private Boolean visible;
    /** Optional CSS width, e.g. "120px" — null = let the grid size it. */
    private String width;
    /** Free-form per-column settings for later features (pinning, format, …). */
    private Map<String, Object> extraSettings;

    public ColumnConfig(String field, String labelKey, boolean visible) {
        this.field = field;
        this.labelKey = labelKey;
        this.visible = visible;
    }

    public ColumnConfig copy() {
        ColumnConfig c = new ColumnConfig();
        c.field = field;
        c.labelKey = labelKey;
        c.order = order;
        c.visible = visible;
        c.width = width;
        c.extraSettings = extraSettings == null ? null : new LinkedHashMap<>(extraSettings);
        return c;
    }
}
