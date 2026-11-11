package in.opt.sfa.tenant.dcr.view.controller;

import in.opt.sfa.tenant.dcr.view.service.AttendanceReportService;
import in.opt.sfa.tenant.dcr.view.service.DcrReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Tag(name = "Attendance Report", description = "Employee x date attendance sheet judged by the attendance rules, and the rules")
@RestController
@RequestMapping("/api/report/attendance")
public class AttendanceReportController {

    private final AttendanceReportService service;
    private final DcrReportService dcr;

    public AttendanceReportController(AttendanceReportService service, DcrReportService dcr) { this.service = service; this.dcr = dcr; }

    @GetMapping("/filters") public Map<String, Object> filters() { return dcr.filters(); }

    @GetMapping("/sheet")
    public Map<String, Object> sheet(@RequestParam(required = false) String month,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long hqId, @RequestParam(required = false) Integer designationId,
                                     @RequestParam(required = false) String empId, @RequestParam(required = false) String empStatus) {
        if (month != null && !month.isBlank()) { YearMonth ym = YearMonth.parse(month.trim()); from = ym.atDay(1); to = ym.atEndOfMonth(); }
        return service.sheet(new DcrReportService.Filter(from, to, stateId, hqId, designationId, empId, null, null, true), empStatus);
    }

    @GetMapping("/rules") public Map<String, Object> rules() { return service.rules(); }
    @PostMapping("/rules") public Map<String, Object> save(@RequestBody Map<String, Object> b) { return service.saveRule(b); }
    @PostMapping("/rules/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> b) { return Map.of("deleted", service.deleteRules(((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList())); }
}
