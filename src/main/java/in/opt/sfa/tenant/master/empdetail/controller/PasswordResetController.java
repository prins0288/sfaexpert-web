package in.opt.sfa.tenant.master.empdetail.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.empdetail.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Master Entry > Password Reset. All calls need USER_PASSWORD_RESET. */
@RestController
@RequestMapping("/api/master/password-reset")
@Tag(name = "Password Reset", description = "Reset other users' login passwords (typed or auto-generated)")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @RequiresPermission("USER_PASSWORD_RESET")
    @GetMapping
    @Operation(summary = "Employees with their login: username, active, last login")
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @RequiresPermission("USER_PASSWORD_RESET")
    @GetMapping("/generate")
    @Operation(summary = "A new 8-digit password suggestion (not saved)")
    public Map<String, String> generate() {
        return Map.of("password", service.generate());
    }

    @RequiresPermission("USER_PASSWORD_RESET")
    @PostMapping("/{empId}")
    @Operation(summary = "Set a new password; body {password}; blank = auto-generate. Returns username + password once.")
    public Map<String, Object> reset(@PathVariable String empId, @RequestBody(required = false) Map<String, String> body) {
        return service.reset(empId, body == null ? null : body.get("password"));
    }
}
