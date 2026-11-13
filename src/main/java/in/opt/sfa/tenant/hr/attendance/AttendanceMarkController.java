package in.opt.sfa.tenant.hr.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Mark attendance / leave", description = "Admin marks attendance or leave for several employees")
@RestController
@RequestMapping("/api/hr/mark")
public class AttendanceMarkController {

    private final AttendanceMarkService svc;

    public AttendanceMarkController(AttendanceMarkService svc) { this.svc = svc; }

    @GetMapping("/worktypes") public List<Map<String, Object>> workTypes() { return svc.workTypes(); }
    @GetMapping("/employees") public List<Map<String, Object>> employees(@RequestParam(required = false) String q) { return svc.employees(q); }
    @PostMapping public Map<String, Object> mark(@RequestBody Map<String, Object> body) { return svc.mark(body); }
}
