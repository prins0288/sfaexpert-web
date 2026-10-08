package in.opt.sfa.tenant.master.route.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.route.dto.RouteDto;
import in.opt.sfa.tenant.master.route.service.RouteService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Route master — list / get / save / add-multiple / bulk-upload / soft-delete. Tenant-routed. */
@Tag(name = "Route Master", description = "Routes — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/route")
public class RouteController {

    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    @GetMapping
    public List<RouteDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public RouteDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @RequiresPermission("ROUTE_SAVE")
    @PostMapping
    public RouteDto save(@RequestBody RouteDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @RequiresPermission("ROUTE_SAVE")
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<RouteDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("ROUTE_STATUS")
    @PostMapping("/{oid}/status")
    public RouteDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    /** Downloadable Excel template. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("route_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by route_code; hq/state resolved by code or name. */
    @RequiresPermission("ROUTE_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
