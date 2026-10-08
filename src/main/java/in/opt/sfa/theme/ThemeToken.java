package in.opt.sfa.theme;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * THE single source of truth for every themeable value in the application.
 *
 * Nothing in the CSS is allowed to hard-code a colour or a layout dimension —
 * every one of them is a token declared here, rendered into a CSS custom
 * property (`--sfa-*`) at runtime. Adding a new themeable value means adding one
 * enum constant; the REST API, the customizer UI and the CSS variable all pick
 * it up automatically with no other change.
 *
 * Each token carries:
 *   key          stable id used in the DB (user_theme_token.token_key) and JSON
 *   cssVar       the CSS custom property the browser actually reads
 *   label        human text shown in the appearance customizer
 *   group        which section of the customizer it appears under
 *   type         COLOR -> colour picker, SIZE -> text/number input
 *   lightDefault value used in light mode when the user has no override
 *   darkDefault  value used in dark mode when the user has no override
 *
 * The LIGHT defaults are deliberately the exact colours the app already used
 * before theming existed, so the default look is unchanged.
 */
public enum ThemeToken {

    // ---- Brand -------------------------------------------------------------
    PRIMARY          ("primary",           "Primary",            Group.BRAND,   Type.COLOR, "#1b3a6b", "#4c8dff"),
    PRIMARY_HOVER    ("primary-hover",     "Primary (hover)",    Group.BRAND,   Type.COLOR, "#2a56a0", "#3d7bef"),
    PRIMARY_SOFT     ("primary-soft",      "Primary (soft bg)",  Group.BRAND,   Type.COLOR, "#eef2f8", "#1b2740"),
    PRIMARY_CONTRAST ("primary-contrast",  "Primary text-on",    Group.BRAND,   Type.COLOR, "#ffffff", "#0b1220"),
    ACCENT           ("accent",            "Accent",             Group.BRAND,   Type.COLOR, "#0aa3a3", "#2ad4c6"),
    ACCENT_CONTRAST  ("accent-contrast",   "Accent text-on",     Group.BRAND,   Type.COLOR, "#ffffff", "#04201f"),

    // ---- Canvas & surfaces -------------------------------------------------
    BODY_BG          ("body-bg",           "Page background",    Group.SURFACE, Type.COLOR, "#f2f4f8", "#0f1621"),
    SURFACE          ("surface",           "Surface",            Group.SURFACE, Type.COLOR, "#ffffff", "#18202e"),
    SURFACE_2        ("surface-2",         "Surface (sunken)",   Group.SURFACE, Type.COLOR, "#eef2f8", "#131b27"),
    BORDER           ("border",            "Border",             Group.SURFACE, Type.COLOR, "#e3e7ee", "#26303f"),
    SHADOW           ("shadow",            "Shadow",             Group.SURFACE, Type.COLOR, "rgba(20,40,80,.06)", "rgba(0,0,0,.45)"),

    // ---- Text --------------------------------------------------------------
    TEXT             ("text",              "Body text",          Group.TEXT,    Type.COLOR, "#24303f", "#dbe3ef"),
    TEXT_MUTED       ("text-muted",        "Muted text",         Group.TEXT,    Type.COLOR, "#6b7a90", "#8b9ab1"),
    TEXT_HEADING     ("text-heading",      "Headings",           Group.TEXT,    Type.COLOR, "#1b3a6b", "#eaf0f9"),
    TEXT_LABEL       ("text-label",        "Form labels",        Group.TEXT,    Type.COLOR, "#3a4a60", "#aab7c9"),

    // ---- Vertical navigation (sidebar) -------------------------------------
    SIDEBAR_BG       ("sidebar-bg",        "Sidebar background", Group.SIDEBAR, Type.COLOR, "#12294d", "#131b27"),
    SIDEBAR_FG       ("sidebar-fg",        "Sidebar text",       Group.SIDEBAR, Type.COLOR, "#c9d6ea", "#aab7c9"),
    SIDEBAR_GROUP_FG ("sidebar-group-fg",  "Sidebar section",    Group.SIDEBAR, Type.COLOR, "#7f93b5", "#6b7a90"),
    SIDEBAR_HOVER_BG ("sidebar-hover-bg",  "Sidebar hover",      Group.SIDEBAR, Type.COLOR, "rgba(255,255,255,.06)", "rgba(255,255,255,.05)"),
    SIDEBAR_ACTIVE_BG("sidebar-active-bg", "Sidebar active bg",  Group.SIDEBAR, Type.COLOR, "rgba(10,163,163,.16)", "rgba(76,141,255,.16)"),
    SIDEBAR_ACTIVE_FG("sidebar-active-fg", "Sidebar active text",Group.SIDEBAR, Type.COLOR, "#ffffff", "#ffffff"),
    SIDEBAR_MARKER   ("sidebar-marker",    "Sidebar active bar", Group.SIDEBAR, Type.COLOR, "#0aa3a3", "#4c8dff"),
    SIDEBAR_BRAND_BG ("sidebar-brand-bg",  "Sidebar brand strip",Group.SIDEBAR, Type.COLOR, "rgba(0,0,0,.18)", "rgba(0,0,0,.28)"),
    SIDEBAR_WIDTH    ("sidebar-width",     "Sidebar width",      Group.SIDEBAR, Type.SIZE,  "250px",   "250px"),

