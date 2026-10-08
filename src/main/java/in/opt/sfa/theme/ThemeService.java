package in.opt.sfa.theme;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.theme.dto.ThemeConfig;
import in.opt.sfa.theme.dto.ThemeUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves and persists the appearance — PER TENANT, not per user.
 *
 * Every company gets ONE look: whoever edits Appearance changes it for that
 * whole tenant, and another tenant is completely unaffected. Persistence is
 * therefore in the TENANT database (each tenant owns its own), stored as
 * key/value rows in company_setting_master alongside the other company-wide
 * settings (dateFormat, contentProtection, ...) rather than in its own table:
 *
 *   theme.navLayout / theme.mode / theme.preset / theme.fontScale
 *   theme.density   / theme.sidebarCollapsed
 *   theme.{mode}.{tokenKey}   e.g. "theme.light.sidebar-bg" = "#12294d"
 *
 * Resolution order for every token (lowest to highest precedence):
 *   1. the mode default    (ThemeToken.defaultFor(mode))
 *   2. the preset delta    (ThemePreset.delta(), if a preset is picked)
 *   3. the tenant override (a theme.{mode}.{key} row)
 *
 * The result is a full {@link ThemeConfig}: the top-level choices, the metadata
 * that drives the customizer UI, and the fully-resolved token maps for BOTH
 * modes so the browser can flip light/dark instantly with no round-trip.
 */
@Service
public class ThemeService {

    private static final String P = "theme.";              // key prefix for every theme row
    private static final String K_NAV = P + "navLayout";
    private static final String K_MODE = P + "mode";
    private static final String K_PRESET = P + "preset";
    private static final String K_FONT = P + "fontScale";
    private static final String K_DENSITY = P + "density";
    private static final String K_COLLAPSED = P + "sidebarCollapsed";

    private final CompanySettingStore settings;

    public ThemeService(CompanySettingStore settings) {
        this.settings = settings;
    }

    /** Fully-resolved config for the CURRENT tenant (defaults if nothing saved yet). */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public ThemeConfig get() {
        return build(load());
    }

    /**
     * Apply a partial update: any non-null top-level field is changed; any tokens
     * in the map are upserted as overrides for the given mode (a null/blank value
     * removes the override so the token falls back to preset/default).
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public ThemeConfig update(ThemeUpdateRequest req) {
        Map<String, String> all = load();

        if (req.navLayout() != null) put(K_NAV, NavLayout.of(req.navLayout()).key());
        if (req.mode() != null)      put(K_MODE, ThemeMode.of(req.mode()).key());
        if (req.preset() != null)    put(K_PRESET, ThemePreset.of(req.preset()).key());
        if (req.fontScale() != null) put(K_FONT, sanitizeScale(req.fontScale()));
        if (req.density() != null)   put(K_DENSITY, "compact".equalsIgnoreCase(req.density()) ? "compact" : "comfortable");
        if (req.sidebarCollapsed() != null) put(K_COLLAPSED, String.valueOf(req.sidebarCollapsed()));

        if (req.tokens() != null && !req.tokens().isEmpty()) {
            String mode = ThemeMode.of(req.mode() != null ? req.mode() : all.getOrDefault(K_MODE, "light")).key();
            req.tokens().forEach((key, value) -> {
                if (ThemeToken.byKey(key).isEmpty()) return;         // ignore unknown keys
                String settingKey = P + mode + "." + key;
                if (value == null || value.isBlank()) remove(settingKey);   // blank => clear override
                else put(settingKey, value.trim());
            });
        }

        return build(load());
    }

    /** Remove every override for a mode (reset that mode back to preset/defaults). */
    @Transactional(transactionManager = "commonTransactionManager")
    public ThemeConfig resetMode(String mode) {
        String prefix = P + ThemeMode.of(mode).key() + ".";
        settings.rows().stream()
                .map(s -> s.getSettingKey())
                .filter(k -> k != null && k.startsWith(prefix))
                .forEach(settings::remove);
        return build(load());
    }

