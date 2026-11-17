package in.opt.sfa.tenant.expense;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Expense", description = "Monthly expense statement built from the DCR: user submits, manager approves, admin approves; amounts can be changed with a logged history")
@RestController
@RequestMapping("/api/expense")
public class ExpenseController {
    private final ExpenseService service;
    public ExpenseController(ExpenseService service) { this.service = service; }

    @GetMapping("/setup") public Map<String, Object> setup() { return service.setup(); }
    @PostMapping("/setup/settings") public Map<String, Object> settings(@RequestBody Map<String, Object> b) { service.saveSettings(b); return Map.of("saved", true); }
    @PostMapping("/setup/da") public Map<String, Object> da(@RequestBody Map<String, Object> b) { service.saveDa(b); return Map.of("saved", true); }
    @PostMapping("/setup/fare") public Map<String, Object> fare(@RequestBody Map<String, Object> b) { service.saveFare(b); return Map.of("saved", true); }
    @PostMapping("/setup/{kind}/{id}/delete") public Map<String, Object> deleteRate(@PathVariable String kind, @PathVariable long id) { service.deleteRate(kind, id); return Map.of("deleted", true); }

    @GetMapping("/emp-rates") public List<Map<String, Object>> empRates(@RequestParam String empId) { return service.empRates(empId); }
    @PostMapping("/emp-rates")
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveEmpRates(@RequestBody Map<String, Object> b) { service.saveEmpRates(String.valueOf(b.get("empId")), (List<Map<String, Object>>) b.getOrDefault("rates", List.of())); return Map.of("saved", true); }

    @GetMapping("/list")
    public List<Map<String, Object>> list(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year, @RequestParam(required = false) String status, @RequestParam(required = false) String empId) { return service.list(month, year, status, empId); }
    @GetMapping("/find") public Map<String, Object> find(@RequestParam String empId, @RequestParam int month, @RequestParam int year) throws Exception { return service.find(empId, month, year); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable long id) throws Exception { return service.get(id); }
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> b) throws Exception { return service.generate(String.valueOf(b.get("empId")), Integer.parseInt(String.valueOf(b.get("month"))), Integer.parseInt(String.valueOf(b.get("year"))), Boolean.TRUE.equals(b.get("refresh"))); }
    @PostMapping("/day/{dayId}") public Map<String, Object> day(@PathVariable long dayId, @RequestBody Map<String, Object> b) throws Exception { return service.changeDay(dayId, b); }
    @GetMapping("/{id}/history") public List<Map<String, Object>> history(@PathVariable long id, @RequestParam(required = false) Long dayId) { return service.history(id, dayId); }
    @PostMapping("/{id}/submit") public Map<String, Object> submit(@PathVariable long id) { return service.submit(id); }
    @PostMapping("/{id}/approve") public Map<String, Object> approve(@PathVariable long id) { return service.approve(id); }
    @PostMapping("/{id}/reject") public Map<String, Object> reject(@PathVariable long id, @RequestBody Map<String, Object> b) { return service.reject(id, b.get("reason") == null ? null : b.get("reason").toString()); }
}