    // ---- Horizontal navigation (top menu bar) ------------------------------
    NAVBAR_BG        ("navbar-bg",         "Menu bar background",Group.NAVBAR,  Type.COLOR, "#12294d", "#131b27"),
    NAVBAR_FG        ("navbar-fg",         "Menu bar text",      Group.NAVBAR,  Type.COLOR, "#c9d6ea", "#aab7c9"),
    NAVBAR_HOVER_BG  ("navbar-hover-bg",   "Menu bar hover",     Group.NAVBAR,  Type.COLOR, "rgba(255,255,255,.08)", "rgba(255,255,255,.06)"),
    NAVBAR_ACTIVE_BG ("navbar-active-bg",  "Menu bar active bg", Group.NAVBAR,  Type.COLOR, "rgba(10,163,163,.20)", "rgba(76,141,255,.20)"),
    NAVBAR_ACTIVE_FG ("navbar-active-fg",  "Menu bar active txt",Group.NAVBAR,  Type.COLOR, "#ffffff", "#ffffff"),
    NAVBAR_DROP_BG   ("navbar-drop-bg",    "Dropdown background",Group.NAVBAR,  Type.COLOR, "#ffffff", "#18202e"),
    NAVBAR_DROP_FG   ("navbar-drop-fg",    "Dropdown text",      Group.NAVBAR,  Type.COLOR, "#24303f", "#dbe3ef"),
    NAVBAR_HEIGHT    ("navbar-height",     "Menu bar height",    Group.NAVBAR,  Type.SIZE,  "46px",    "46px"),

    // ---- Header / topbar ---------------------------------------------------
    HEADER_BG        ("header-bg",         "Header background",  Group.HEADER,  Type.COLOR, "#ffffff", "#18202e"),
    HEADER_FG        ("header-fg",         "Header text",        Group.HEADER,  Type.COLOR, "#33445c", "#dbe3ef"),
    HEADER_BORDER    ("header-border",     "Header border",      Group.HEADER,  Type.COLOR, "#e3e7ee", "#26303f"),
    HEADER_TITLE_FG  ("header-title-fg",   "Header title",       Group.HEADER,  Type.COLOR, "#1b3a6b", "#eaf0f9"),
    HEADER_HEIGHT    ("header-height",     "Header height",      Group.HEADER,  Type.SIZE,  "56px",    "56px"),

    // ---- Footer ------------------------------------------------------------
    FOOTER_BG        ("footer-bg",         "Footer background",  Group.FOOTER,  Type.COLOR, "#ffffff", "#18202e"),
    FOOTER_FG        ("footer-fg",         "Footer text",        Group.FOOTER,  Type.COLOR, "#6b7a90", "#8b9ab1"),
    FOOTER_BORDER    ("footer-border",     "Footer border",      Group.FOOTER,  Type.COLOR, "#e3e7ee", "#26303f"),

    // ---- Cards -------------------------------------------------------------
    CARD_BG          ("card-bg",           "Card background",    Group.CARD,    Type.COLOR, "#ffffff", "#18202e"),
    CARD_HEADER_BG   ("card-header-bg",    "Card header bg",     Group.CARD,    Type.COLOR, "#ffffff", "#18202e"),
    CARD_HEADER_FG   ("card-header-fg",    "Card header text",   Group.CARD,    Type.COLOR, "#1b3a6b", "#eaf0f9"),
    CARD_BORDER      ("card-border",       "Card border",        Group.CARD,    Type.COLOR, "#e3e7ee", "#26303f"),
    CARD_RADIUS      ("card-radius",       "Card corner radius", Group.CARD,    Type.SIZE,  "12px",    "12px"),
    MODAL_HEADER_BG  ("modal-header-bg",   "Modal header bg",    Group.CARD,    Type.COLOR, "#1b3a6b", "#1b2740"),
    MODAL_HEADER_FG  ("modal-header-fg",   "Modal header text",  Group.CARD,    Type.COLOR, "#ffffff", "#eaf0f9"),

