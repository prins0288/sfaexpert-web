package in.opt.sfa.tenant.report.activity.controller;

import in.opt.sfa.tenant.report.activity.dto.ActivityRow;
import in.opt.sfa.tenant.report.activity.dto.ActivitySaveRequest;
import in.opt.sfa.tenant.report.activity.service.ReportActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Report Activity", description = "Per-user, per-report search history — record + replay past filter selections")
@RestController
@RequestMapping("/api/report-activity")
public class ReportActivityController {

    private final ReportActivityService service;

    public ReportActivityController(ReportActivityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record one search (filters is an opaque JSON string). Repeating the exact " +
            "same filter selection for the same report UPDATES that existing row's timestamp instead " +
            "of inserting a duplicate — it just bumps back to the top of the recent list.")
    public void save(@RequestBody ActivitySaveRequest req) {
        service.save(req);
    }

    @GetMapping
    @Operation(summary = "The CALLER's own last 10 searches for one report — never another user's")
    public List<ActivityRow> list(@RequestParam String reportKey) {
        return service.list(reportKey);
    }
}
