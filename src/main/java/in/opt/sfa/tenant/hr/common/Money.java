package in.opt.sfa.tenant.hr.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money is DECIMAL(12,2) - never float/double. Rounding mode is a setting (NEAREST / FLOOR / CEIL). */
public final class Money {

    private Money() { }

    public static BigDecimal round(BigDecimal v, String mode, int scale) {
        if (v == null) return BigDecimal.ZERO.setScale(scale);
        RoundingMode rm = switch (mode == null ? "NEAREST" : mode.toUpperCase()) {
            case "FLOOR" -> RoundingMode.FLOOR;
            case "CEIL" -> RoundingMode.CEILING;
            case "NEAREST" -> RoundingMode.HALF_UP;
            default -> throw new IllegalArgumentException("Unknown rounding mode " + mode);
        };
        return v.setScale(scale, rm);
    }

    public static long toPaise(BigDecimal rupees) { return rupees.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact(); }

    public static BigDecimal fromPaise(long paise) { return BigDecimal.valueOf(paise, 2); }
}
