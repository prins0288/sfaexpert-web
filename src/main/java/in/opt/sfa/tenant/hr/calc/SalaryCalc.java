package in.opt.sfa.tenant.hr.calc;

import in.opt.sfa.tenant.hr.common.Money;

import java.math.BigDecimal;
import java.util.*;

/**
 * Pure monthly salary calculation: structure (heads + values) + days + loss-of-pay -> payslip lines, PF, ESI and net. No database, no clock.
 * Head calc types: FIXED (value = rupees a month), PERCENT (value = percent of the base head, or of GROSS), BALANCE (earning that fills the rest of the gross).
 */
public final class SalaryCalc {

    private SalaryCalc() { }

    public record Head(String code, String name, String type, String calc, String base, boolean prorata, boolean pfWage, boolean esiWage, int sort) { }
    public record Value(Head head, BigDecimal value) { }
    public record Params(BigDecimal pfCeiling, BigDecimal pfEeRate, BigDecimal pfErEpfRate, BigDecimal pfErEpsRate, BigDecimal pfEdliRate, BigDecimal pfAdminRate,
                         BigDecimal esiLimit, BigDecimal esiEeRate, BigDecimal esiErRate, String rounding) { }
    public record Input(BigDecimal gross, List<Value> values, int daysInMonth, BigDecimal lopDays, boolean pfApplicable, boolean esiApplicable, Params p) { }
    public record Line(String code, String name, String type, BigDecimal amount, int sort) { }
    public record Output(List<Line> lines, BigDecimal paidDays, BigDecimal grossEarnings, BigDecimal totalDeductions, BigDecimal net, BigDecimal employerCost, BigDecimal pfWage, BigDecimal esiWage) { }

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static Output compute(Input in) {
        Params p = in.p();
        String rm = p.rounding();
        if (in.daysInMonth() <= 0) throw new IllegalArgumentException("Days in month must be positive");
        BigDecimal lop = in.lopDays() == null ? BigDecimal.ZERO : in.lopDays();
        BigDecimal days = BigDecimal.valueOf(in.daysInMonth());
        BigDecimal paidDays = days.subtract(lop).max(BigDecimal.ZERO).min(days);

        List<Value> earnings = in.values().stream().filter(v -> "EARNING".equals(v.head().type())).sorted(Comparator.comparingInt(v -> v.head().sort())).toList();
        List<Value> deductions = in.values().stream().filter(v -> "DEDUCTION".equals(v.head().type())).sorted(Comparator.comparingInt(v -> v.head().sort())).toList();

        // 1. full-month amounts of earnings
        Map<String, BigDecimal> full = new LinkedHashMap<>();
        BigDecimal fixedSum = BigDecimal.ZERO;
        for (Value v : earnings) {
            if ("BALANCE".equals(v.head().calc())) continue;
            BigDecimal amt = amount(v, in.gross(), full, rm);
            full.put(v.head().code(), amt);
            fixedSum = fixedSum.add(amt);
        }
        for (Value v : earnings) {
            if (!"BALANCE".equals(v.head().calc())) continue;
            BigDecimal rest = in.gross().subtract(fixedSum);
            if (rest.signum() < 0) throw new IllegalArgumentException("Heads add up to more than the gross (" + fixedSum.toPlainString() + " > " + in.gross().toPlainString() + ")");
            full.put(v.head().code(), Money.round(rest, rm, 2));
            fixedSum = fixedSum.add(rest);
        }
        // 2. paid amounts
        List<Line> lines = new ArrayList<>();
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        BigDecimal grossPaid = BigDecimal.ZERO, pfWage = BigDecimal.ZERO, esiWage = BigDecimal.ZERO;
        for (Value v : earnings) {
            BigDecimal f = full.get(v.head().code());
            BigDecimal a = v.head().prorata() ? Money.round(f.multiply(paidDays).divide(days, 6, java.math.RoundingMode.HALF_UP), rm, 2) : f;
            paid.put(v.head().code(), a);
            lines.add(new Line(v.head().code(), v.head().name(), "EARNING", a, v.head().sort()));
            grossPaid = grossPaid.add(a);
            if (v.head().pfWage()) pfWage = pfWage.add(a);
            if (v.head().esiWage()) esiWage = esiWage.add(a);
        }
        // 3. structure deductions
        BigDecimal ded = BigDecimal.ZERO;
        for (Value v : deductions) {
            BigDecimal a = "PERCENT".equals(v.head().calc()) ? Money.round(base(v, in.gross(), paid).multiply(v.value()).divide(HUNDRED, 6, java.math.RoundingMode.HALF_UP), rm, 2) : Money.round(v.value(), rm, 2);
            if (v.head().prorata() && !"PERCENT".equals(v.head().calc())) a = Money.round(a.multiply(paidDays).divide(days, 6, java.math.RoundingMode.HALF_UP), rm, 2);
            lines.add(new Line(v.head().code(), v.head().name(), "DEDUCTION", a, v.head().sort()));
            ded = ded.add(a);
        }
        // 4. PF
        BigDecimal employer = BigDecimal.ZERO, pfBase = pfWage.min(p.pfCeiling());
        if (in.pfApplicable() && pfBase.signum() > 0) {
            BigDecimal ee = pct(pfBase, p.pfEeRate(), rm);
            lines.add(new Line("PF_EE", "Provident Fund (employee)", "DEDUCTION", ee, 900));
            ded = ded.add(ee);
            BigDecimal epf = pct(pfBase, p.pfErEpfRate(), rm), eps = pct(pfBase, p.pfErEpsRate(), rm), edli = pct(pfBase, p.pfEdliRate(), rm), adm = pct(pfBase, p.pfAdminRate(), rm);
            lines.add(new Line("PF_ER_EPF", "PF employer - EPF", "EMPLOYER", epf, 910));
            lines.add(new Line("PF_ER_EPS", "PF employer - EPS", "EMPLOYER", eps, 911));
            lines.add(new Line("PF_EDLI", "EDLI", "EMPLOYER", edli, 912));
            lines.add(new Line("PF_ADMIN", "PF admin charges", "EMPLOYER", adm, 913));
            employer = employer.add(epf).add(eps).add(edli).add(adm);
        }
        // 5. ESI: decided on the contract gross of the month, charged on the paid ESI wage; rounded UP to the rupee
        if (in.esiApplicable() && in.gross().compareTo(p.esiLimit()) <= 0 && esiWage.signum() > 0) {
            BigDecimal ee = Money.round(esiWage.multiply(p.esiEeRate()).divide(HUNDRED, 6, java.math.RoundingMode.HALF_UP), "CEIL", 0).setScale(2);
            BigDecimal er = Money.round(esiWage.multiply(p.esiErRate()).divide(HUNDRED, 6, java.math.RoundingMode.HALF_UP), "CEIL", 0).setScale(2);
            lines.add(new Line("ESI_EE", "ESI (employee)", "DEDUCTION", ee, 920));
            lines.add(new Line("ESI_ER", "ESI employer", "EMPLOYER", er, 921));
            ded = ded.add(ee); employer = employer.add(er);
        }
        // deductions can never be more than what was earned (e.g. no paid days in the month): cut them back from the last one, so net pay is not negative
        if (ded.compareTo(grossPaid) > 0) {
            BigDecimal over = ded.subtract(grossPaid);
            for (int i = lines.size() - 1; i >= 0 && over.signum() > 0; i--) {
                Line l = lines.get(i);
                if (!"DEDUCTION".equals(l.type())) continue;
                BigDecimal cut = l.amount().min(over);
                lines.set(i, new Line(l.code(), l.name(), l.type(), l.amount().subtract(cut), l.sort()));
                over = over.subtract(cut);
            }
            ded = grossPaid;
        }
        BigDecimal net = grossPaid.subtract(ded);
        lines.sort(Comparator.comparing((Line l) -> "EARNING".equals(l.type()) ? 0 : "DEDUCTION".equals(l.type()) ? 1 : 2).thenComparingInt(Line::sort));
        return new Output(lines, paidDays, grossPaid, ded, net, grossPaid.add(employer), pfBase, esiWage);
    }

    private static BigDecimal pct(BigDecimal base, BigDecimal rate, String rm) { return Money.round(base.multiply(rate).divide(HUNDRED, 6, java.math.RoundingMode.HALF_UP), rm, 2); }

    private static BigDecimal base(Value v, BigDecimal gross, Map<String, BigDecimal> known) {
        String b = v.head().base();
        if (b == null || b.isBlank() || "GROSS".equals(b)) return gross;
        BigDecimal x = known.get(b);
        if (x == null) throw new IllegalArgumentException(v.head().code() + " is a percent of " + b + ", which is not in this salary (or comes later)");
        return x;
    }

    private static BigDecimal amount(Value v, BigDecimal gross, Map<String, BigDecimal> full, String rm) {
        if ("PERCENT".equals(v.head().calc())) return Money.round(base(v, gross, full).multiply(v.value()).divide(HUNDRED, 6, java.math.RoundingMode.HALF_UP), rm, 2);
        return Money.round(v.value(), rm, 2);
    }
}
