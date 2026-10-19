package in.opt.sfa.tenant.master.area.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.entity.AreaMasterApproval;
import in.opt.sfa.tenant.master.area.service.AreaService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
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

    @RequiresPermission("AREA_SAVE")
    @PostMapping
    public AreaDto save(@RequestBody AreaDto form) {
        return service.save(form);
    }

    /** Add Multiple — one new area per submitted row (invalid / duplicate rows skipped with the reason). */
    @RequiresPermission("AREA_SAVE")
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<AreaDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("AREA_STATUS")
    @PostMapping("/{oid}/status")
    public AreaDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/options")
    @Operation(summary = "Can the user pick the area's employee (admin), the employees to pick from, the approval flow")
    public Map<String, Object> options() {
        return service.options();
    }

    @RequiresPermission("AREA_DELETE")
    @PostMapping("/{oid}/delete-request")
    @Operation(summary = "Ask for the area to be deleted; body {reason}. It stays active until the request is approved")
    public AreaDto requestDelete(@PathVariable Long oid, @RequestBody Map<String, String> body) {
        return service.requestDelete(oid, body == null ? null : body.get("reason"));
    }

    // Rights are checked in the service: AREA_ADD_APPROVE / AREA_DELETE_APPROVE by the request, role, team, never your own
    @PostMapping("/{oid}/approve")
    @Operation(summary = "Approve the step the area's add / delete request waits for (manager / admin, per the company's area approval flow)")
    public AreaDto approve(@PathVariable Long oid) {
        return service.approve(oid);
    }

    @PostMapping("/{oid}/reject")
    @Operation(summary = "Reject at the current step; body {reason}. Editing a rejected new area sends it for approval again")
    public AreaDto reject(@PathVariable Long oid, @RequestBody Map<String, String> body) {
        return service.reject(oid, body == null ? null : body.get("reason"));
    }

    @GetMapping("/{oid}/approvals")
    @Operation(summary = "Approval history: every request / approval / rejection with who did it")
    public List<AreaMasterApproval> approvals(@PathVariable Long oid) {
        return service.history(oid);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("area_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by area_code (only your own / team's areas); hq/state resolved by code or name. */
    @RequiresPermission("AREA_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
