package in.opt.sfa.common.service;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages login credentials in the COMMON database (app_user). Employee CRUD in
 * the tenant DB calls this to keep the matching login row (username / plain-text
 * password / role / enabled) in sync, linked by emp_id + company_code.
 *
 * All methods use the "commonTransactionManager" so they run against sfa_central
 * even when invoked from within a tenant-DB transaction.
 */
@Service
public class AppUserAdminService {

    private final AppUserRepository users;

    public AppUserAdminService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public List<AppUser> listByCompanyCode(String companyCode) {
        return users.findByCompanyCode(companyCode);
    }

    /**
     * Create or update the login for an employee. On create a password is
     * required; on update a blank password keeps the existing one.
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public AppUser upsert(String companyCode, String empId, String username,
                          String rawPassword, boolean enabled) {
        if (empId == null || empId.isBlank()) {
            throw new IllegalStateException("emp_id is required");   // user_login_master.emp_id is mandatory
        }
        AppUser u = users.findByEmpIdAndCompanyCode(empId, companyCode).orElseGet(AppUser::new);
        boolean isNew = u.getId() == null;

        if (username != null && !username.isBlank()) {
            users.findByUsername(username)
                    .filter(other -> u.getId() == null || !other.getId().equals(u.getId()))
                    .ifPresent(other -> { throw new IllegalStateException("Username already exists: " + username); });
            u.setUsername(username);
        } else if (isNew) {
            throw new IllegalStateException("Username is required");
        }

        u.setCompanyCode(companyCode);
        u.setEmpId(empId);
        u.setActive(enabled);

        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(rawPassword);
        } else if (isNew) {
            throw new IllegalStateException("Password is required for a new login");
        }
        return users.save(u);
    }

    /**
     * Is this username free? Usernames are unique across ALL companies
     * (user_login_master.username). A name already owned by the same
     * employee (companyCode + empId — i.e. editing yourself) counts as free.
     */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public boolean isUsernameAvailable(String username, String companyCode, String empId) {
        return users.findByUsername(username)
                .map(u -> empId != null && empId.equals(u.getEmpId()) && companyCode.equals(u.getCompanyCode()))
                .orElse(true);
    }

    /** Link a login to an employee id if it isn't linked yet (so future JWTs carry it). */
    @Transactional(transactionManager = "commonTransactionManager")
    public void linkEmpId(String username, String empId) {
        users.findByUsername(username).ifPresent(u -> {
            if (u.getEmpId() == null || u.getEmpId().isBlank()) {
                u.setEmpId(empId);
                users.save(u);
            }
        });
    }

    /** Change the caller's OWN password after verifying the current one. */
    @Transactional(transactionManager = "commonTransactionManager")
    public void changeOwnPassword(String username, String currentRaw, String newRaw) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
        if (currentRaw == null || !currentRaw.equals(u.getPassword())) {
            throw new IllegalStateException("Current password is incorrect");
        }
        if (newRaw == null || newRaw.isBlank()) {
            throw new IllegalStateException("New password is required");
        }
        u.setPassword(newRaw);
        users.save(u);
    }

    @Transactional(transactionManager = "commonTransactionManager")
    public void setEnabled(String companyCode, String empId, boolean enabled) {
        users.findByEmpIdAndCompanyCode(empId, companyCode).ifPresent(u -> {
            u.setActive(enabled);
            users.save(u);
        });
    }
}
