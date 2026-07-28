package in.opt.sfa.theme;

import in.opt.sfa.theme.dto.ThemeConfig;
import in.opt.sfa.theme.dto.ThemeUpdateRequest;
import in.opt.sfa.theme.entity.UserPreference;
import in.opt.sfa.theme.entity.UserThemeToken;
import in.opt.sfa.theme.repository.UserPreferenceRepository;
import in.opt.sfa.theme.repository.UserThemeTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves and persists a user's appearance.
 *
 * Resolution order for every token (lowest to highest precedence):
 *   1. the mode default   (ThemeToken.defaultFor(mode))
 *   2. the preset delta    (ThemePreset.delta(), if the user picked a preset)
 *   3. the user override   (user_theme_token row for that mode + key)
 *
 * The result is a full {@link ThemeConfig}: the top-level choices, the metadata
 * that drives the customizer UI, and the fully-resolved token maps for BOTH
 * modes so the browser can flip light/dark instantly with no round-trip.
 *
 * All persistence runs against the COMMON database.
 */
@Service
public class ThemeService {

    private final UserPreferenceRepository prefs;
    private final UserThemeTokenRepository tokens;

    public ThemeService(UserPreferenceRepository prefs, UserThemeTokenRepository tokens) {
        this.prefs = prefs;
        this.tokens = tokens;
    }

    /** Fully-resolved config for a user (creates sensible defaults if none saved). */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public ThemeConfig get(String username) {
        UserPreference pref = prefs.findByUsername(username).orElseGet(() -> defaultPref(username));
        List<UserThemeToken> overrides = tokens.findByUsername(username);
        return build(pref, overrides);
    }

    /**
     * Apply a partial update: any non-null top-level field is changed; any tokens
     * in the map are upserted as overrides for the given mode (a null/blank value
     * removes the override so the token falls back to preset/default).
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public ThemeConfig update(String username, ThemeUpdateRequest req) {
        UserPreference pref = prefs.findByUsername(username).orElseGet(() -> defaultPref(username));

        if (req.navLayout() != null)  pref.setNavLayout(NavLayout.of(req.navLayout()).key());
        if (req.mode() != null)       pref.setThemeMode(ThemeMode.of(req.mode()).key());
        if (req.preset() != null)     pref.setPreset(ThemePreset.of(req.preset()).key());
        if (req.fontScale() != null)  pref.setFontScale(sanitizeScale(req.fontScale()));
        if (req.density() != null)    pref.setDensity("compact".equalsIgnoreCase(req.density()) ? "compact" : "comfortable");
        if (req.sidebarCollapsed() != null) pref.setSidebarCollapsed(req.sidebarCollapsed());
        prefs.save(pref);

        if (req.tokens() != null && !req.tokens().isEmpty()) {
            String mode = ThemeMode.of(req.mode() != null ? req.mode() : pref.getThemeMode()).key();
            upsertTokens(username, mode, req.tokens());
        }

        return build(pref, tokens.findByUsername(username));
    }

    /** Remove every override for a user+mode (reset the mode back to preset/defaults). */
    @Transactional(transactionManager = "commonTransactionManager")
    public ThemeConfig resetMode(String username, String mode) {
        String m = ThemeMode.of(mode).key();
        tokens.deleteByUsernameAndMode(username, m);
        UserPreference pref = prefs.findByUsername(username).orElseGet(() -> defaultPref(username));
        return build(pref, tokens.findByUsername(username));
    }

    // ------------------------------------------------------------------------

    private void upsertTokens(String username, String mode, Map<String, String> values) {
        List<UserThemeToken> existing = tokens.findByUsernameAndMode(username, mode);
        Map<String, UserThemeToken> byKey = new LinkedHashMap<>();
        existing.forEach(t -> byKey.put(t.getTokenKey(), t));

        values.forEach((key, value) -> {
            if (ThemeToken.byKey(key).isEmpty()) return;          // ignore unknown keys
            UserThemeToken row = byKey.get(key);
            if (value == null || value.isBlank()) {
                if (row != null) tokens.delete(row);              // blank => clear override
                return;
            }
            if (row == null) {
                row = new UserThemeToken();
                row.setUsername(username);
                row.setMode(mode);
                row.setTokenKey(key);
            }
            row.setTokenValue(value.trim());
            tokens.save(row);
        });
    }

    /** Build the resolved config from a preference row + all its overrides. */
    private ThemeConfig build(UserPreference pref, List<UserThemeToken> overrides) {
        ThemePreset preset = ThemePreset.of(pref.getPreset());

        // group overrides by mode -> key -> value
        Map<String, Map<String, String>> ov = new LinkedHashMap<>();
        ov.put("light", new LinkedHashMap<>());
        ov.put("dark", new LinkedHashMap<>());
        for (UserThemeToken t : overrides) {
            ov.computeIfAbsent(ThemeMode.of(t.getMode()).key(), k -> new LinkedHashMap<>())
              .put(t.getTokenKey(), t.getTokenValue());
        }

        Map<String, String> resolvedLight = resolve(ThemeMode.LIGHT, preset, ov.get("light"));
        Map<String, String> resolvedDark  = resolve(ThemeMode.DARK, preset, ov.get("dark"));

        Map<String, Map<String, String>> resolved = new LinkedHashMap<>();
        resolved.put("light", resolvedLight);
        resolved.put("dark", resolvedDark);

        return new ThemeConfig(
                pref.getUsername(),
                NavLayout.of(pref.getNavLayout()).key(),
                ThemeMode.of(pref.getThemeMode()).key(),
                preset.key(),
                pref.getFontScale(),
                pref.getDensity(),
                pref.isSidebarCollapsed(),
                ThemeConfig.presetList(),
                ThemeConfig.groups(resolvedLight, resolvedDark, ov.get("light"), ov.get("dark")),
                resolved);
    }

    /** mode default -> preset delta -> user override, for every token. */
    private Map<String, String> resolve(ThemeMode mode, ThemePreset preset, Map<String, String> userOverrides) {
        Map<String, String> out = new LinkedHashMap<>();
        for (ThemeToken token : ThemeToken.values()) {
            String value = token.defaultFor(mode.key());
            String presetVal = preset.delta().get(token);
            if (presetVal != null) value = presetVal;
            String userVal = userOverrides == null ? null : userOverrides.get(token.key());
            if (userVal != null && !userVal.isBlank()) value = userVal;
            out.put(token.key(), value);
        }
        return out;
    }

    private UserPreference defaultPref(String username) {
        UserPreference p = new UserPreference();
        p.setUsername(username);
        return p;   // entity field defaults = vertical / light / default / 1.0 / comfortable
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
    public Map<String, String> resolvedTokens(String username, String mode) {
        return get(username).resolved().getOrDefault(ThemeMode.of(mode).key(), new LinkedHashMap<>());
    }

    /** Every group's tokens flat (used by callers that don't need grouping). */
    public List<String> allTokenKeys() {
        List<String> keys = new ArrayList<>();
        for (ThemeToken t : ThemeToken.values()) keys.add(t.key());
        return keys;
    }
}
