package in.opt.sfa.tenant.hr.common;

import java.math.BigDecimal;
import java.util.Arrays;

/** Validates / normalises a setting value against its definition (type INT, DECIMAL, BOOL, STRING, ENUM, JSON, optional min / max / allowed values). */
public final class SettingTypes {

    private SettingTypes() { }

    public static String normalise(String type, String raw, String min, String max, String enumValues) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Value is required");
        switch (type) {
            case "INT" -> {
                long n;
                try { n = Long.parseLong(v); } catch (NumberFormatException e) { throw new IllegalArgumentException("Whole number expected: " + v); }
                range(BigDecimal.valueOf(n), min, max);
                return Long.toString(n);
            }
            case "DECIMAL" -> {
                BigDecimal d;
                try { d = new BigDecimal(v); } catch (NumberFormatException e) { throw new IllegalArgumentException("Number expected: " + v); }
                range(d, min, max);
                return d.stripTrailingZeros().toPlainString();
            }
            case "BOOL" -> {
                if (v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes")) return "true";
                if (v.equalsIgnoreCase("false") || v.equals("0") || v.equalsIgnoreCase("no")) return "false";
                throw new IllegalArgumentException("Yes / No expected: " + v);
            }
            case "ENUM" -> {
                if (enumValues == null || Arrays.stream(enumValues.split(",")).map(String::trim).noneMatch(v::equals))
                    throw new IllegalArgumentException("Must be one of " + enumValues + ": " + v);
                return v;
            }
            case "JSON" -> {
                if (!(v.startsWith("{") || v.startsWith("["))) throw new IllegalArgumentException("JSON expected");
                return v;
            }
            case "STRING" -> { return v; }
            default -> throw new IllegalArgumentException("Unknown setting type " + type);
        }
    }

    private static void range(BigDecimal n, String min, String max) {
        if (min != null && !min.isBlank() && n.compareTo(new BigDecimal(min)) < 0) throw new IllegalArgumentException("Must be at least " + min);
        if (max != null && !max.isBlank() && n.compareTo(new BigDecimal(max)) > 0) throw new IllegalArgumentException("Must be at most " + max);
    }
}
