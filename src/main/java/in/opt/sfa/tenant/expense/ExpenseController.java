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
    private final ExpenseMasterService master;
    private final in.opt.sfa.storage.FileUploadService uploads;
    public ExpenseController(ExpenseService service, ExpenseMasterService master, in.opt.sfa.storage.FileUploadService uploads) { this.service = service; this.master = master; this.uploads = uploads; }

    @GetMapping("/setup") public Map<String, Object> setup() { return service.setup(); }
    @PostMapping("/setup/settings") public Map<String, Object> settings(@RequestBody Map<String, Object> b) { service.saveSettings(b); return Map.of("saved", true); }
    @PostMapping("/setup/da") public Map<String, Object> da(@RequestBody Map<String, Object> b) { service.saveDa(b); return Map.of("saved", true); }
    @PostMapping("/setup/fare") public Map<String, Object> fare(@RequestBody Map<String, Object> b) { service.saveFare(b); return Map.of("saved", true); }
    @PostMapping("/setup/{kind}/{id}/delete") public Map<String, Object> deleteRate(@PathVariable String kind, @PathVariable long id) { service.deleteRate(kind, id); return Map.of("deleted", true); }

    // ---- expense master (fixed / variable), fixed assignment, daily feed
    @GetMapping("/types") public List<Map<String, Object>> types() { return master.types(); }
    @GetMapping("/types/variable") public List<Map<String, Object>> variableTypes() { return master.variableTypes(); }
    @PostMapping("/types") public Map<String, Object> saveType(@RequestBody Map<String, Object> b) { master.saveType(b); return Map.of("saved", true); }
    @PostMapping("/types/{id}/delete") public Map<String, Object> deleteType(@PathVariable long id) { master.deleteType(id); return Map.of("deleted", true); }
    @GetMapping("/fixed") public List<Map<String, Object>> fixedList(@RequestParam(required = false) Long expenseId, @RequestParam(required = false) String empId) { return master.fixedList(expenseId, empId); }
    @PostMapping("/fixed/assign") public Map<String, Object> assign(@RequestBody Map<String, Object> b) { return master.assign(b); }
    @PostMapping("/fixed/{id}") public Map<String, Object> updateFixed(@PathVariable long id, @RequestBody Map<String, Object> b) { master.updateFixed(id, b); return Map.of("saved", true); }
    @PostMapping("/fixed/{id}/delete") public Map<String, Object> deleteFixed(@PathVariable long id) { master.deleteFixed(id); return Map.of("deleted", true); }
    @PostMapping("/fixed-line/{id}") public Map<String, Object> fixedLine(@PathVariable long id, @RequestBody Map<String, Object> b) { return service.changeFixed(id, b); }
    @GetMapping("/var-assign") public List<Map<String, Object>> varList(@RequestParam(required = false) Long expenseId) { return master.varList(expenseId); }
    @PostMapping("/var-assign") public Map<String, Object> varAssign(@RequestBody Map<String, Object> b) { return master.varAssign(b); }
    @PostMapping("/var-assign/{id}/delete") public Map<String, Object> varDelete(@PathVariable long id) { master.varDelete(id); return Map.of("deleted", true); }
    @GetMapping("/day-started") public Map<String, Object> dayStarted(@RequestParam String empId, @RequestParam String date) { return master.dayStarted(empId, date); }
    @GetMapping("/entries") public Map<String, Object> entries(@RequestParam String empId, @RequestParam int month, @RequestParam int year) { return master.entries(empId, month, year); }
    @PostMapping("/entry") public Map<String, Object> saveEntry(@RequestBody Map<String, Object> b) { return master.saveEntry(b); }
    @PostMapping("/entry/{id}/delete") public Map<String, Object> deleteEntry(@PathVariable long id) { master.deleteEntry(id); return Map.of("deleted", true); }
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        in.opt.sfa.storage.StoredFile f = uploads.upload("expense", file, in.opt.sfa.storage.FileUploadService.Kind.DOCUMENT, true);
        Map<String, Object> m = new java.util.LinkedHashMap<>(); m.put("path", f.path()); m.put("name", f.originalName()); m.put("type", f.type()); return m;
    }

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
    @GetMapping("/day/{dayId}/bills") public List<Map<String, Object>> bills(@PathVariable long dayId) { return service.bills(dayId); }
    @GetMapping("/{id}/stay-areas") public List<Map<String, Object>> stayAreas(@PathVariable long id) { return service.stayAreas(id); }
    @GetMapping("/{id}/history") public List<Map<String, Object>> history(@PathVariable long id, @RequestParam(required = false) Long dayId) { return service.history(id, dayId); }
    @PostMapping("/{id}/submit") public Map<String, Object> submit(@PathVariable long id) { return service.submit(id); }
    @PostMapping("/{id}/approve") public Map<String, Object> approve(@PathVariable long id) { return service.approve(id); }
    @PostMapping("/{id}/reject") public Map<String, Object> reject(@PathVariable long id, @RequestBody Map<String, Object> b) { return service.reject(id, b.get("reason") == null ? null : b.get("reason").toString()); }
}
