package in.opt.sfa.tenant.dashboard.view.controller;

import in.opt.sfa.tenant.dashboard.view.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Dashboard stat-card counts. View-only (no save/edit/delete) — hence the "view" sub-package. */
@Tag(name = "Dashboard", description = "Lightweight counts for the dashboard's stat cards")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/counts")
    @Operation(summary = "Route/Area/Client/Product row counts")
    public Map<String, Long> counts() {
        return service.counts();
    }
}
