package in.opt.sfa.tenant.dcr.view.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.dcr.view.service.DcrReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * DCR report from the 2026 DCR tables: day-wise summary (with absent / holiday / weekly-off days), a complete single day, and an Excel export.
 * View-only. A user sees his own, a manager his team's, an admin everyone's.
 */
@Tag(name = "DCR Report", description = "Day-wise DCR summary, absent days, one-day detail, Excel")
@RestController
@RequestMapping("/api/report/dcr")
public class DcrReportController {

    private final DcrReportService service;

    public DcrReportController(DcrReportService service) {
        this.service = service;
    }

    private static DcrReportService.Filter filter(String month, LocalDate from, LocalDate to, Long stateId, Long hqId, Integer designationId, String empId, Integer status, Integer source, boolean absent) {
        if (month != null && !month.isBlank()) {   // monthly: yyyy-MM
            YearMonth ym = YearMonth.parse(month.trim());
            from = ym.atDay(1); to = ym.atEndOfMonth();
        }
        return new DcrReportService.Filter(from, to, stateId, hqId, designationId, empId, status, source, absent);
    }

    @GetMapping("/filters")
    @Operation(summary = "States, HQs, designations and employees (in the user's scope) for the filters")
    public Map<String, Object> filters() { return service.filters(); }

    @GetMapping("/summary")
    @Operation(summary = "One row per employee per day (DCR, or Absent / Holiday / Weekly off) with totals; monthly (month=yyyy-MM) or from / to")
    public Map<String, Object> summary(@RequestParam(required = false) String month,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long hqId, @RequestParam(required = false) Integer designationId,
                                       @RequestParam(required = false) String empId, @RequestParam(required = false) Integer status, @RequestParam(required = false) Integer source,
                                       @RequestParam(defaultValue = "true") boolean absent) {
        return service.summary(filter(month, from, to, stateId, hqId, designationId, empId, status, source, absent));
    }

    @GetMapping("/day/{dcrId}")
    @Operation(summary = "One DCR complete: day facts, every call with items / leaflets / treatment / joint work, expenses, day end, approval")
    public Map<String, Object> day(@PathVariable Long dcrId) { return service.day(dcrId); }

    @GetMapping("/export")
    @Operation(summary = "Excel of the same filter: DCR Summary, Calls and Expenses sheets")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String month,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long hqId, @RequestParam(required = false) Integer designationId,
                                         @RequestParam(required = false) String empId, @RequestParam(required = false) Integer status, @RequestParam(required = false) Integer source,
                                         @RequestParam(defaultValue = "true") boolean absent) throws IOException {
        DcrReportService.Filter f = filter(month, from, to, stateId, hqId, designationId, empId, status, source, absent);
        String name = "DCR-report-" + (f.from() == null ? "" : f.from()) + "-to-" + (f.to() == null ? "" : f.to()) + ".xlsx";
        return ExcelUtil.xlsxResponse(name, service.export(f));
    }
}
