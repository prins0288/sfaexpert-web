package in.opt.sfa.tenant.master.zone.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.zone.dto.ZoneDto;
import in.opt.sfa.tenant.master.zone.service.ZoneService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Zone master — belongs to a Division. Tenant-routed. */
@Tag(name = "Zone Master", description = "Zones — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/zone")
public class ZoneController {

    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @GetMapping
    public List<ZoneDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public ZoneDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public ZoneDto save(@RequestBody ZoneDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<ZoneDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public ZoneDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("zone_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by zone_code; division resolved by code or name. */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
