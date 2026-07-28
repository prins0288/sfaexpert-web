package in.opt.sfa.theme;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static in.opt.sfa.theme.ThemeToken.*;

/**
 * Ready-made colour presets. Picking one is a one-click starting point: it
 * overrides a handful of brand / nav tokens on top of the current mode's
 * defaults. The user can then fine-tune any individual token in the customizer.
 *
 * DEFAULT is the app's original navy/teal StarSFA look — choosing it means
 * "no preset delta", so the base mode defaults show through unchanged.
 *
 * A preset delta value is applied to BOTH light and dark modes (the mode still
 * supplies every other token, e.g. surfaces and text), which keeps presets
 * simple while remaining fully overridable per token afterwards.
 */
public enum ThemePreset {

    DEFAULT("Default Navy", Map.of()),

    TEAL("Teal", deltas(
            PRIMARY, "#0f766e",
            PRIMARY_HOVER, "#0d9488",
            ACCENT, "#f59e0b",
            SIDEBAR_BG, "#0b3b38",
            SIDEBAR_MARKER, "#2dd4bf",
            NAVBAR_BG, "#0b3b38",
            MODAL_HEADER_BG, "#0f766e")),

    INDIGO("Indigo", deltas(
            PRIMARY, "#4338ca",
            PRIMARY_HOVER, "#4f46e5",
            ACCENT, "#ec4899",
            SIDEBAR_BG, "#1e1b4b",
            SIDEBAR_MARKER, "#818cf8",
            NAVBAR_BG, "#1e1b4b",
            MODAL_HEADER_BG, "#4338ca")),

    SLATE("Slate", deltas(
            PRIMARY, "#334155",
            PRIMARY_HOVER, "#475569",
            ACCENT, "#0ea5e9",
            SIDEBAR_BG, "#1e293b",
            SIDEBAR_MARKER, "#38bdf8",
            NAVBAR_BG, "#1e293b",
            MODAL_HEADER_BG, "#334155")),

    EMERALD("Emerald", deltas(
            PRIMARY, "#047857",
            PRIMARY_HOVER, "#059669",
            ACCENT, "#f59e0b",
            SIDEBAR_BG, "#064e3b",
            SIDEBAR_MARKER, "#34d399",
            NAVBAR_BG, "#064e3b",
            MODAL_HEADER_BG, "#047857"));

    private final String label;
    private final Map<ThemeToken, String> delta;

    ThemePreset(String label, Map<ThemeToken, String> delta) {
        this.label = label;
        this.delta = delta;
    }

    public String key()   { return name().toLowerCase(); }
    public String label() { return label; }

    /** Token overrides this preset contributes (may be empty for DEFAULT). */
    public Map<ThemeToken, String> delta() { return delta; }

    /** Same delta as a key->value string map for JSON. */
    public Map<String, String> deltaAsKeys() {
        Map<String, String> m = new LinkedHashMap<>();
        delta.forEach((t, v) -> m.put(t.key(), v));
        return m;
    }

    public static ThemePreset of(String s) {
        return byKey(s).orElse(DEFAULT);
    }

    public static Optional<ThemePreset> byKey(String key) {
        if (key == null) return Optional.empty();
        return Arrays.stream(values()).filter(p -> p.key().equalsIgnoreCase(key)).findFirst();
    }

    /** Varargs helper: token, value, token, value, ... -> ordered map. */
    private static Map<ThemeToken, String> deltas(Object... kv) {
        Map<ThemeToken, String> m = new EnumMap<>(ThemeToken.class);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((ThemeToken) kv[i], (String) kv[i + 1]);
        }
        return m;
    }
}
