package in.opt.sfa.common.util;

import java.math.BigDecimal;

/** Small shared numeric parsing helpers used across feature modules (bulk upload row parsing, etc). */
public final class Numbers {
    private Numbers() {}

    public static BigDecimal toBigDecimal(String s) {
        try { return Strings.isBlank(s) ? null : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
