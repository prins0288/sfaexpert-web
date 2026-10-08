package in.opt.sfa.common.util;

/** Small shared string helpers used across feature modules (bulk upload row validation, etc). */
public final class Strings {
    private Strings() {}

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
