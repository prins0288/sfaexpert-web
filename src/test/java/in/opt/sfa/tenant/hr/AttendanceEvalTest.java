package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.dcr.view.service.AttendanceEval;
import in.opt.sfa.tenant.dcr.view.service.AttendanceEval.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceEvalTest {
    static Rule rule(int minCalls, String callsRes, String minHours, String hoursRes) {
        return new Rule(Set.of("WORKING"), minCalls, callsRes, true, new BigDecimal(minHours), hoursRes, null, "HALF", null, "HALF", false);
    }
    static Day day(int status, String wt, int listed, int unlisted, Integer workMin) { return new Day(status, wt, listed, unlisted, workMin, LocalTime.of(9, 0), LocalTime.of(18, 0)); }

    @Test void noRuleSubmittedIsPresent() { assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 0, 0, 100), Rule.none()).result()); }
    @Test void draftIsAbsent() { assertEquals(Result.ABSENT, AttendanceEval.evaluate(day(0, "WORKING", 12, 0, 500), Rule.none()).result()); }
    @Test void rejectedIsAbsent() { assertEquals(Result.ABSENT, AttendanceEval.evaluate(day(3, "WORKING", 12, 0, 500), Rule.none()).result()); }
    @Test void needApprovedOnlyApprovedCounts() {
        Rule r = new Rule(Set.of("WORKING"), 0, "ABSENT", true, BigDecimal.ZERO, "HALF", null, "HALF", null, "HALF", true);
        assertEquals(Result.ABSENT, AttendanceEval.evaluate(day(1, "WORKING", 5, 0, 500), r).result());
        assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(2, "WORKING", 5, 0, 500), r).result());
    }
    @Test void fewerCallsThanMinimumIsAbsent() {
        Outcome o = AttendanceEval.evaluate(day(1, "WORKING", 6, 0, 500), rule(10, "ABSENT", "0", "HALF"));
        assertEquals(Result.ABSENT, o.result()); assertTrue(o.reasons().get(0).contains("6 call"));
    }
    @Test void exactlyMinimumCallsIsPresent() { assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 10, 0, 500), rule(10, "ABSENT", "0", "HALF")).result()); }
    @Test void unlistedCallsCountWhenAsked() {
        assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 7, 3, 500), rule(10, "ABSENT", "0", "HALF")).result());
        Rule noUnl = new Rule(Set.of("WORKING"), 10, "ABSENT", false, BigDecimal.ZERO, "HALF", null, "HALF", null, "HALF", false);
        assertEquals(Result.ABSENT, AttendanceEval.evaluate(day(1, "WORKING", 7, 3, 500), noUnl).result());
    }
    @Test void fewerHoursIsHalfDay() {
        Outcome o = AttendanceEval.evaluate(day(1, "WORKING", 12, 0, 270), rule(10, "ABSENT", "5", "HALF"));   // 4h 30m of 5h
        assertEquals(Result.HALF, o.result()); assertTrue(o.reasons().get(0).contains("4h 30m"));
    }
    @Test void fiveHoursExactlyIsFullDay() { assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 12, 0, 300), rule(10, "ABSENT", "5", "HALF")).result()); }
    @Test void hoursUnknownIsNotChecked() { assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 12, 0, null), rule(10, "ABSENT", "5", "HALF")).result()); }
    @Test void worstResultWins() {
        Outcome o = AttendanceEval.evaluate(day(1, "WORKING", 3, 0, 200), rule(10, "ABSENT", "5", "HALF"));
        assertEquals(Result.ABSENT, o.result()); assertEquals(2, o.reasons().size());
    }
    @Test void callsCanBeHalfDayToo() { assertEquals(Result.HALF, AttendanceEval.evaluate(day(1, "WORKING", 6, 0, 500), rule(10, "HALF", "0", "HALF")).result()); }
    @Test void hoursCanBeAbsentToo() { assertEquals(Result.ABSENT, AttendanceEval.evaluate(day(1, "WORKING", 12, 0, 100), rule(0, "ABSENT", "5", "ABSENT")).result()); }
    @Test void otherWorkTypesAreNotChecked() {
        assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "R_TRAINING", 0, 0, 30), rule(10, "ABSENT", "5", "ABSENT")).result());
        assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "ATTENDANCE", 0, 0, null), rule(10, "ABSENT", "5", "ABSENT")).result());
    }
    @Test void lateStartAndEarlyEnd() {
        Rule r = new Rule(Set.of("WORKING"), 0, "ABSENT", true, BigDecimal.ZERO, "HALF", LocalTime.of(10, 30), "HALF", LocalTime.of(17, 0), "HALF", false);
        assertEquals(Result.PRESENT, AttendanceEval.evaluate(new Day(1, "WORKING", 5, 0, 480, LocalTime.of(10, 30), LocalTime.of(17, 0)), r).result());
        assertEquals(Result.HALF, AttendanceEval.evaluate(new Day(1, "WORKING", 5, 0, 480, LocalTime.of(11, 0), LocalTime.of(18, 0)), r).result());
        assertEquals(Result.HALF, AttendanceEval.evaluate(new Day(1, "WORKING", 5, 0, 480, LocalTime.of(9, 0), LocalTime.of(16, 30)), r).result());
    }
    @Test void lateStartCanBeAbsent() {
        Rule r = new Rule(Set.of("WORKING"), 0, "ABSENT", true, BigDecimal.ZERO, "HALF", LocalTime.of(10, 0), "ABSENT", null, "HALF", false);
        assertEquals(Result.ABSENT, AttendanceEval.evaluate(new Day(1, "WORKING", 5, 0, 480, LocalTime.of(10, 5), LocalTime.of(18, 0)), r).result());
    }
    @Test void zeroMeansNotChecked() { assertEquals(Result.PRESENT, AttendanceEval.evaluate(day(1, "WORKING", 0, 0, 10), rule(0, "ABSENT", "0", "HALF")).result()); }
}
