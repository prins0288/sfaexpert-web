package in.opt.sfa.tenant.master.empdetail.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest;
import in.opt.sfa.tenant.master.empdetail.dto.EmpProfileDto;
import in.opt.sfa.tenant.master.empdetail.service.EmpProfileService;
import in.opt.sfa.tenant.master.empdetail.service.EmpDetailService;
import in.opt.sfa.tenant.master.empdetail.service.EmpFileService;
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
    private final EmpProfileService profileService;
    private final EmpFileService files;

    public EmpDetailController(EmpDetailService service, EmpProfileService profileService, EmpFileService files) {
        this.files = files;
        this.service = service;
        this.profileService = profileService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    /** Default login for a new employee: {username: PREFIX + 8 digits, password: 8 digits}. */
    @GetMapping("/suggest-login")
    public Map<String, Object> suggestLogin() {
        return service.suggestLogin();
    }

    /** {username, available, message} — empId (on edit) lets an employee keep their own username. */
    @GetMapping("/username-available")
    public Map<String, Object> usernameAvailable(@RequestParam String username,
                                                 @RequestParam(required = false) String empId) {
        return service.usernameAvailability(username, empId);
    }

    /** Dropdown values for the profile sections that have no master table (relationships, blood groups, …). */
    @GetMapping("/profile-options")
    public Map<String, Object> profileOptions() {
        return profileService.options();
    }

    /** The optional profile sections (address, bank, nominee, …) of one employee. */
    @GetMapping("/{empId}/profile")
    public EmpProfileDto profile(@PathVariable String empId) {
        return profileService.load(empId);
    }

    @GetMapping("/{empId}")
    public Map<String, Object> get(@PathVariable String empId) {
        return service.get(empId);
    }

    /** Promotion / transfer timeline: every designation / manager / division / HQ / state change, oldest first. */
    @GetMapping("/{empId}/history")
    public List<Map<String, Object>> history(@PathVariable String empId) {
        return service.assignmentHistory(empId);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @PostMapping
    public Map<String, Object> save(@RequestBody EmpDetailSaveRequest form) {
        return service.save(form);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @PostMapping(value = "/{empId}/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Set the employee's photo (image only); shown in the top-right avatar when they log in")
    public Map<String, Object> photo(@PathVariable String empId, @RequestParam("file") MultipartFile file) throws Exception {
        return files.setPhoto(empId, file);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @DeleteMapping("/{empId}/photo")
    public Map<String, Object> removePhoto(@PathVariable String empId) throws Exception {
        return files.setPhoto(empId, null);
    }

    @GetMapping("/{empId}/documents")
    @io.swagger.v3.oas.annotations.Operation(summary = "The employee's uploaded documents (type from Document Master)")
    public List<Map<String, Object>> documents(@PathVariable String empId) {
        return files.list(empId);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @PostMapping(value = "/{empId}/documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Upload one document (PDF or image) for the employee")
    public Map<String, Object> uploadDocument(@PathVariable String empId, @RequestParam("documentTypeId") Long documentTypeId,
                                              @RequestParam(value = "documentNo", required = false) String documentNo,
                                              @RequestParam(value = "remarks", required = false) String remarks,
                                              @RequestParam("file") MultipartFile file) throws Exception {
        return files.upload(empId, documentTypeId, documentNo, remarks, file);
    }

    @RequiresPermission("EMP_DETAIL_SAVE")
    @DeleteMapping("/{empId}/documents/{documentId}")
    public Map<String, Object> removeDocument(@PathVariable String empId, @PathVariable Long documentId) {
        files.remove(empId, documentId);
        return Map.of("removed", true);
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
