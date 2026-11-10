package in.opt.sfa.tenant.hr.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure leave policy logic: day counting (with the sandwich rule), accrual, and the policy checks. No database, no clock - everything is passed in. */
public final class LeaveRules {

    private LeaveRules() { }

    public record Policy(String code, boolean paid, BigDecimal quota, String accrual, BigDecimal carryMax, boolean halfDay, int maxConsecutive, BigDecimal maxPerMonth,
                         int minNoticeDays, int backdateDays, int futureDays, boolean reasonRequired, boolean sandwich, boolean allowNegative, int minServiceMonths) { }

    public record Request(LocalDate from, LocalDate to, String part, String reason, LocalDate today, LocalDate joined, BigDecimal balance, BigDecimal usedThisMonth,
                          Set<LocalDate> nonWorking, boolean overlapsOther) { }

    public record Result(List<String> errors, BigDecimal days, BigDecimal lwpDays) {
        public boolean ok() { return errors.isEmpty(); }
    }

    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v); }

    /**
     * Days of leave for a range. Working days count 1 (0.5 for a half day). Weekly offs / holidays count only when the sandwich rule is on and they
     * fall BETWEEN two leave working days - a non-working day at the start or end of the range is never leave.
     */
    public static BigDecimal countDays(LocalDate from, LocalDate to, String part, Set<LocalDate> nonWorking, boolean sandwich) {
        if (from == null || to == null || to.isBefore(from)) return BigDecimal.ZERO;
        LocalDate first = null, last = null;
        int working = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!nonWorking.contains(d)) { working++; if (first == null) first = d; last = d; }
        }
        if (working == 0) return BigDecimal.ZERO;
        int total = working;
        if (sandwich) {
            for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) if (nonWorking.contains(d)) total++;
        }
        BigDecimal days = BigDecimal.valueOf(total);
        if (!"FULL".equals(part) && from.equals(to)) days = bd(0.5);
        return days;
    }

    /** First day of the leave year that contains {@code date}. */
    public static LocalDate yearStart(LocalDate date, int fyStartMonth, String mode) {
        int m = "CALENDAR".equals(mode) ? 1 : fyStartMonth;
        LocalDate s = LocalDate.of(date.getYear(), m, 1);
        return s.isAfter(date) ? s.minusYears(1) : s;
    }

    /** Days earned by {@code asOf} in the leave year starting {@code yearStart}; joining part-way through the year earns pro-rata. Rounded DOWN to 0.5. */
    public static BigDecimal accrued(BigDecimal quota, String accrual, LocalDate yearStart, LocalDate asOf, LocalDate joined) {
        if (quota.signum() == 0) return BigDecimal.ZERO;
        LocalDate startMonth = joined != null && joined.isAfter(yearStart) ? joined.withDayOfMonth(1) : yearStart;
        if (asOf.isBefore(startMonth)) return BigDecimal.ZERO;
        int monthsInYearFromStart = (int) ChronoUnit.MONTHS.between(startMonth, yearStart.plusYears(1));      // months the person is employed this year
        int monthsElapsed = (int) ChronoUnit.MONTHS.between(startMonth, asOf.withDayOfMonth(1)) + 1;          // including the current month
        BigDecimal per = quota.divide(bd(12), 6, RoundingMode.HALF_UP);
        BigDecimal v = "MONTHLY".equals(accrual) ? per.multiply(bd(Math.min(monthsElapsed, monthsInYearFromStart))) : per.multiply(bd(monthsInYearFromStart));
        return floorHalf(v);
    }

    public static BigDecimal floorHalf(BigDecimal v) { return v.multiply(bd(2)).setScale(0, RoundingMode.FLOOR).divide(bd(2), 2, RoundingMode.UNNECESSARY); }

    /** Unused days of the year that ended, capped by the carry-forward limit. */
    public static BigDecimal carryForward(BigDecimal unused, BigDecimal carryMax) {
        if (unused == null || unused.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return unused.min(carryMax).setScale(2, RoundingMode.DOWN);
    }

    public static Result check(Policy p, Request r) {
        List<String> e = new ArrayList<>();
        if (r.from() == null || r.to() == null) { e.add("Choose the leave dates"); return new Result(e, BigDecimal.ZERO, BigDecimal.ZERO); }
        if (r.to().isBefore(r.from())) e.add("'To' date is before the 'From' date");
        if (!List.of("FULL", "FIRST", "SECOND").contains(r.part())) e.add("Day part must be full day, first half or second half");
        if (!"FULL".equals(r.part())) {
            if (!p.halfDay()) e.add(p.code() + " cannot be taken as a half day");
            if (!r.from().equals(r.to())) e.add("A half day can be only for one date");
        }
        if (p.reasonRequired() && (r.reason() == null || r.reason().isBlank())) e.add("Please give a reason");
        long calendar = ChronoUnit.DAYS.between(r.from(), r.to()) + 1;
        if (p.maxConsecutive() > 0 && calendar > p.maxConsecutive()) e.add(p.code() + " can be taken for at most " + p.maxConsecutive() + " days at a time");
        if (r.from().isBefore(r.today().minusDays(p.backdateDays()))) e.add(p.backdateDays() == 0 ? "Leave cannot be applied for a past date" : "Leave can be applied only up to " + p.backdateDays() + " day(s) back");
        if (p.minNoticeDays() > 0 && r.from().isBefore(r.today().plusDays(p.minNoticeDays()))) e.add(p.code() + " needs " + p.minNoticeDays() + " day(s) notice");
        if (p.futureDays() > 0 && r.from().isAfter(r.today().plusDays(p.futureDays()))) e.add("Leave can be applied only " + p.futureDays() + " day(s) ahead");
        if (p.minServiceMonths() > 0 && (r.joined() == null || r.joined().plusMonths(p.minServiceMonths()).isAfter(r.from()))) e.add(p.code() + " is available after " + p.minServiceMonths() + " month(s) of service");
        if (r.overlapsOther()) e.add("You already have leave or a worked day in these dates");
        BigDecimal days = e.isEmpty() || r.to().isAfter(r.from().minusDays(1)) ? countDays(r.from(), r.to(), r.part(), r.nonWorking(), p.sandwich()) : BigDecimal.ZERO;
        if (days.signum() == 0 && e.isEmpty()) e.add("These dates are weekly off / holiday - no leave is needed");
        if (p.maxPerMonth().signum() > 0 && r.usedThisMonth().add(days).compareTo(p.maxPerMonth()) > 0)
            e.add(p.code() + " is limited to " + p.maxPerMonth().stripTrailingZeros().toPlainString() + " day(s) a month (already " + r.usedThisMonth().stripTrailingZeros().toPlainString() + ")");
        BigDecimal lwp = BigDecimal.ZERO;
        if (p.paid() && days.compareTo(r.balance()) > 0) {
            if (p.allowNegative()) lwp = days.subtract(r.balance().max(BigDecimal.ZERO));
            else e.add("Not enough " + p.code() + " balance (" + r.balance().stripTrailingZeros().toPlainString() + " left, " + days.stripTrailingZeros().toPlainString() + " needed)");
        }
        return new Result(e, days, lwp);
    }
}
