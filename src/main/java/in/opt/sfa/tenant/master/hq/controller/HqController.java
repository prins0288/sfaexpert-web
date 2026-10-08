package in.opt.sfa.tenant.master.hq.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.hq.dto.HqDto;
import in.opt.sfa.tenant.master.hq.service.HqService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** HQ master — belongs to a State. Tenant-routed. */
@Tag(name = "HQ Master", description = "HQs — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/hq")
public class HqController {

    private final HqService service;

    public HqController(HqService service) {
        this.service = service;
    }

    @GetMapping
    public List<HqDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public HqDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public HqDto save(@RequestBody HqDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<HqDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public HqDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("hq_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by hq_code; state resolved by code or name. */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
