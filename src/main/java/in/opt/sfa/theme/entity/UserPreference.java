package in.opt.sfa.theme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * One row per user (COMMON db). Holds the "top-level" appearance choices that
 * are not individual colour tokens: which nav layout, light/dark mode, which
 * preset, font scale and density. The fine-grained colour overrides live in
 * {@link UserThemeToken}.
 *
 * Keyed by username (app_user.username is globally unique), so a user's look
 * follows them regardless of tenant.
 */
@Entity
@Table(name = "user_preference")
@Getter
@Setter
public class UserPreference {

    @Id
    @Column(name = "username", length = 128)
    private String username;

    /** "vertical" | "horizontal" */
    @Column(name = "nav_layout", length = 16, nullable = false)
    private String navLayout = "vertical";

    /** "light" | "dark" */
    @Column(name = "theme_mode", length = 16, nullable = false)
    private String themeMode = "light";

    /** ThemePreset key, e.g. "default" | "teal" | "indigo" | "slate". */
    @Column(name = "preset", length = 32, nullable = false)
    private String preset = "default";

    /** Base font scale multiplier as a string, e.g. "1.0" (0.875 – 1.25). */
    @Column(name = "font_scale", length = 8, nullable = false)
    private String fontScale = "1.0";

    /** "comfortable" | "compact" */
    @Column(name = "density", length = 16, nullable = false)
    private String density = "comfortable";

    /** Sidebar collapsed to icon-rail (vertical layout only). */
    @Column(name = "sidebar_collapsed", nullable = false)
    private boolean sidebarCollapsed = false;
}
