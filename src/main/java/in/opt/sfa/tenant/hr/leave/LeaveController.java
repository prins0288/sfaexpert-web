package in.opt.sfa.tenant.hr.leave;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "Leave", description = "Leave master (policy), balances, applications and approvals")
@RestController
@RequestMapping("/api/hr/leave")
public class LeaveController {

    private final LeaveService svc;

    public LeaveController(LeaveService svc) { this.svc = svc; }

    private static String s(Map<String, Object> b, String k) { Object v = b.get(k); return v == null ? null : v.toString(); }

    @GetMapping("/types") public List<Map<String, Object>> types() { return svc.types(false); }
    @PostMapping("/types") public Map<String, Object> saveType(@RequestBody Map<String, Object> b) { return svc.saveType(b); }
    @PostMapping("/types/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteTypes(@RequestBody Map<String, Object> b) { return Map.of("deleted", svc.deleteTypes(((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList())); }

    @GetMapping("/mine") public Map<String, Object> mine() { return svc.mine(); }
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam long typeId, @RequestParam String from, @RequestParam(required = false) String to, @RequestParam(defaultValue = "FULL") String part, @RequestParam(required = false) String reason) {
        LocalDate f = LocalDate.parse(from);
        return svc.preview(typeId, f, to == null || to.isBlank() ? f : LocalDate.parse(to), part, reason);
    }
    @PostMapping("/apply") public Map<String, Object> apply(@RequestBody Map<String, Object> b) { return svc.applyMine(b); }
    @GetMapping("/approvals") public List<Map<String, Object>> approvals(@RequestParam(defaultValue = "false") boolean all) { return svc.toApprove(all); }
    @PostMapping("/{id}/approve") public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { svc.decide(id, true, b == null ? null : s(b, "remarks")); return Map.of("ok", true); }
    @PostMapping("/{id}/reject") public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { svc.decide(id, false, b == null ? null : s(b, "remarks")); return Map.of("ok", true); }
    @PostMapping("/{id}/cancel") public Map<String, Object> cancel(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { svc.cancel(id, b == null ? null : s(b, "reason")); return Map.of("ok", true); }
    @GetMapping("/balances") public List<Map<String, Object>> balances(@RequestParam String empId) { return svc.balances(empId, LocalDate.now()); }
    @PostMapping("/adjust") public Map<String, Object> adjust(@RequestBody Map<String, Object> b) { svc.adjust(s(b, "empId"), Long.parseLong(s(b, "leaveTypeId")), new BigDecimal(s(b, "days")), s(b, "reason")); return Map.of("ok", true); }
}
