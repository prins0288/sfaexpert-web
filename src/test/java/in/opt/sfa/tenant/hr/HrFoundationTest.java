package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.hr.calc.RoleResolver;
import in.opt.sfa.tenant.hr.common.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HrFoundationTest {

    record Row(String v, LocalDate from, LocalDate to) { }
    static LocalDate d(String s) { return LocalDate.parse(s); }
    static Optional<Row> pick(List<Row> rows, String date) { return EffectiveDated.pick(rows, d(date), Row::from, Row::to); }

    @Test void picksRowCoveringDate() {
        var rows = List.of(new Row("old", d("2000-04-01"), d("2026-09-16")), new Row("new", d("2026-09-17"), null));
        assertEquals("old", pick(rows, "2026-09-16").get().v());
        assertEquals("new", pick(rows, "2026-09-17").get().v());
        assertEquals("new", pick(rows, "2030-01-01").get().v());
    }
    @Test void nothingBeforeFirstRow() { assertTrue(pick(List.of(new Row("a", d("2026-01-01"), null)), "2025-12-31").isEmpty()); }
    @Test void gapReturnsEmpty() {
        var rows = List.of(new Row("a", d("2026-01-01"), d("2026-01-31")), new Row("b", d("2026-03-01"), null));
        assertTrue(pick(rows, "2026-02-10").isEmpty());
    }
    @Test void overlapLatestFromWins() {
        var rows = List.of(new Row("a", d("2026-01-01"), null), new Row("b", d("2026-02-01"), null));
        assertEquals("b", pick(rows, "2026-02-01").get().v());
        assertEquals("a", pick(rows, "2026-01-31").get().v());
    }
    @Test void endDateInclusive() { assertTrue(pick(List.of(new Row("a", d("2026-01-01"), d("2026-01-31"))), "2026-01-31").isPresent()); }
    @Test void nullInputs() {
        assertTrue(EffectiveDated.pick(null, d("2026-01-01"), Row::from, Row::to).isEmpty());
        assertTrue(EffectiveDated.pick(List.of(), null, Row::from, Row::to).isEmpty());
    }
    @Test void closeDateIsDayBefore() { assertEquals(d("2026-09-16"), EffectiveDated.closeDateFor(d("2026-09-17"))); }
    @Test void overlapsDetection() {
        assertTrue(EffectiveDated.overlaps(d("2026-01-01"), null, d("2026-06-01"), d("2026-06-30")));
        assertFalse(EffectiveDated.overlaps(d("2026-01-01"), d("2026-01-31"), d("2026-02-01"), null));
        assertTrue(EffectiveDated.overlaps(d("2026-01-01"), d("2026-02-01"), d("2026-02-01"), null));
    }

    @Test void intSetting() {
        assertEquals("25000", SettingTypes.normalise("INT", " 25000 ", "0", "100000", null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("INT", "12.5", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("INT", "-1", "0", null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("INT", "101", null, "100", null));
    }
    @Test void decimalSetting() {
        assertEquals("3.67", SettingTypes.normalise("DECIMAL", "3.670", null, null, null));
        assertEquals("12", SettingTypes.normalise("DECIMAL", "12.00", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("DECIMAL", "abc", null, null, null));
    }
    @Test void boolSetting() {
        assertEquals("true", SettingTypes.normalise("BOOL", "Yes", null, null, null));
        assertEquals("false", SettingTypes.normalise("BOOL", "0", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("BOOL", "maybe", null, null, null));
    }
    @Test void enumSetting() {
        assertEquals("FLOOR", SettingTypes.normalise("ENUM", "FLOOR", null, null, "NEAREST,FLOOR,CEIL"));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("ENUM", "UP", null, null, "NEAREST,FLOOR,CEIL"));
    }
    @Test void jsonStringAndBlank() {
        assertEquals("[1]", SettingTypes.normalise("JSON", "[1]", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("JSON", "x", null, null, null));
        assertEquals("abc", SettingTypes.normalise("STRING", "abc", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("STRING", "  ", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> SettingTypes.normalise("WAT", "1", null, null, null));
    }

    @Test void moneyRounding() {
        assertEquals(new BigDecimal("10.13"), Money.round(new BigDecimal("10.125"), "NEAREST", 2));
        assertEquals(new BigDecimal("10.12"), Money.round(new BigDecimal("10.129"), "FLOOR", 2));
        assertEquals(new BigDecimal("10.13"), Money.round(new BigDecimal("10.121"), "CEIL", 2));
        assertEquals(new BigDecimal("11"), Money.round(new BigDecimal("10.5"), "NEAREST", 0));
        assertThrows(IllegalArgumentException.class, () -> Money.round(BigDecimal.ONE, "X", 2));
        assertEquals(1234L, Money.toPaise(new BigDecimal("12.34")));
        assertEquals(new BigDecimal("12.34"), Money.fromPaise(1234));
        assertEquals(new BigDecimal("0.00"), Money.round(null, "NEAREST", 2));
    }

    @Test void auditDiffFindsChangedFields() {
        Map<String, Object> b = new HashMap<>(); b.put("name", "A"); b.put("city", "X"); b.put("gone", 1);
        Map<String, Object> a = new HashMap<>(); a.put("name", "A"); a.put("city", "Y"); a.put("added", 2);
        var ch = AuditDiff.diff(b, a);
        assertEquals(3, ch.size());
        assertTrue(ch.contains(new AuditDiff.Change("city", "X", "Y")));
        assertTrue(ch.contains(new AuditDiff.Change("gone", "1", null)));
        assertTrue(ch.contains(new AuditDiff.Change("added", null, "2")));
        assertEquals(1, AuditDiff.diff(null, Map.of("a", 1)).size());
        assertTrue(AuditDiff.diff(Map.of("a", 1), Map.of("a", 1)).isEmpty());
    }

    @Test void implicitRoles() {
        assertEquals(Set.of("EMPLOYEE"), RoleResolver.roles("USER", null));
        assertTrue(RoleResolver.roles("MANAGER", null).contains("MANAGER"));
        assertTrue(RoleResolver.roles("ADMIN", null).contains("HR_ADMIN"));
        assertTrue(RoleResolver.roles("SUPER_ADMIN", null).contains("SUPER_ADMIN"));
        assertFalse(RoleResolver.roles("ADMIN", null).contains("PAYROLL_APPROVER"));
        assertTrue(RoleResolver.roles("USER", List.of("PAYROLL_APPROVER")).contains("PAYROLL_APPROVER"));
    }
    @Test void permissionUnion() {
        var perms = Map.of("HR_ADMIN", Set.of("HR_SETUP"), "EMPLOYEE", Set.of("SELF_VIEW"));
        assertTrue(RoleResolver.allowed(Set.of("EMPLOYEE", "HR_ADMIN"), perms, "HR_SETUP"));
        assertFalse(RoleResolver.allowed(Set.of("EMPLOYEE"), perms, "HR_SETUP"));
        assertTrue(RoleResolver.allowed(Set.of("SUPER_ADMIN"), perms, "ANYTHING"));
        assertFalse(RoleResolver.allowed(Set.of("EMPLOYEE"), perms, "UNKNOWN"));
    }
    @Test void makerChecker() {
        assertFalse(RoleResolver.canApprove("E1", "E1"));
        assertTrue(RoleResolver.canApprove("E1", "E2"));
        assertFalse(RoleResolver.canApprove(null, "E2"));
    }
}
