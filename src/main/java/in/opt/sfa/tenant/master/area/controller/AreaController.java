package in.opt.sfa.tenant.master.area.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.service.AreaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Area master — list / get / save / add-multiple / bulk-upload / soft-delete. Tenant-routed. */
@Tag(name = "Area Master", description = "Areas — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/area")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AreaDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public AreaDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public AreaDto save(@RequestBody AreaDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<AreaDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public AreaDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("area_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by area_code; hq/state resolved by code or name. */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
