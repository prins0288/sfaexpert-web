package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.hr.calc.SalaryCalc;
import in.opt.sfa.tenant.hr.calc.SalaryCalc.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SalaryCalcTest {
    static BigDecimal n(String s) { return new BigDecimal(s); }
    static final Params P = new Params(n("15000"), n("12"), n("3.67"), n("8.33"), n("0.5"), n("0.5"), n("21000"), n("0.75"), n("3.25"), "NEAREST");
    static Head basic = new Head("BASIC", "Basic", "EARNING", "PERCENT", "GROSS", true, true, true, 1);
    static Head hra = new Head("HRA", "HRA", "EARNING", "PERCENT", "BASIC", true, false, true, 2);
    static Head conv = new Head("CONV", "Conveyance", "EARNING", "FIXED", null, true, false, true, 3);
    static Head spl = new Head("SPL", "Special", "EARNING", "BALANCE", null, true, false, true, 4);
    static Head pt = new Head("PT", "Professional tax", "DEDUCTION", "FIXED", null, false, false, false, 10);
    static List<Value> structure() { return List.of(new Value(basic, n("50")), new Value(hra, n("40")), new Value(conv, n("1600")), new Value(spl, n("0")), new Value(pt, n("200"))); }
    static Output run(String gross, int days, String lop, boolean pf, boolean esi) { return SalaryCalc.compute(new Input(n(gross), structure(), days, n(lop), pf, esi, P)); }
    static BigDecimal line(Output o, String code) { return o.lines().stream().filter(l -> l.code().equals(code)).map(Line::amount).findFirst().orElse(null); }

    @Test void fullMonthHeads() { Output o = run("40000", 30, "0", false, false); assertEquals(n("20000.00"), line(o, "BASIC")); assertEquals(n("8000.00"), line(o, "HRA")); assertEquals(n("1600.00"), line(o, "CONV")); assertEquals(n("10400.00"), line(o, "SPL")); assertEquals(n("40000.00"), o.grossEarnings()); }
    @Test void balanceFillsTheRest() { Output o = run("30000", 30, "0", false, false); assertEquals(n("30000.00"), o.grossEarnings()); }
    @Test void lopProRates() { Output o = run("30000", 30, "3", false, false); assertEquals(n("27000.00"), o.grossEarnings()); assertEquals(n("27"), o.paidDays()); assertEquals(n("13500.00"), line(o, "BASIC")); }
    @Test void ptNotProRated() { Output o = run("30000", 30, "3", false, false); assertEquals(n("200.00"), line(o, "PT")); }
    @Test void pfOnBasicCappedAtCeiling() { Output o = run("40000", 30, "0", true, false); assertEquals(n("15000"), o.pfWage()); assertEquals(n("1800.00"), line(o, "PF_EE")); assertEquals(n("550.50"), line(o, "PF_ER_EPF")); assertEquals(n("1249.50"), line(o, "PF_ER_EPS")); }
    @Test void pfBelowCeilingUsesActualWage() { Output o = run("20000", 30, "0", true, false); assertEquals(n("10000.00"), o.pfWage()); assertEquals(n("1200.00"), line(o, "PF_EE")); }
    @Test void pfWageFollowsLop() { Output o = run("20000", 30, "15", true, false); assertEquals(n("5000.00"), o.pfWage()); assertEquals(n("600.00"), line(o, "PF_EE")); }
    @Test void noPfWhenNotApplicable() { assertNull(line(run("20000", 30, "0", false, false), "PF_EE")); }
    @Test void esiBelowLimit() { Output o = run("20000", 30, "0", false, true); assertEquals(n("150.00"), line(o, "ESI_EE")); assertEquals(n("650.00"), line(o, "ESI_ER")); }
    @Test void esiRoundsUpToRupee() { Output o = run("20001", 30, "0", false, true); assertEquals(0, line(o, "ESI_EE").remainder(BigDecimal.ONE).signum()); assertEquals(n("151.00"), line(o, "ESI_EE")); }
    @Test void noEsiAboveLimit() { assertNull(line(run("21001", 30, "0", false, true), "ESI_EE")); }
    @Test void esiStaysWhenLopDropsPay() { Output o = run("21000", 30, "20", false, true); assertNotNull(line(o, "ESI_EE")); }
    @Test void netIsGrossMinusDeductions() { Output o = run("40000", 30, "0", true, false); assertEquals(n("40000.00").subtract(n("1800.00")).subtract(n("200.00")), o.net()); assertEquals(o.grossEarnings().subtract(o.totalDeductions()), o.net()); }
    @Test void employerCostAddsEmployerShare() { Output o = run("40000", 30, "0", true, false); assertEquals(n("40000.00").add(n("550.50")).add(n("1249.50")).add(n("75.00")).add(n("75.00")), o.employerCost()); }
    @Test void lopCannotExceedMonth() { Output o = run("30000", 30, "45", false, false); assertEquals(BigDecimal.ZERO, o.paidDays()); assertEquals(n("0.00"), o.grossEarnings()); }
    @Test void headsOverGrossRejected() { assertThrows(IllegalArgumentException.class, () -> run("4000", 30, "0", false, false)); }
    @Test void percentOfMissingBaseRejected() {
        assertThrows(IllegalArgumentException.class, () -> SalaryCalc.compute(new Input(n("10000"), List.of(new Value(hra, n("40")), new Value(spl, n("0"))), 30, n("0"), false, false, P)));
    }
    @Test void zeroDaysRejected() { assertThrows(IllegalArgumentException.class, () -> run("10000", 0, "0", false, false)); }
    @Test void floorRounding() {
        Params f = new Params(n("15000"), n("12"), n("3.67"), n("8.33"), n("0.5"), n("0.5"), n("21000"), n("0.75"), n("3.25"), "FLOOR");
        Output o = SalaryCalc.compute(new Input(n("10001.019"), List.of(new Value(basic, n("50")), new Value(spl, n("0"))), 30, n("0"), false, false, f));
        assertEquals(n("5000.50"), line(o, "BASIC"));
    }
    @Test void percentDeductionOfGross() {
        Head d = new Head("TD", "Test deduction", "DEDUCTION", "PERCENT", "GROSS", false, false, false, 11);
        Output o = SalaryCalc.compute(new Input(n("10000"), List.of(new Value(basic, n("100")), new Value(d, n("2"))), 30, n("0"), false, false, P));
        assertEquals(n("200.00"), line(o, "TD"));
    }
    @Test void linesOrdered() { Output o = run("40000", 30, "0", true, true); assertEquals("EARNING", o.lines().get(0).type()); assertEquals("EMPLOYER", o.lines().get(o.lines().size() - 1).type()); }
    @Test void proRataOffKeepsFixed() {
        Head fixed = new Head("FX", "Fixed allowance", "EARNING", "FIXED", null, false, false, false, 5);
        Output o = SalaryCalc.compute(new Input(n("10000"), List.of(new Value(basic, n("50")), new Value(fixed, n("1000")), new Value(spl, n("0"))), 30, n("10"), false, false, P));
        assertEquals(n("1000.00"), line(o, "FX"));
    }
}