    // ---- Tables ------------------------------------------------------------
    TABLE_HEAD_BG    ("table-head-bg",     "Table header bg",    Group.TABLE,   Type.COLOR, "#eef2f8", "#131b27"),
    TABLE_HEAD_FG    ("table-head-fg",     "Table header text",  Group.TABLE,   Type.COLOR, "#1b3a6b", "#dbe3ef"),
    TABLE_STRIPE_BG  ("table-stripe-bg",   "Table stripe",       Group.TABLE,   Type.COLOR, "#f7f9fc", "#1c2534"),
    TABLE_HOVER_BG   ("table-hover-bg",    "Table row hover",    Group.TABLE,   Type.COLOR, "#eef4ff", "#212c3e"),
    TABLE_BORDER     ("table-border",      "Table border",       Group.TABLE,   Type.COLOR, "#e3e7ee", "#26303f"),
    TABLE_ROW_HIGHLIGHT("table-row-highlight","Row highlight (on click)",Group.TABLE,Type.COLOR,"#eef2f8","#1b2740"),

    // ---- Forms -------------------------------------------------------------
    INPUT_BG         ("input-bg",          "Input background",   Group.FORM,    Type.COLOR, "#ffffff", "#131b27"),
    INPUT_FG         ("input-fg",          "Input text",         Group.FORM,    Type.COLOR, "#24303f", "#dbe3ef"),
    INPUT_BORDER     ("input-border",      "Input border",       Group.FORM,    Type.COLOR, "#d6dce7", "#2e3a4d"),
    INPUT_FOCUS      ("input-focus",       "Input focus ring",   Group.FORM,    Type.COLOR, "#2a56a0", "#4c8dff"),
    REQUIRED_STAR    ("required-star",     "Required marker",    Group.FORM,    Type.COLOR, "#c0392b", "#ff7a6b"),

    // ---- Status ------------------------------------------------------------
    SUCCESS          ("success",           "Success",            Group.STATUS,  Type.COLOR, "#1a9d63", "#2fbf7e"),
    WARNING          ("warning",           "Warning",            Group.STATUS,  Type.COLOR, "#d98324", "#f0a442"),
    DANGER           ("danger",            "Danger",             Group.STATUS,  Type.COLOR, "#b0384a", "#e5596d"),
    INFO             ("info",              "Info",               Group.STATUS,  Type.COLOR, "#2a56a0", "#4c8dff"),

    // ---- Typography & density ---------------------------------------------
    FONT_FAMILY      ("font-family",       "Font family",        Group.TYPE,    Type.SIZE,  "\"Segoe UI\", Roboto, Arial, sans-serif",
                                                                                            "\"Segoe UI\", Roboto, Arial, sans-serif"),
    FONT_SIZE        ("font-size",         "Base font size",     Group.TYPE,    Type.SIZE,  "14px",    "14px"),
    RADIUS           ("radius",            "Control radius",     Group.TYPE,    Type.SIZE,  "8px",     "8px");

    /** Customizer sections, in display order. */
    public enum Group {
        BRAND("Brand"), SURFACE("Surface & background"), TEXT("Text"),
        SIDEBAR("Vertical nav (sidebar)"), NAVBAR("Horizontal nav (menu bar)"),
        HEADER("Header"), FOOTER("Footer"), CARD("Cards & modals"),
        TABLE("Tables"), FORM("Forms"), STATUS("Status colours"), TYPE("Typography & shape");

        private final String label;
        Group(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Drives which control the customizer renders for the token. */
    public enum Type { COLOR, SIZE }

    private final String key;
    private final String label;
    private final Group group;
    private final Type type;
    private final String lightDefault;
    private final String darkDefault;

    ThemeToken(String key, String label, Group group, Type type,
               String lightDefault, String darkDefault) {
        this.key = key;
        this.label = label;
        this.group = group;
        this.type = type;
        this.lightDefault = lightDefault;
        this.darkDefault = darkDefault;
    }

    public String key()          { return key; }
    public String label()        { return label; }
    public Group group()         { return group; }
    public Type type()           { return type; }
    public String lightDefault() { return lightDefault; }
    public String darkDefault()  { return darkDefault; }

    /** The CSS custom property the stylesheet reads, e.g. "--sfa-sidebar-bg". */
    public String cssVar() { return "--sfa-" + key; }

    /** Default for the given mode ("dark" -> dark default, anything else -> light). */
    public String defaultFor(String mode) {
        return ThemeMode.DARK.matches(mode) ? darkDefault : lightDefault;
    }

    public static Optional<ThemeToken> byKey(String key) {
        return Arrays.stream(values()).filter(t -> t.key.equals(key)).findFirst();
    }

    /** Every token's default for a mode, keyed by token key. */
    public static Map<String, String> defaults(String mode) {
        Map<String, String> map = new LinkedHashMap<>();
        for (ThemeToken t : values()) {
            map.put(t.key, t.defaultFor(mode));
        }
        return map;
    }
}
