package in.opt.sfa.tenant.hr.calc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Pure loss-of-pay calculation of a month from attendance facts. A working day with no submitted DCR (and no leave covering it) is ABSENT; a day of
 * unpaid leave is loss of pay; paid leave is not. Weekly offs / holidays are never absent. Days before joining / after the last working day are not paid.
 */
public final class AttendanceCalc {

    private AttendanceCalc() { }

    /** A day (or half) of leave: {@code part} is the fraction of the day it covers (1 or 0.5), {@code paid} says whether it is paid leave. */
    public record LeaveDay(BigDecimal part, boolean paid) { }

    public record Input(LocalDate first, LocalDate last, LocalDate cutoff, Set<LocalDate> nonWorking, LocalDate joined, LocalDate lastWorking,
                        Map<LocalDate, LeaveDay> leave, Set<LocalDate> dcrDays, boolean useDcr, BigDecimal extraLwp) { }

    public record Result(BigDecimal absent, BigDecimal unpaidLeave, BigDecimal notEmployed, BigDecimal extraLwp, List<LocalDate> absentDates) {
        public BigDecimal total() { return absent.add(unpaidLeave).add(notEmployed).add(extraLwp); }
        public String note() {
            List<String> p = new ArrayList<>();
            if (absent.signum() > 0) p.add("Absent " + absent.stripTrailingZeros().toPlainString() + " (" + absentDates.stream().map(d -> String.valueOf(d.getDayOfMonth())).reduce((a, b) -> a + ", " + b).orElse("") + ")");
            if (unpaidLeave.signum() > 0) p.add("Unpaid leave " + unpaidLeave.stripTrailingZeros().toPlainString());
            if (extraLwp.signum() > 0) p.add("Leave over balance " + extraLwp.stripTrailingZeros().toPlainString());
            if (notEmployed.signum() > 0) p.add("Not in service " + notEmployed.stripTrailingZeros().toPlainString());
            return String.join("; ", p);
        }
    }

    private static final BigDecimal ONE = BigDecimal.ONE;

    public static Result compute(Input in) {
        BigDecimal absent = BigDecimal.ZERO, unpaid = BigDecimal.ZERO, notEmp = BigDecimal.ZERO;
        List<LocalDate> absentDates = new ArrayList<>();
        for (LocalDate d = in.first(); !d.isAfter(in.last()); d = d.plusDays(1)) {
            if ((in.joined() != null && d.isBefore(in.joined())) || (in.lastWorking() != null && d.isAfter(in.lastWorking()))) { notEmp = notEmp.add(ONE); continue; }
            LeaveDay lv = in.leave().get(d);
            if (lv != null && !lv.paid()) unpaid = unpaid.add(lv.part());
            if (in.nonWorking().contains(d)) continue;                                   // weekly off / holiday: never absent
            BigDecimal rest = ONE.subtract(lv == null ? BigDecimal.ZERO : lv.part());
            if (rest.signum() > 0 && in.useDcr() && d.isBefore(in.cutoff()) && !in.dcrDays().contains(d)) { absent = absent.add(rest); absentDates.add(d); }
        }
        BigDecimal extra = in.extraLwp() == null ? BigDecimal.ZERO : in.extraLwp();
        return new Result(absent, unpaid, notEmp, extra, absentDates);
    }
}
