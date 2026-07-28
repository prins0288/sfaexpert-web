package in.opt.sfa.web.master;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.entity.Area;
import in.opt.sfa.tenant.entity.RouteAreaMap;
import in.opt.sfa.tenant.repository.AreaRepository;
import in.opt.sfa.tenant.repository.HqRepository;
import in.opt.sfa.tenant.repository.RouteAreaMapRepository;
import in.opt.sfa.tenant.repository.StateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Area master — list / get / save / add-multiple / bulk-upload / soft-delete. Tenant-routed. */
@Tag(name = "Area Master", description = "Areas — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/area")
public class AreaController {

    private final AreaRepository areas;
    private final RouteAreaMapRepository maps;
    private final HqRepository hqs;
    private final StateRepository states;

    public AreaController(AreaRepository areas, RouteAreaMapRepository maps,
                          HqRepository hqs, StateRepository states) {
        this.areas = areas;
        this.maps = maps;
        this.hqs = hqs;
        this.states = states;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Area> list() {
        List<Area> rows = areas.findAllByOrderByOidAsc();
        for (Area a : rows) {
            a.setRoutes(maps.findByAreaOidAndStatus(a.getOid(), "Y").stream()
                    .map(RouteAreaMap::getRouteName)
                    .collect(Collectors.joining(", ")));
        }
        return rows;
    }

    @GetMapping("/{oid}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Area get(@PathVariable Long oid) {
        return areas.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Area not found: " + oid));
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Area save(@RequestBody Area form) {
        if (form.getStatus() == null || form.getStatus().isBlank()) form.setStatus("Y");
        return areas.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(@RequestBody List<Area> forms) {
        int saved = 0, skipped = 0;
        for (Area f : forms) {
            if (RouteController.isBlank(f.getAreaCode()) || RouteController.isBlank(f.getAreaName())
                    || areas.findByAreaCode(f.getAreaCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setAreaCode(f.getAreaCode().trim());
            f.setAreaName(f.getAreaName().trim());
            f.setStatus("Y");
            areas.save(f);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Area status(@PathVariable Long oid, @RequestParam String status) {
        Area a = areas.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Area not found: " + oid));
        a.setStatus(status);
        return areas.save(a);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        byte[] xlsx = ExcelUtil.template("Area",
                List.of("area_code", "area_name", "city", "hq", "state", "pincode", "area_type", "distance_km"),
                List.of("AR-XXX", "Sample Area", "Mumbai", "HQ-MUM", "MH", "400001", "Core", "5"));
        return ExcelUtil.xlsxResponse("area_template.xlsx", xlsx);
    }

    /** Bulk upload = upsert by area_code; hq/state resolved by code or name. */
    @PostMapping("/upload")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("area_code"), name = row.get("area_name");
            if (RouteController.isBlank(code) || RouteController.isBlank(name)) { skipped++; continue; }
            Area a = areas.findByAreaCode(code.trim()).orElseGet(Area::new);
            boolean isNew = a.getOid() == null;
            a.setAreaCode(code.trim());
            a.setAreaName(name.trim());
            a.setCity(row.get("city"));
            a.setHqOid(resolveHq(row.get("hq")));
            a.setStateOid(resolveState(row.get("state")));
            a.setPincode(row.get("pincode"));
            a.setAreaType(row.get("area_type"));
            a.setDistanceKm(RouteController.toDecimal(row.get("distance_km")));
            a.setStatus("Y");
            areas.save(a);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Long resolveHq(String v) {
        return RouteController.isBlank(v) ? null
                : hqs.findFirstByHqCodeOrHqName(v.trim(), v.trim()).map(h -> h.getOid()).orElse(null);
    }
    private Long resolveState(String v) {
        return RouteController.isBlank(v) ? null
                : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }
}
