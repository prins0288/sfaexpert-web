package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin password reset (Master Entry > Password Reset): set a typed or an
 * auto-generated 8-digit password for another user's login. Plain text, like
 * every password in this app. A user can reset only users of the same or a
 * lower emp level; SUPER_ADMIN can reset anyone.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmpDetailService employeesService;
    private final EmpDetailRepository employees;
    private final AppUserAdminService credentials;
    private final DesignationRepository designations;

    public PasswordResetService(EmpDetailService employeesService, EmpDetailRepository employees, AppUserAdminService credentials,
                                DesignationRepository designations) {
        this.designations = designations;
        this.employeesService = employeesService;
        this.employees = employees;
        this.credentials = credentials;
    }

    /** Employees with their login (username, active, last login). */
    public List<Map<String, Object>> list() {
        return employeesService.list();
    }

    /** A fresh 8-digit password (not saved). */
    public String generate() {
        return String.valueOf(10_000_000 + RANDOM.nextInt(90_000_000));
    }

    /** Reset the password; blank password = generate one. Returns username + the new password. */
    public Map<String, Object> reset(String empId, String password) {
        EmpDetail target = employees.findById(empId)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + empId));
        UserContext.CurrentUser me = UserContext.get();
        boolean superAdmin = me != null && "SUPER_ADMIN".equals(me.role());
        if (!superAdmin) {
            int myLevel = me == null || me.hierarchyLevel() == null ? 0 : me.hierarchyLevel();
            int theirLevel = target.getDesignationId() == null ? 0
                    : designations.findById(target.getDesignationId()).map(Designation::getHierarchyLevel).orElse(0);
            if (theirLevel > myLevel) {
                throw new ForbiddenException("You cannot reset the password of a higher-level user (" + target.getEmpName() + ")");
            }
        }
        String pwd = Strings.isBlank(password) ? generate() : password.trim();
        if (pwd.length() < 4) throw new IllegalStateException("Password must be at least 4 characters");
        if (pwd.length() > 100) throw new IllegalStateException("Password can be at most 100 characters");
        if (pwd.contains(" ")) throw new IllegalStateException("Password cannot contain spaces");
        var login = credentials.resetPassword(TenantContext.getCompanyCode(), empId, pwd);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("empId", empId);
        m.put("empName", target.getEmpName());
        m.put("username", login.getUsername());
        m.put("password", pwd);
        return m;
    }
}
