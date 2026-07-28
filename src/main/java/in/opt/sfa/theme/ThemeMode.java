package in.opt.sfa.theme;

/** Light or dark base. Every theme token has a default per mode. */
public enum ThemeMode {
    LIGHT, DARK;

    /** Lenient parse: anything that isn't clearly "dark" is LIGHT. */
    public static ThemeMode of(String s) {
        return DARK.name().equalsIgnoreCase(s) ? DARK : LIGHT;
    }

    public boolean matches(String s) {
        return this == of(s);
    }

    public String key() { return name().toLowerCase(); }
}
