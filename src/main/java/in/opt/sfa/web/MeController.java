package in.opt.sfa.web;

import in.opt.sfa.security.JwtUtil;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.state.entity.State;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Current user's profile. Auth fields (username/tenant/role/emp_id) come from
 * the JWT (common DB); the full profile (name/designation/state) is fetched
 * from the TENANT database's emp_detail table, linked by emp_id. This is where
 * the common<->tenant relation is resolved at runtime.
 */
@Tag(name = "Current User", description = "Profile of the signed-in user (login + tenant employee record)")
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final JwtUtil jwtUtil;
    private final EmpDetailRepository employees;
    private final DesignationRepository designations;
    private final StateRepository states;

    public MeController(JwtUtil jwtUtil, EmpDetailRepository employees,
                        DesignationRepository designations, StateRepository states) {
        this.jwtUtil = jwtUtil;
        this.employees = employees;
        this.designations = designations;
        this.states = states;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> me(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        Claims claims = jwtUtil.parse(header.substring(7));   // filter guarantees a valid Bearer token

        Map<String, Object> res = new LinkedHashMap<>();
        // ---- from common DB (via JWT) ----  sub = emp_id; username is its own claim
        String username = claims.get("username", String.class);
        if (username == null) username = claims.getSubject();      // old-token fallback
        String empId = claims.get("emp_id", String.class);
        if (empId == null) empId = claims.getSubject();
        res.put("username", username);
        res.put("tenant", claims.get("tenant", String.class));
        res.put("role", claims.get("role", String.class));
        res.put("empId", empId);

        // ---- full profile from the tenant DB ----
        if (empId != null) {
            employees.findById(empId).ifPresent(e -> {
                res.put("name", e.getEmpName());
                res.put("designation", e.getDesignationId() == null ? null
                        : designations.findById(e.getDesignationId()).map(Designation::getDesignationName).orElse(null));
                res.put("state", e.getStateId() == null ? null
                        : states.findById(e.getStateId()).map(State::getStateName).orElse(null));
                res.put("mobile", e.getMobile());
                res.put("email", e.getOfficialEmail());
                res.put("empLevel", e.getEmpLevel());
            });
        }
        return res;
    }
}
