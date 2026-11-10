package in.opt.sfa.tenant.hr.common;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure effective-date resolver: a rule row is valid on a date when {@code from <= date} and ({@code to} is empty or {@code to >= date}).
 * When several rows cover the date (overlap) the one with the LATEST {@code from} wins, so a newer revision overrides an older one.
 */
public final class EffectiveDated {

    private EffectiveDated() { }

    public static <T> Optional<T> pick(List<T> rows, LocalDate date, Function<T, LocalDate> from, Function<T, LocalDate> to) {
        if (rows == null || date == null) return Optional.empty();
        return rows.stream()
                .filter(r -> from.apply(r) != null && !from.apply(r).isAfter(date))
                .filter(r -> to.apply(r) == null || !to.apply(r).isBefore(date))
                .max(Comparator.comparing(from));
    }

    /** The day a previous open row must end when a new revision starts on {@code newFrom}. */
    public static LocalDate closeDateFor(LocalDate newFrom) { return newFrom.minusDays(1); }

    /** True when two ranges share at least one day (open end = forever). */
    public static boolean overlaps(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
        boolean aBeforeB = aTo != null && aTo.isBefore(bFrom);
        boolean bBeforeA = bTo != null && bTo.isBefore(aFrom);
        return !(aBeforeB || bBeforeA);
    }
}
