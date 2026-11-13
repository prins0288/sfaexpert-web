package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.hr.calc.LeaveRules;
import in.opt.sfa.tenant.hr.calc.LeaveRules.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeaveRulesTest {
    static LocalDate d(String s) { return LocalDate.parse(s); }
    static BigDecimal n(String s) { return new BigDecimal(s); }
    // Sat 2026-11-14 and Sun 2026-11-15 are weekly off
    static final Set<LocalDate> OFF = Set.of(d("2026-11-14"), d("2026-11-15"), d("2026-11-21"), d("2026-11-22"));
    static final LocalDate TODAY = d("2026-11-10");

    static Policy cl() { return new Policy("CL", true, n("12"), "MONTHLY", n("0"), true, 3, n("3"), 0, 7, 0, true, false, false, 0); }
    static Request req(String from, String to, String part, BigDecimal bal) {
        return new Request(d(from), d(to), part, "fever", TODAY, d("2020-01-01"), bal, n("0"), OFF, false);
    }

    @Test void countsWorkingDaysOnly() { assertEquals(n("3"), LeaveRules.countDays(d("2026-11-11"), d("2026-11-13"), "FULL", OFF, false)); }
    @Test void weekendInsideRangeSkippedWithoutSandwich() { assertEquals(n("2"), LeaveRules.countDays(d("2026-11-13"), d("2026-11-16"), "FULL", OFF, false)); }
    @Test void sandwichCountsWeekendBetween() { assertEquals(n("4"), LeaveRules.countDays(d("2026-11-13"), d("2026-11-16"), "FULL", OFF, true)); }
    @Test void sandwichIgnoresLeadingAndTrailingOff() { assertEquals(n("1"), LeaveRules.countDays(d("2026-11-13"), d("2026-11-15"), "FULL", OFF, true)); assertEquals(n("1"), LeaveRules.countDays(d("2026-11-14"), d("2026-11-16"), "FULL", OFF, true)); }
    @Test void allOffIsZero() { assertEquals(BigDecimal.ZERO, LeaveRules.countDays(d("2026-11-14"), d("2026-11-15"), "FULL", OFF, true)); }
    @Test void halfDay() { assertEquals(n("0.5"), LeaveRules.countDays(d("2026-11-12"), d("2026-11-12"), "FIRST", OFF, false)); }
    @Test void badRangeIsZero() { assertEquals(BigDecimal.ZERO, LeaveRules.countDays(d("2026-11-12"), d("2026-11-11"), "FULL", OFF, false)); assertEquals(BigDecimal.ZERO, LeaveRules.countDays(null, d("2026-11-11"), "FULL", OFF, false)); }

    @Test void yearStartFinancialAndCalendar() {
        assertEquals(d("2026-04-01"), LeaveRules.yearStart(d("2026-11-10"), 4, "FINANCIAL"));
        assertEquals(d("2026-04-01"), LeaveRules.yearStart(d("2027-03-31"), 4, "FINANCIAL").minusYears(0).withYear(2026));
        assertEquals(d("2025-04-01"), LeaveRules.yearStart(d("2026-02-01"), 4, "FINANCIAL"));
        assertEquals(d("2026-01-01"), LeaveRules.yearStart(d("2026-11-10"), 4, "CALENDAR"));
    }
    @Test void monthlyAccrual() {
        assertEquals(n("1.00"), LeaveRules.accrued(n("12"), "MONTHLY", d("2026-04-01"), d("2026-04-20"), d("2020-01-01")));
        assertEquals(n("8.00"), LeaveRules.accrued(n("12"), "MONTHLY", d("2026-04-01"), d("2026-11-10"), d("2020-01-01")));
        assertEquals(n("12.00"), LeaveRules.accrued(n("12"), "MONTHLY", d("2026-04-01"), d("2027-03-31"), d("2020-01-01")));
    }
    @Test void upfrontAccrual() { assertEquals(n("12.00"), LeaveRules.accrued(n("12"), "UPFRONT", d("2026-04-01"), d("2026-04-02"), d("2020-01-01"))); }
    @Test void proRataForJoiner() {
        assertEquals(n("6.00"), LeaveRules.accrued(n("12"), "UPFRONT", d("2026-04-01"), d("2026-11-10"), d("2026-10-15")));
        assertEquals(n("2.00"), LeaveRules.accrued(n("12"), "MONTHLY", d("2026-04-01"), d("2026-11-10"), d("2026-10-15")));
    }
    @Test void notJoinedYetEarnsNothing() { assertEquals(BigDecimal.ZERO, LeaveRules.accrued(n("12"), "UPFRONT", d("2026-04-01"), d("2026-04-30"), d("2026-06-01")).stripTrailingZeros().max(BigDecimal.ZERO)); }
    @Test void roundsDownToHalf() { assertEquals(n("0.50"), LeaveRules.accrued(n("7"), "MONTHLY", d("2026-04-01"), d("2026-04-10"), d("2020-01-01"))); }
    @Test void carryForwardCapped() { assertEquals(n("5.00"), LeaveRules.carryForward(n("8"), n("5"))); assertEquals(n("3.00"), LeaveRules.carryForward(n("3"), n("5"))); assertEquals(n("0.00"), LeaveRules.carryForward(n("-2"), n("5"))); }

    @Test void validRequestPasses() { Result r = LeaveRules.check(cl(), req("2026-11-11", "2026-11-12", "FULL", n("5"))); assertTrue(r.ok(), r.errors().toString()); assertEquals(n("2"), r.days()); }
    @Test void reasonRequired() { Request q = new Request(d("2026-11-11"), d("2026-11-11"), "FULL", " ", TODAY, d("2020-01-01"), n("5"), n("0"), OFF, false); assertFalse(LeaveRules.check(cl(), q).ok()); }
    @Test void insufficientBalance() { Result r = LeaveRules.check(cl(), req("2026-11-11", "2026-11-12", "FULL", n("1"))); assertFalse(r.ok()); assertTrue(r.errors().get(0).contains("Not enough")); }
    @Test void shortfallBecomesLwpWhenAllowed() {
        Policy p = new Policy("PL", true, n("18"), "MONTHLY", n("0"), true, 0, n("0"), 0, 7, 0, true, false, true, 0);
        Result r = LeaveRules.check(p, req("2026-11-11", "2026-11-13", "FULL", n("1")));
        assertTrue(r.ok()); assertEquals(n("2"), r.lwpDays());
    }
    @Test void unpaidTypeNeedsNoBalance() {
        Policy lwp = new Policy("LWP", false, n("0"), "UPFRONT", n("0"), true, 0, n("0"), 0, 7, 0, true, false, true, 0);
        Result r = LeaveRules.check(lwp, req("2026-11-11", "2026-11-12", "FULL", n("0"))); assertTrue(r.ok()); assertEquals(BigDecimal.ZERO, r.lwpDays());
    }
    @Test void maxConsecutive() { assertFalse(LeaveRules.check(cl(), req("2026-11-10", "2026-11-13", "FULL", n("9"))).ok()); }
    @Test void backdateLimit() { assertFalse(LeaveRules.check(cl(), req("2026-10-30", "2026-10-30", "FULL", n("9"))).ok()); assertTrue(LeaveRules.check(cl(), req("2026-11-04", "2026-11-04", "FULL", n("9"))).ok()); }
    @Test void noticeRequired() {
        Policy pl = new Policy("PL", true, n("18"), "MONTHLY", n("0"), false, 0, n("0"), 7, 0, 0, true, false, false, 0);
        assertFalse(LeaveRules.check(pl, req("2026-11-12", "2026-11-12", "FULL", n("9"))).ok());
        assertTrue(LeaveRules.check(pl, req("2026-11-17", "2026-11-17", "FULL", n("9"))).ok());
    }
    @Test void futureLimit() {
        Policy p = new Policy("X", true, n("12"), "UPFRONT", n("0"), true, 0, n("0"), 0, 0, 30, true, false, false, 0);
        assertFalse(LeaveRules.check(p, req("2027-01-20", "2027-01-20", "FULL", n("9"))).ok());
    }
    @Test void halfDayNotAllowedForType() {
        Policy p = new Policy("PL", true, n("18"), "MONTHLY", n("0"), false, 0, n("0"), 0, 7, 0, true, false, false, 0);
        assertFalse(LeaveRules.check(p, req("2026-11-12", "2026-11-12", "FIRST", n("9"))).ok());
    }
    @Test void halfDayOnlyOneDate() { assertFalse(LeaveRules.check(cl(), req("2026-11-11", "2026-11-12", "SECOND", n("9"))).ok()); }
    @Test void perMonthLimit() {
        Request q = new Request(d("2026-11-11"), d("2026-11-12"), "FULL", "x", TODAY, d("2020-01-01"), n("9"), n("2"), OFF, false);
        assertFalse(LeaveRules.check(cl(), q).ok());
    }
    @Test void minService() {
        Policy pl = new Policy("PL", true, n("18"), "MONTHLY", n("0"), false, 0, n("0"), 0, 7, 0, true, false, false, 6);
        Request q = new Request(d("2026-11-11"), d("2026-11-11"), "FULL", "x", TODAY, d("2026-09-01"), n("9"), n("0"), OFF, false);
        assertFalse(LeaveRules.check(pl, q).ok());
    }
    @Test void overlapBlocked() { Request q = new Request(d("2026-11-11"), d("2026-11-11"), "FULL", "x", TODAY, d("2020-01-01"), n("9"), n("0"), OFF, true); assertFalse(LeaveRules.check(cl(), q).ok()); }
    @Test void weekendOnlyNeedsNoLeave() { assertFalse(LeaveRules.check(cl(), req("2026-11-14", "2026-11-15", "FULL", n("9"))).ok()); }
    @Test void toBeforeFrom() { assertFalse(LeaveRules.check(cl(), req("2026-11-12", "2026-11-11", "FULL", n("9"))).ok()); }
    @Test void missingDates() { assertFalse(LeaveRules.check(cl(), new Request(null, null, "FULL", "x", TODAY, null, n("1"), n("0"), OFF, false)).ok()); }
    @Test void sandwichPolicyCountsWeekend() {
        Policy p = new Policy("SL", true, n("12"), "UPFRONT", n("0"), true, 0, n("0"), 0, 7, 0, true, true, false, 0);
        assertEquals(n("4"), LeaveRules.check(p, req("2026-11-13", "2026-11-16", "FULL", n("12"))).days());
    }
}
