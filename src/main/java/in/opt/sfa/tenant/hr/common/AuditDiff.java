package in.opt.sfa.tenant.hr.common;

import java.util.*;

/** Pure: which fields changed between two maps (a missing key counts as null). Used to write one audit row per changed field. */
public final class AuditDiff {

    public record Change(String field, String oldValue, String newValue) { }

    private AuditDiff() { }

    public static List<Change> diff(Map<String, ?> before, Map<String, ?> after) {
        Map<String, ?> b = before == null ? Map.of() : before, a = after == null ? Map.of() : after;
        Set<String> keys = new TreeSet<>(b.keySet());
        keys.addAll(a.keySet());
        List<Change> out = new ArrayList<>();
        for (String k : keys) {
            String o = text(b.get(k)), n = text(a.get(k));
            if (!Objects.equals(o, n)) out.add(new Change(k, o, n));
        }
        return out;
    }

    private static String text(Object o) { return o == null ? null : o.toString(); }
}