    // ---- persistence helpers -----------------------------------------------

    /** Every theme.* row as a flat key -> value map. */
    private Map<String, String> load() {
        Map<String, String> out = new LinkedHashMap<>();
        settings.all().forEach((k, v) -> {
            if (k != null && k.startsWith(P)) out.put(k, v);
        });
        return out;
    }

    private void put(String key, String value) {
        settings.put(key, value);
    }

    private void remove(String key) {
        settings.remove(key);
    }

    // ---- resolution ---------------------------------------------------------

    /** Build the resolved config from the flat settings map. */
    private ThemeConfig build(Map<String, String> all) {
        ThemePreset preset = ThemePreset.of(all.getOrDefault(K_PRESET, "default"));

        Map<String, String> ovLight = overridesFor(all, "light");
        Map<String, String> ovDark = overridesFor(all, "dark");

        Map<String, String> resolvedLight = resolve(ThemeMode.LIGHT, preset, ovLight);
        Map<String, String> resolvedDark = resolve(ThemeMode.DARK, preset, ovDark);

        Map<String, Map<String, String>> resolved = new LinkedHashMap<>();
        resolved.put("light", resolvedLight);
        resolved.put("dark", resolvedDark);

        return new ThemeConfig(
                TenantContext.getCompanyCode(),
                NavLayout.of(all.getOrDefault(K_NAV, "vertical")).key(),
                ThemeMode.of(all.getOrDefault(K_MODE, "light")).key(),
                preset.key(),
                all.getOrDefault(K_FONT, "1.0"),
                "compact".equals(all.get(K_DENSITY)) ? "compact" : "comfortable",
                Boolean.parseBoolean(all.getOrDefault(K_COLLAPSED, "false")),
                ThemeConfig.presetList(),
                ThemeConfig.groups(resolvedLight, resolvedDark, ovLight, ovDark),
                resolved);
    }

    /** Pull the "theme.{mode}.*" rows out as tokenKey -> value. */
    private Map<String, String> overridesFor(Map<String, String> all, String mode) {
        String prefix = P + mode + ".";
        Map<String, String> out = new LinkedHashMap<>();
        all.forEach((k, v) -> {
            if (k.startsWith(prefix) && v != null && !v.isBlank()) out.put(k.substring(prefix.length()), v);
        });
        return out;
    }

    /** mode default -> preset delta -> tenant override, for every token. */
    private Map<String, String> resolve(ThemeMode mode, ThemePreset preset, Map<String, String> overrides) {
        Map<String, String> out = new LinkedHashMap<>();
        for (ThemeToken token : ThemeToken.values()) {
            String value = token.defaultFor(mode.key());
            String presetVal = preset.delta().get(token);
            if (presetVal != null) value = presetVal;
            String ov = overrides == null ? null : overrides.get(token.key());
            if (ov != null && !ov.isBlank()) value = ov;
            out.put(token.key(), value);
        }
        return out;
    }

    /** Clamp the font scale to a sane range and a fixed precision. */
    private String sanitizeScale(String raw) {
        try {
            double d = Double.parseDouble(raw.trim());
            d = Math.max(0.85, Math.min(1.30, d));
            return String.valueOf(Math.round(d * 100.0) / 100.0);
        } catch (NumberFormatException e) {
            return "1.0";
        }
    }

    /** Convenience for other layers that just want a resolved map. */
    public Map<String, String> resolvedTokens(String mode) {
        return get().resolved().getOrDefault(ThemeMode.of(mode).key(), new LinkedHashMap<>());
    }

    /** Every group's tokens flat (used by callers that don't need grouping). */
    public List<String> allTokenKeys() {
        List<String> keys = new ArrayList<>();
        for (ThemeToken t : ThemeToken.values()) keys.add(t.key());
        return keys;
    }
}
