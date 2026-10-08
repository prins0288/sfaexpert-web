package in.opt.sfa.tenant.master.state.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.state.dto.StateDto;
import in.opt.sfa.tenant.master.state.service.StateService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** State master — belongs to a Country. Tenant-routed. */
@Tag(name = "State Master", description = "States — soft-delete, bulk add, Excel template/upload")
@RestController
@RequestMapping("/api/master/state")
public class StateController {

    private final StateService service;

    public StateController(StateService service) {
        this.service = service;
    }

    @GetMapping
    public List<StateDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public StateDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @RequiresPermission("STATE_SAVE")
    @PostMapping
    public StateDto save(@RequestBody StateDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @RequiresPermission("STATE_SAVE")
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<StateDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("STATE_STATUS")
    @PostMapping("/{oid}/status")
    public StateDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("state_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by state_code; country resolved by code or name. */
    @RequiresPermission("STATE_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
