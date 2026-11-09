package in.opt.sfa.tenant.dcr.view.controller;

import in.opt.sfa.tenant.dcr.view.repository.DcrReportRow;
import in.opt.sfa.tenant.dcr.view.service.DcrReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Day-wise DCR report over dcr_parent_summary / dcr_client_summary /
 * dcr_unlisted_client_summary: one row per call. Filters: Division (the employee's), Area,
 * Employee, date range. Own / team / all by role (TeamScope).
 *
 * View-only (no save/edit/delete) — hence the "view" sub-package.
 */
@Tag(name = "DCR Report", description = "Day-wise DCR calls (listed + unlisted)")
@RestController
@RequestMapping("/api/report/dcr")
public class DcrReportController {

    private final DcrReportService service;

    public DcrReportController(DcrReportService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Filtered DCR report rows")
    public List<DcrReportRow> report(
            @RequestParam(required = false) Long divisionOid,
            @RequestParam(required = false) Long areaOid,
            @RequestParam(required = false) String empId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.report(divisionOid, areaOid, empId, fromDate, toDate);
    }
}
