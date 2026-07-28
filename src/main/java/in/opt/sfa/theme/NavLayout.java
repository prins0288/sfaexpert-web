package in.opt.sfa.theme;

/**
 * How the primary navigation is rendered:
 *   VERTICAL   -> fixed left sidebar (the classic look)
 *   HORIZONTAL -> top menu bar with hover/click dropdowns
 * Stored per user; the shell reads it and renders the matching chrome.
 */
public enum NavLayout {
    VERTICAL, HORIZONTAL;

    public static NavLayout of(String s) {
        return HORIZONTAL.name().equalsIgnoreCase(s) ? HORIZONTAL : VERTICAL;
    }

    public String key() { return name().toLowerCase(); }
}
