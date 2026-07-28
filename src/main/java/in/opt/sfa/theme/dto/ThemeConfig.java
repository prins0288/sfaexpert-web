package in.opt.sfa.theme.dto;

import in.opt.sfa.theme.ThemePreset;
import in.opt.sfa.theme.ThemeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The full appearance payload sent to the browser. Carries the user's choices,
 * the fully-resolved token maps for BOTH modes (so the client can flip
 * light/dark with no round-trip), and the grouped metadata that drives the
 * customizer UI.
 */
public record ThemeConfig(
        String username,
        String navLayout,
        String mode,
        String preset,
        String fontScale,
        String density,
        boolean sidebarCollapsed,
        List<PresetInfo> presets,
        List<Group> groups,
        Map<String, Map<String, String>> resolved
) {

    /** A selectable preset in the customizer. */
    public record PresetInfo(String key, String label) {}

    /** A customizer section with its tokens. */
    public record Group(String key, String label, List<TokenInfo> tokens) {}

    /**
     * One themeable value as the customizer needs it: metadata + the current
     * resolved values and defaults for each mode, plus whether the user has an
     * explicit override (so the UI can show a "reset" affordance).
     */
    public record TokenInfo(
            String key,
            String label,
            String type,          // "color" | "size"
            String light,         // resolved light value
            String dark,          // resolved dark value
            String lightDefault,  // mode default (pre-override) for light
            String darkDefault,   // mode default (pre-override) for dark
            boolean lightOverridden,
            boolean darkOverridden
    ) {}

    public static List<PresetInfo> presetList() {
        List<PresetInfo> list = new ArrayList<>();
        for (ThemePreset p : ThemePreset.values()) list.add(new PresetInfo(p.key(), p.label()));
        return list;
    }

    /** Build the grouped token metadata from the two resolved maps + override maps. */
    public static List<Group> groups(Map<String, String> resolvedLight,
                                     Map<String, String> resolvedDark,
                                     Map<String, String> overridesLight,
                                     Map<String, String> overridesDark) {
        // preserve enum group order
        List<Group> groups = new ArrayList<>();
        for (ThemeToken.Group g : ThemeToken.Group.values()) {
            List<TokenInfo> items = new ArrayList<>();
            for (ThemeToken t : ThemeToken.values()) {
                if (t.group() != g) continue;
                items.add(new TokenInfo(
                        t.key(),
                        t.label(),
                        t.type() == ThemeToken.Type.COLOR ? "color" : "size",
                        resolvedLight.get(t.key()),
                        resolvedDark.get(t.key()),
                        t.lightDefault(),
                        t.darkDefault(),
                        overridesLight != null && overridesLight.containsKey(t.key()),
                        overridesDark != null && overridesDark.containsKey(t.key())
                ));
            }
            if (!items.isEmpty()) groups.add(new Group(g.name(), g.label(), items));
        }
        return groups;
    }
}
