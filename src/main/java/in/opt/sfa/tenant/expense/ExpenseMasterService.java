package in.opt.sfa.tenant.expense;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Expense types: FIXED (a monthly amount given to the employees - mobile bill, internet bill ...) and VARIABLE (entered when it happens, with the bill - courier, birthday ...;
 * these are the only types the DCR expense check-in and the Daily Expense feed offer). Admin: the master and the assignment of the fixed expenses. Everyone: his own daily entries.
 */
@Service
public class ExpenseMasterService {
    private final JdbcTemplate jdbc;
    private final TeamScope scope;

    public ExpenseMasterService(JdbcTemplate jdbc, TeamScope scope) { this.jdbc = jdbc; this.scope = scope; }

    private static boolean flag(Object o) { return Boolean.TRUE.equals(o) || "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o)); }

    // ------------------------------------------------------------------ the master
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> types() {
        Authz.requireRole("ADMIN");
        return jdbc.queryForList("SELECT id, expense_code AS code, expense_name AS name, exp_kind AS kind, is_travel AS travel, bill_required AS billRequired, sort_order AS sortOrder, is_active AS active FROM expense_master ORDER BY exp_kind, sort_order, id");
    }

    /** The variable types a user can feed (DCR expense and Daily Expense). */
    public List<Map<String, Object>> variableTypes() {
        return jdbc.queryForList("SELECT id, expense_name AS name, is_travel AS travel, bill_required AS billRequired FROM expense_master WHERE is_active = 1 AND exp_kind = 'VARIABLE' ORDER BY sort_order, expense_name");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveType(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        Long id = b.get("id") == null || b.get("id").toString().isBlank() ? null : Long.valueOf(b.get("id").toString());
        String code = String.valueOf(b.get("code")).trim().toUpperCase(), name = b.get("name") == null ? "" : b.get("name").toString().trim(), kind = String.valueOf(b.get("kind"));
        if (!code.matches("[A-Z][A-Z0-9_]{1,29}")) throw new IllegalArgumentException("Code: 2-30 capital letters / digits / _ (e.g. MOBILE)");
        if (name.isEmpty() || name.length() > 80) throw new IllegalArgumentException("Name is required (up to 80 characters)");
        if (!List.of("FIXED", "VARIABLE").contains(kind)) throw new IllegalArgumentException("Kind must be FIXED or VARIABLE");
        boolean travel = flag(b.get("travel")) && "VARIABLE".equals(kind), bill = flag(b.get("billRequired")) && "VARIABLE".equals(kind), active = !Boolean.FALSE.equals(b.get("active")) && !"0".equals(String.valueOf(b.get("active")));
        int sort = b.get("sortOrder") == null ? 100 : Integer.parseInt(b.get("sortOrder").toString());
        if (id == null) jdbc.update("INSERT INTO expense_master (expense_code, expense_name, exp_kind, is_travel, bill_required, sort_order, is_active) VALUES (?,?,?,?,?,?,?)", code, name, kind, travel ? 1 : 0, bill ? 1 : 0, sort, active ? 1 : 0);
        else {
            String old = jdbc.queryForList("SELECT exp_kind FROM expense_master WHERE id = ?", String.class, id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Type not found"));
            if (!old.equals(kind) && (jdbc.queryForObject("SELECT COUNT(*) FROM dcr_expense WHERE expense_id = ?", Integer.class, id) + jdbc.queryForObject("SELECT COUNT(*) FROM expense_entry WHERE expense_id = ?", Integer.class, id) + jdbc.queryForObject("SELECT COUNT(*) FROM expense_fixed_assign WHERE expense_id = ?", Integer.class, id)) > 0)
                throw new IllegalStateException("This type is already used - it cannot change between fixed and variable (switch it off and add a new one)");
            jdbc.update("UPDATE expense_master SET expense_code = ?, expense_name = ?, exp_kind = ?, is_travel = ?, bill_required = ?, sort_order = ?, is_active = ? WHERE id = ?", code, name, kind, travel ? 1 : 0, bill ? 1 : 0, sort, active ? 1 : 0, id);
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteType(long id) {
        Authz.requireRole("ADMIN");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM dcr_expense WHERE expense_id = ?", Integer.class, id) + jdbc.queryForObject("SELECT COUNT(*) FROM expense_entry WHERE expense_id = ?", Integer.class, id) + jdbc.queryForObject("SELECT COUNT(*) FROM expense_fixed_assign WHERE expense_id = ?", Integer.class, id) > 0)
            throw new IllegalStateException("This type is used - switch it off instead of deleting it");
        jdbc.update("DELETE FROM expense_master WHERE id = ?", id);
    }

    // ------------------------------------------------------------------ fixed expense: assign to the employees
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> fixedList(Long expenseId, String empId) {
        Authz.requireRole("ADMIN");
        StringBuilder sql = new StringBuilder("SELECT a.id, a.expense_id AS expenseId, m.expense_name AS expense, e.emp_code AS empCode, e.emp_name AS empName, d.designation, a.amount, a.effective_from AS effectiveFrom, a.effective_to AS effectiveTo FROM expense_fixed_assign a JOIN expense_master m ON m.id = a.expense_id JOIN emp_detail e ON e.emp_id = a.emp_id LEFT JOIN designation_master d ON d.id = e.designation WHERE 1 = 1");
        List<Object> a = new ArrayList<>();
        if (expenseId != null) { sql.append(" AND a.expense_id = ?"); a.add(expenseId); }
        if (empId != null && !empId.isBlank()) { sql.append(" AND a.emp_id = ?"); a.add(empId); }
        sql.append(" ORDER BY m.expense_name, e.emp_name, a.effective_from DESC");
        return jdbc.queryForList(sql.toString(), a.toArray());
    }

    /** Give a fixed expense to the chosen employees, or to everyone / a state / HQ / designation. The same employee + expense + start date is updated, never duplicated. */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> assign(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        long expenseId = Long.parseLong(String.valueOf(b.get("expenseId")));
        if (jdbc.queryForObject("SELECT COUNT(*) FROM expense_master WHERE id = ? AND exp_kind = 'FIXED' AND is_active = 1", Integer.class, expenseId) == 0) throw new IllegalArgumentException("Choose a fixed expense");
        BigDecimal amount; try { amount = new BigDecimal(String.valueOf(b.get("amount")).trim()).setScale(2, RoundingMode.HALF_UP); } catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be a number"); }
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        LocalDate from = b.get("effectiveFrom") == null || b.get("effectiveFrom").toString().isBlank() ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(b.get("effectiveFrom").toString());
        Set<String> emps = new LinkedHashSet<>();
        if (b.get("empIds") instanceof List<?> l && !l.isEmpty()) l.forEach(o -> emps.add(o.toString()));
        else {
            StringBuilder sql = new StringBuilder("SELECT emp_id FROM emp_detail WHERE is_active = 1 AND COALESCE(is_vacant, 0) = 0"); List<Object> a = new ArrayList<>();
            if (b.get("stateId") != null && !b.get("stateId").toString().isBlank()) { sql.append(" AND state_id = ?"); a.add(Long.valueOf(b.get("stateId").toString())); }
            if (b.get("hqId") != null && !b.get("hqId").toString().isBlank()) { sql.append(" AND hq_id = ?"); a.add(Long.valueOf(b.get("hqId").toString())); }
            if (b.get("designationId") != null && !b.get("designationId").toString().isBlank()) { sql.append(" AND designation = ?"); a.add(Integer.valueOf(b.get("designationId").toString())); }
            if (!flag(b.get("all")) && a.isEmpty()) throw new IllegalArgumentException("Choose the employees, or All / a state / HQ / designation");
            emps.addAll(jdbc.queryForList(sql.toString(), String.class, a.toArray()));
        }
        if (emps.isEmpty()) throw new IllegalArgumentException("No employee found for this choice");
        int created = 0, updated = 0;
        for (String e : emps) {
            int n = jdbc.update("INSERT INTO expense_fixed_assign (emp_id, expense_id, amount, effective_from, created_by) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE amount = VALUES(amount), effective_to = NULL", e, expenseId, amount, from, TeamScope.me());
            if (n == 1) created++; else updated++;
        }
        return Map.of("created", created, "updated", updated);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void updateFixed(long id, Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        BigDecimal amount; try { amount = new BigDecimal(String.valueOf(b.get("amount")).trim()).setScale(2, RoundingMode.HALF_UP); } catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be a number"); }
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        LocalDate to = b.get("effectiveTo") == null || b.get("effectiveTo").toString().isBlank() ? null : LocalDate.parse(b.get("effectiveTo").toString());
        jdbc.update("UPDATE expense_fixed_assign SET amount = ?, effective_to = ? WHERE id = ?", amount, to, id);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteFixed(long id) { Authz.requireRole("ADMIN"); jdbc.update("DELETE FROM expense_fixed_assign WHERE id = ?", id); }

    // ------------------------------------------------------------------ daily feed (variable expense with the bill)
    private void checkEmp(String empId) { if (!empId.equals(TeamScope.me())) scope.require(TeamScope.Scope.TEAM_SELF, empId, "This employee is not in your team"); }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> entries(String empId, int month, int year) {
        checkEmp(empId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", jdbc.queryForList("SELECT x.id, x.exp_date AS date, x.expense_id AS expenseId, m.expense_name AS expense, x.amount, x.description, x.file_name AS fileName, x.file_path AS filePath FROM expense_entry x JOIN expense_master m ON m.id = x.expense_id WHERE x.emp_id = ? AND MONTH(x.exp_date) = ? AND YEAR(x.exp_date) = ? ORDER BY x.exp_date, x.id", empId, month, year));
        out.put("types", variableTypes()); out.put("locked", locked(empId, month, year)); out.put("canFeed", empId.equals(TeamScope.me()) || TeamScope.isAdmin());
        return out;
    }

    /** The statement of the month is submitted (or beyond): the entries of the month are frozen. */
    private boolean locked(String empId, int month, int year) {
        String st = jdbc.queryForList("SELECT status FROM expense_statement WHERE emp_id = ? AND exp_month = ? AND exp_year = ?", String.class, empId, month, year).stream().findFirst().orElse(null);
        return st != null && !List.of("DRAFT", "REJECTED").contains(st);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveEntry(Map<String, Object> b) {
        String empId = String.valueOf(b.get("empId"));
        if (!empId.equals(TeamScope.me()) && !TeamScope.isAdmin()) throw new ForbiddenException("A user feeds his own expense; only an admin can feed it for another employee");
        LocalDate d = LocalDate.parse(String.valueOf(b.get("date")));
        if (d.isAfter(LocalDate.now())) throw new IllegalArgumentException("A future date cannot be fed");
        if (locked(empId, d.getMonthValue(), d.getYear())) throw new IllegalStateException("The expense statement of this month is already submitted - the entries are closed");
        long expenseId = Long.parseLong(String.valueOf(b.get("expenseId")));
        List<Map<String, Object>> t = jdbc.queryForList("SELECT bill_required FROM expense_master WHERE id = ? AND exp_kind = 'VARIABLE' AND is_active = 1", expenseId);
        if (t.isEmpty()) throw new IllegalArgumentException("Choose a variable expense type");
        BigDecimal amount; try { amount = new BigDecimal(String.valueOf(b.get("amount")).trim()).setScale(2, RoundingMode.HALF_UP); } catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be a number"); }
        if (amount.signum() <= 0) throw new IllegalArgumentException("Enter the amount");
        String path = b.get("filePath") == null || b.get("filePath").toString().isBlank() ? null : b.get("filePath").toString();
        if (flag(t.get(0).get("bill_required")) && path == null) throw new IllegalArgumentException("The bill / invoice file is required for this expense");
        String desc = b.get("description") == null || b.get("description").toString().isBlank() ? null : b.get("description").toString().trim();
        if (desc != null && desc.length() > 150) desc = desc.substring(0, 150);
        Long id = b.get("id") == null || b.get("id").toString().isBlank() ? null : Long.valueOf(b.get("id").toString());
        if (id == null) jdbc.update("INSERT INTO expense_entry (emp_id, exp_date, expense_id, amount, description, file_name, file_type, file_path) VALUES (?,?,?,?,?,?,?,?)", empId, d, expenseId, amount, desc, str(b.get("fileName")), str(b.get("fileType")), path);
        else {
            if (jdbc.queryForList("SELECT emp_id FROM expense_entry WHERE id = ?", String.class, id).stream().noneMatch(empId::equals)) throw new IllegalArgumentException("Entry not found");
            jdbc.update("UPDATE expense_entry SET exp_date = ?, expense_id = ?, amount = ?, description = ?, file_name = ?, file_type = ?, file_path = ?, updated_at = NOW() WHERE id = ?", d, expenseId, amount, desc, str(b.get("fileName")), str(b.get("fileType")), path, id);
        }
        return Map.of("saved", true);
    }
    private static String str(Object o) { return o == null || o.toString().isBlank() ? null : o.toString(); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteEntry(long id) {
        Map<String, Object> e = jdbc.queryForList("SELECT emp_id, exp_date FROM expense_entry WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Entry not found"));
        String empId = (String) e.get("emp_id"); LocalDate d = ((java.sql.Date) e.get("exp_date")).toLocalDate();
        if (!empId.equals(TeamScope.me()) && !TeamScope.isAdmin()) throw new ForbiddenException("You can delete only your own entries");
        if (locked(empId, d.getMonthValue(), d.getYear())) throw new IllegalStateException("The expense statement of this month is already submitted - the entries are closed");
        jdbc.update("DELETE FROM expense_entry WHERE id = ?", id);
    }
}
