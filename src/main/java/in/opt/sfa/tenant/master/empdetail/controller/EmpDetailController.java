package in.opt.sfa.tenant.master.empdetail.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest;
import in.opt.sfa.tenant.master.empdetail.service.EmpDetailService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * "Create Employee" (Masters > Master Entry > Create Employee) — single
 * quick-create/update form + bulk Excel upload. Creates/updates emp_detail
 * (tenant db) and keeps user_login_master (common db) in sync.
 */
@Tag(name = "Create Employee", description = "Quick-create employee + login, bulk Excel upload")
@RestController
@RequestMapping("/api/master/create-employee")
public class EmpDetailController {

    private final EmpDetailService service;

    public EmpDetailController(EmpDetailService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @GetMapping("/{empId}")
    public Map<String, Object> get(@PathVariable String empId) {
        return service.get(empId);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @PostMapping
    public Map<String, Object> save(@RequestBody EmpDetailSaveRequest form) {
        return service.save(form);
    }

    @RequiresPermission("EMP_DETAIL_STATUS")
    @PostMapping("/{empId}/status")
    public Map<String, Object> status(@PathVariable String empId, @RequestParam boolean active) {
        return service.updateStatus(empId, active);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("employee_template.xlsx", service.template());
    }

    @RequiresPermission("EMP_DETAIL_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
