package in.opt.sfa.tenant.hr.payroll;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "Payroll", description = "Salary heads, employee salary, payroll run, payslips, salary hold, slip design")
@RestController
@RequestMapping("/api/hr")
public class PayrollController {

    private final SalaryService salary;
    private final PayrollService payroll;
    private final HoldService holds;

    public PayrollController(SalaryService salary, PayrollService payroll, HoldService holds) { this.salary = salary; this.payroll = payroll; this.holds = holds; }

    private static String s(Map<String, Object> b, String k) { Object v = b == null ? null : b.get(k); return v == null ? null : v.toString(); }
    @SuppressWarnings("unchecked") private static List<Long> ids(Map<String, Object> b) { return ((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList(); }

    // heads
    @GetMapping("/salary/heads") public List<Map<String, Object>> heads() { return salary.heads(false); }
    @PostMapping("/salary/heads") public Map<String, Object> saveHead(@RequestBody Map<String, Object> b) { return salary.saveHead(b); }
    @PostMapping("/salary/heads/delete") public Map<String, Object> deleteHeads(@RequestBody Map<String, Object> b) { return Map.of("deleted", salary.deleteHeads(ids(b))); }
    // employee salary
    @GetMapping("/salary/employees") public List<Map<String, Object>> employees(@RequestParam(required = false) String q) { return salary.employees(q); }
    @GetMapping("/salary/employees/{empId}") public Map<String, Object> salaryOf(@PathVariable String empId) { return salary.salaryOf(empId); }
    @SuppressWarnings("unchecked")
    @PostMapping("/salary/employees/{empId}/preview")
    public Map<String, Object> preview(@PathVariable String empId, @RequestBody Map<String, Object> b) {
        return salary.preview(empId, new BigDecimal(s(b, "gross")), (List<Map<String, Object>>) b.getOrDefault("values", List.of()), s(b, "effectiveFrom") == null ? LocalDate.now() : LocalDate.parse(s(b, "effectiveFrom")));
    }
    @SuppressWarnings("unchecked")
    @PostMapping("/salary/employees/{empId}")
    public Map<String, Object> saveSalary(@PathVariable String empId, @RequestBody Map<String, Object> b) {
        return salary.saveSalary(empId, LocalDate.parse(s(b, "effectiveFrom")), new BigDecimal(s(b, "gross")), (List<Map<String, Object>>) b.getOrDefault("values", List.of()), s(b, "reason"));
    }
    // payroll
    @GetMapping("/payroll/runs") public List<Map<String, Object>> runs() { return payroll.runs(); }
    @GetMapping("/payroll/runs/{id}") public Map<String, Object> run(@PathVariable long id) { return payroll.run(id); }
    @PostMapping("/payroll/generate") public Map<String, Object> generate(@RequestBody Map<String, Object> b) { return payroll.generate(Integer.parseInt(s(b, "year")), Integer.parseInt(s(b, "month"))); }
    @PostMapping("/payroll/runs/{id}/approve") public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { payroll.approve(id, s(b, "reason")); return Map.of("ok", true); }
    @PostMapping("/payroll/runs/{id}/paid") public Map<String, Object> paid(@PathVariable long id) { payroll.markPaid(id); return Map.of("ok", true); }
    @PostMapping("/payroll/runs/{id}/delete") public Map<String, Object> deleteRun(@PathVariable long id) { payroll.deleteRun(id); return Map.of("ok", true); }
    @GetMapping("/payroll/slips/{id}") public Map<String, Object> slip(@PathVariable long id) { return payroll.slip(id); }
    @GetMapping("/payroll/mine") public List<Map<String, Object>> mine() { return payroll.mySlips(); }
    @PostMapping("/payroll/slips/{id}/lop") public Map<String, Object> lop(@PathVariable long id, @RequestBody Map<String, Object> b) { return payroll.setLop(id, new BigDecimal(s(b, "lopDays")), s(b, "reason")); }
    @PostMapping("/payroll/slips/{id}/delete") public Map<String, Object> delSlip(@PathVariable long id, @RequestBody Map<String, Object> b) { payroll.deleteSlip(id, s(b, "reason")); return Map.of("ok", true); }
    @PostMapping("/payroll/slips/{id}/hold") public Map<String, Object> hold(@PathVariable long id, @RequestBody Map<String, Object> b) { payroll.holdSlip(id, s(b, "reason")); return Map.of("ok", true); }
    @PostMapping("/payroll/slips/{id}/release") public Map<String, Object> release(@PathVariable long id, @RequestBody Map<String, Object> b) { payroll.releaseSlip(id, s(b, "reason")); return Map.of("ok", true); }
    // hold rules
    @GetMapping("/payroll/holds/config") public Map<String, Object> holdConfig() { return holds.config(); }
    @GetMapping("/payroll/holds") public List<Map<String, Object>> holdList() { return holds.list(); }
    @PostMapping("/payroll/holds") public Map<String, Object> holdCreate(@RequestBody Map<String, Object> b) { return holds.create(b); }
    @PostMapping("/payroll/holds/{id}/release") public Map<String, Object> holdRelease(@PathVariable long id, @RequestBody Map<String, Object> b) { holds.release(id, s(b, "reason")); return Map.of("ok", true); }
    // slip design
    @GetMapping("/payroll/slip-design") public Map<String, Object> design() { return payroll.slipConfig(); }
    @PostMapping("/payroll/slip-design") public Map<String, Object> saveDesign(@RequestBody Map<String, Object> b) { payroll.saveSlipConfig(b); return Map.of("ok", true); }
}
