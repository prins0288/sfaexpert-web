package in.opt.sfa.web;

import in.opt.sfa.security.JwtUtil;
import in.opt.sfa.tenant.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Current user's profile. Auth fields (username/tenant/role/emp_id) come from
 * the JWT (common DB); the full profile (name/designation/state/district/dob)
 * is fetched from the TENANT database's employee table, linked by emp_id.
 * This is where the common<->tenant relation is resolved at runtime.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final JwtUtil jwtUtil;
    private final EmployeeRepository employees;

    public MeController(JwtUtil jwtUtil, EmployeeRepository employees) {
        this.jwtUtil = jwtUtil;
        this.employees = employees;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> me(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        Claims claims = jwtUtil.parse(header.substring(7));   // filter guarantees a valid Bearer token

        Map<String, Object> res = new LinkedHashMap<>();
        // ---- from common DB (via JWT) ----
        res.put("username", claims.getSubject());
        res.put("tenant", claims.get("tenant", String.class));
        res.put("role", claims.get("role", String.class));
        String empId = claims.get("emp_id", String.class);
        res.put("empId", empId);

        // ---- full profile from the tenant DB ----
        if (empId != null) {
            employees.findByEmpId(empId).ifPresent(e -> {
                res.put("name", e.getEmpName());
                res.put("designation", e.getDesignationName());
                res.put("state", e.getStateName());
                res.put("district", e.getDistrictName());
                res.put("dob", e.getDob());
                res.put("mobile", e.getMobile());
                res.put("email", e.getEmail());
                res.put("empLevel", e.getEmpLevel());
            });
        }
        return res;
    }
}
