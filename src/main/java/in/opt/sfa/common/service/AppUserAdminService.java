package in.opt.sfa.common.service;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.repository.AppUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages login credentials in the COMMON database (app_user). Employee CRUD in
 * the tenant DB calls this to keep the matching login row (username / bcrypt
 * password / role / enabled) in sync, linked by emp_id + tenant_id.
 *
 * All methods use the "commonTransactionManager" so they run against sfa_central
 * even when invoked from within a tenant-DB transaction.
 */
@Service
public class AppUserAdminService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AppUserAdminService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public List<AppUser> listByTenant(String tenantId) {
        return users.findByTenantId(tenantId);
    }

    /**
     * Create or update the login for an employee. On create a password is
     * required; on update a blank password keeps the existing one.
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public AppUser upsert(String tenantId, String empId, String username,
                          String rawPassword, String role, boolean enabled) {
        AppUser u = users.findByEmpIdAndTenantId(empId, tenantId).orElseGet(AppUser::new);
        boolean isNew = u.getId() == null;

        if (username != null && !username.isBlank()) {
            users.findByUsername(username)
                    .filter(other -> u.getId() == null || !other.getId().equals(u.getId()))
                    .ifPresent(other -> { throw new IllegalStateException("Username already exists: " + username); });
            u.setUsername(username);
        } else if (isNew) {
            throw new IllegalStateException("Username is required");
        }

        u.setTenantId(tenantId);
        u.setEmpId(empId);
        if (role != null && !role.isBlank()) {
            u.setRole(role);
        } else if (isNew) {
            u.setRole("USER");
        }
        u.setEnabled(enabled);

        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(encoder.encode(rawPassword));
        } else if (isNew) {
            throw new IllegalStateException("Password is required for a new login");
        }
        return users.save(u);
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
        if (currentRaw == null || !encoder.matches(currentRaw, u.getPassword())) {
            throw new IllegalStateException("Current password is incorrect");
        }
        if (newRaw == null || newRaw.isBlank()) {
            throw new IllegalStateException("New password is required");
        }
        u.setPassword(encoder.encode(newRaw));
        users.save(u);
    }

    @Transactional(transactionManager = "commonTransactionManager")
    public void setEnabled(String tenantId, String empId, boolean enabled) {
        users.findByEmpIdAndTenantId(empId, tenantId).ifPresent(u -> {
            u.setEnabled(enabled);
            users.save(u);
        });
    }
}
