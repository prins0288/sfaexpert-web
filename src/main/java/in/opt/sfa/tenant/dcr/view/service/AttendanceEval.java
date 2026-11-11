package in.opt.sfa.tenant.dcr.view.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure rule evaluation of ONE submitted DCR day for the Attendance Report: present, half day or absent - from its status, work type, calls and times.
 * The conditions (attendance_rule) are all optional: 0 / empty = not checked. When several conditions fail the worst result wins (ABSENT over HALF).
 */
public final class AttendanceEval {

    private AttendanceEval() { }

    public enum Result { PRESENT, HALF, ABSENT }

    public record Rule(Set<String> applyTypes, int minCalls, String callsResult, boolean countUnlisted, BigDecimal minHours, String hoursResult,
                       LocalTime lateStartAfter, String lateResult, LocalTime earlyEndBefore, String earlyResult, boolean needApproved) {
        /** No condition at all: a submitted DCR is a present day. */
        /** The same rule with the approval requirement set by the approval system (company switch + work type) instead of the rule's own flag. */
        public Rule withApproval(boolean need) { return new Rule(applyTypes, minCalls, callsResult, countUnlisted, minHours, hoursResult, lateStartAfter, lateResult, earlyEndBefore, earlyResult, need); }
        public static Rule none() { return new Rule(Set.of("WORKING"), 0, "ABSENT", true, BigDecimal.ZERO, "HALF", null, "HALF", null, "HALF", false); }
    }

    /** The facts of the day: DCR status (0 draft, 1 submitted, 2 approved, 3 rejected), work type, calls, and the day's times (null when not recorded). */
    public record Day(int status, String workType, int listedCalls, int unlistedCalls, Integer workMin, LocalTime dayStart, LocalTime dayEnd) { }

    public record Outcome(Result result, List<String> reasons) { }

    public static Outcome evaluate(Day d, Rule r) {
        List<String> why = new ArrayList<>();
        if (d.status() == 0) return new Outcome(Result.ABSENT, List.of("DCR is not submitted (draft)"));
        if (d.status() == 3) return new Outcome(Result.ABSENT, List.of("DCR was rejected"));
        if (r.needApproved() && d.status() != 2) return new Outcome(Result.ABSENT, List.of("DCR is not approved yet"));
        if (r.applyTypes() == null || !r.applyTypes().contains(d.workType())) return new Outcome(Result.PRESENT, List.of());   // office / training / camp ...: a submitted DCR is enough
        Result worst = Result.PRESENT;
        int calls = d.listedCalls() + (r.countUnlisted() ? d.unlistedCalls() : 0);
        if (r.minCalls() > 0 && calls < r.minCalls()) { worst = worse(worst, of(r.callsResult())); why.add(calls + " call(s), minimum " + r.minCalls()); }
        if (r.minHours() != null && r.minHours().signum() > 0 && d.workMin() != null && d.workMin() < r.minHours().multiply(BigDecimal.valueOf(60)).intValue()) {
            worst = worse(worst, of(r.hoursResult())); why.add("worked " + fmt(d.workMin()) + ", minimum " + r.minHours().stripTrailingZeros().toPlainString() + " h");
        }
        if (r.lateStartAfter() != null && d.dayStart() != null && d.dayStart().isAfter(r.lateStartAfter())) { worst = worse(worst, of(r.lateResult())); why.add("day started " + d.dayStart() + ", after " + r.lateStartAfter()); }
        if (r.earlyEndBefore() != null && d.dayEnd() != null && d.dayEnd().isBefore(r.earlyEndBefore())) { worst = worse(worst, of(r.earlyResult())); why.add("day ended " + d.dayEnd() + ", before " + r.earlyEndBefore()); }
        return new Outcome(worst, why);
    }

    private static Result of(String s) { return "ABSENT".equalsIgnoreCase(s) ? Result.ABSENT : Result.HALF; }
    private static Result worse(Result a, Result b) { return a.ordinal() >= b.ordinal() ? a : b; }
    private static String fmt(int m) { return (m / 60) + "h " + (m % 60) + "m"; }
}
