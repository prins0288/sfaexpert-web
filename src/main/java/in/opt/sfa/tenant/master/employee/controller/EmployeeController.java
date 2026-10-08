package in.opt.sfa.tenant.master.employee.controller;

import in.opt.sfa.tenant.master.employee.dto.EmployeeDto;
import in.opt.sfa.tenant.master.employee.service.EmployeeService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Employee master. Tenant-routed profile + common-db login, kept in sync (role-gated). */
@Tag(name = "Employee Master", description = "Employees and their login (role-gated)")
@RestController
@RequestMapping("/api/master/employee")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public EmployeeDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    /** Create/update the employee profile (tenant) AND its login (common). */
    @RequiresPermission("EMPLOYEE_SAVE")
    @PostMapping
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        return service.save(body);
    }

    /** Soft delete / restore — also enables/disables the login. */
    @RequiresPermission("EMPLOYEE_STATUS")
    @PostMapping("/{oid}/status")
    public Map<String, Object> status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }
}
