package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.hr.calc.AttendanceCalc;
import in.opt.sfa.tenant.hr.calc.AttendanceCalc.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceCalcTest {
    static LocalDate d(String s) { return LocalDate.parse(s); }
    static BigDecimal n(String s) { return new BigDecimal(s); }
    // November 2026: Sundays 1, 8, 15, 22, 29 are off
    static final Set<LocalDate> OFF = Set.of(d("2026-11-01"), d("2026-11-08"), d("2026-11-15"), d("2026-11-22"), d("2026-11-29"));
    static Set<LocalDate> workedAll() { Set<LocalDate> s = new HashSet<>(); for (int i = 1; i <= 30; i++) { LocalDate x = LocalDate.of(2026, 11, i); if (!OFF.contains(x)) s.add(x); } return s; }
    static Result run(Set<LocalDate> dcr, Map<LocalDate, LeaveDay> leave, boolean useDcr) { return AttendanceCalc.compute(new Input(d("2026-11-01"), d("2026-11-30"), d("2026-12-01"), OFF, null, null, leave, dcr, useDcr, null)); }

    @Test void everyDayHasDcrNoLop() { assertEquals(n("0"), run(workedAll(), Map.of(), true).total()); }
    @Test void missingDcrIsAbsent() { Set<LocalDate> s = workedAll(); s.remove(d("2026-11-05")); s.remove(d("2026-11-12")); Result r = run(s, Map.of(), true); assertEquals(n("2"), r.absent()); assertEquals(List.of(d("2026-11-05"), d("2026-11-12")), r.absentDates()); }
    @Test void weekOffWithoutDcrIsNotAbsent() { assertEquals(n("0"), run(workedAll(), Map.of(), true).absent()); }
    @Test void paidLeaveDayIsNotDeducted() { Set<LocalDate> s = workedAll(); s.remove(d("2026-11-05")); Result r = run(s, Map.of(d("2026-11-05"), new LeaveDay(n("1"), true)), true); assertEquals(n("0"), r.total()); }
    @Test void unpaidLeaveIsDeducted() { Set<LocalDate> s = workedAll(); s.remove(d("2026-11-05")); Result r = run(s, Map.of(d("2026-11-05"), new LeaveDay(n("1"), false)), true); assertEquals(n("1"), r.unpaidLeave()); assertEquals(n("0"), r.absent()); assertEquals(n("1"), r.total()); }
    @Test void halfDayPaidLeaveWithoutDcrIsHalfAbsent() { Set<LocalDate> s = workedAll(); s.remove(d("2026-11-05")); Result r = run(s, Map.of(d("2026-11-05"), new LeaveDay(n("0.5"), true)), true); assertEquals(n("0.5"), r.absent()); assertEquals(n("0.5"), r.total()); }
    @Test void halfDayUnpaidWithDcrIsHalfLop() { Result r = run(workedAll(), Map.of(d("2026-11-05"), new LeaveDay(n("0.5"), false)), true); assertEquals(n("0.5"), r.total()); }
    @Test void sandwichedUnpaidWeekendCounts() { Result r = run(workedAll(), Map.of(d("2026-11-08"), new LeaveDay(n("1"), false)), true); assertEquals(n("1"), r.unpaidLeave()); }
    @Test void sandwichedPaidWeekendCostsNothing() { assertEquals(n("0"), run(workedAll(), Map.of(d("2026-11-08"), new LeaveDay(n("1"), true)), true).total()); }
    @Test void noDcrRuleMeansNoAbsent() { assertEquals(n("0"), run(new HashSet<>(), Map.of(), false).total()); }
    @Test void joinedMidMonth() { Result r = AttendanceCalc.compute(new Input(d("2026-11-01"), d("2026-11-30"), d("2026-12-01"), OFF, d("2026-11-11"), null, Map.of(), workedAll(), true, null)); assertEquals(n("10"), r.notEmployed()); }
    @Test void leftMidMonth() { Result r = AttendanceCalc.compute(new Input(d("2026-11-01"), d("2026-11-30"), d("2026-12-01"), OFF, null, d("2026-11-20"), Map.of(), workedAll(), true, null)); assertEquals(n("10"), r.notEmployed()); }
    @Test void daysFromTodayAreNotAbsentYet() { Input in = new Input(d("2026-11-01"), d("2026-11-30"), d("2026-11-10"), OFF, null, null, Map.of(), new HashSet<>(), true, null); assertEquals(n("7"), AttendanceCalc.compute(in).absent()); }   // 2..9 minus Sunday the 8th
    @Test void extraLwpIsAdded() { Result r = AttendanceCalc.compute(new Input(d("2026-11-01"), d("2026-11-30"), d("2026-12-01"), OFF, null, null, Map.of(), workedAll(), true, n("2"))); assertEquals(n("2"), r.total()); }
    @Test void noteListsEverything() {
        Set<LocalDate> s = workedAll(); s.remove(d("2026-11-05")); s.remove(d("2026-11-06")); s.remove(d("2026-11-09"));
        Result r = run(s, Map.of(d("2026-11-09"), new LeaveDay(n("1"), false)), true);
        assertEquals("Absent 2 (5, 6); Unpaid leave 1", r.note());
    }
    @Test void absentOnLeaveDayNotDoubleCounted() { Result r = run(new HashSet<>(), Map.of(d("2026-11-05"), new LeaveDay(n("1"), true)), true); assertEquals(n("0"), r.unpaidLeave()); assertEquals(n("0"), r.absentDates().stream().filter(x -> x.equals(d("2026-11-05"))).count() == 0 ? n("0") : n("1")); }
}
