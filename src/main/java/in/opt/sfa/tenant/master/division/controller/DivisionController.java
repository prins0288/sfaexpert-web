package in.opt.sfa.tenant.master.division.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.division.dto.DivisionDto;
import in.opt.sfa.tenant.master.division.service.DivisionService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Division master — top of the geography hierarchy (Division > Zone > State > HQ > City). Tenant-routed. */
@Tag(name = "Division Master", description = "Divisions — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/division")
public class DivisionController {

    private final DivisionService service;

    public DivisionController(DivisionService service) {
        this.service = service;
    }

    @GetMapping
    public List<DivisionDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public DivisionDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @RequiresPermission("DIVISION_SAVE")
    @PostMapping
    public DivisionDto save(@RequestBody DivisionDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @RequiresPermission("DIVISION_SAVE")
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<DivisionDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("DIVISION_STATUS")
    @PostMapping("/{oid}/status")
    public DivisionDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("division_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by division_code. */
    @RequiresPermission("DIVISION_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
