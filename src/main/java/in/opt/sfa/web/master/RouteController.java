package in.opt.sfa.web.master;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.entity.Route;
import in.opt.sfa.tenant.repository.HqRepository;
import in.opt.sfa.tenant.repository.RouteRepository;
import in.opt.sfa.tenant.repository.StateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Route master — list / get / save / add-multiple / bulk-upload / soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/route")
public class RouteController {

    private final RouteRepository routes;
    private final HqRepository hqs;
    private final StateRepository states;

    public RouteController(RouteRepository routes, HqRepository hqs, StateRepository states) {
        this.routes = routes;
        this.hqs = hqs;
        this.states = states;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Route> list() {
        return routes.findAllByOrderByOidAsc();
    }

    @GetMapping("/{oid}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Route get(@PathVariable Long oid) {
        return routes.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Route not found: " + oid));
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Route save(@RequestBody Route form) {
        if (form.getStatus() == null || form.getStatus().isBlank()) form.setStatus("Y");
        return routes.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(@RequestBody List<Route> forms) {
        int saved = 0, skipped = 0;
        for (Route f : forms) {
            if (isBlank(f.getRouteCode()) || isBlank(f.getRouteName())
                    || routes.findByRouteCode(f.getRouteCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setRouteCode(f.getRouteCode().trim());
            f.setRouteName(f.getRouteName().trim());
            f.setStatus("Y");
            routes.save(f);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Route status(@PathVariable Long oid, @RequestParam String status) {
        Route r = routes.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Route not found: " + oid));
        r.setStatus(status);
        return routes.save(r);
    }

    /** Downloadable Excel template. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        byte[] xlsx = ExcelUtil.template("Route",
                List.of("route_code", "route_name", "hq", "state", "distance_km", "description"),
                List.of("RT-XXX-01", "Sample Beat", "HQ-MUM", "MH", "10", "notes"));
        return ExcelUtil.xlsxResponse("route_template.xlsx", xlsx);
    }

    /** Bulk upload = upsert by route_code; hq/state resolved by code or name. */
    @PostMapping("/upload")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("route_code"), name = row.get("route_name");
            if (isBlank(code) || isBlank(name)) { skipped++; continue; }
            Route r = routes.findByRouteCode(code.trim()).orElseGet(Route::new);
            boolean isNew = r.getOid() == null;
            r.setRouteCode(code.trim());
            r.setRouteName(name.trim());
            r.setHqOid(resolveHq(row.get("hq")));
            r.setStateOid(resolveState(row.get("state")));
            r.setDistanceKm(toDecimal(row.get("distance_km")));
            r.setDescription(row.get("description"));
            r.setStatus("Y");
            routes.save(r);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    // --- helpers ---
    private Long resolveHq(String v) {
        return isBlank(v) ? null : hqs.findFirstByHqCodeOrHqName(v.trim(), v.trim()).map(h -> h.getOid()).orElse(null);
    }
    private Long resolveState(String v) {
        return isBlank(v) ? null : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }
    static boolean isBlank(String s) { return s == null || s.isBlank(); }
    static BigDecimal toDecimal(String s) {
        try { return isBlank(s) ? null : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
