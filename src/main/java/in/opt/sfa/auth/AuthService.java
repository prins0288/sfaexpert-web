package in.opt.sfa.auth;

import in.opt.sfa.auth.dto.LoginRequest;
import in.opt.sfa.auth.dto.LoginResponse;
import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.repository.AppUserRepository;
import in.opt.sfa.common.repository.TenantConfigRepository;
import in.opt.sfa.security.JwtUtil;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.datasource.TenantDataSourceManager;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
@Slf4j
public class AuthService {


    private final AppUserRepository appUserRepository;         // common
    private final TenantConfigRepository tenantConfigRepository; // common
    private final TenantDataSourceManager dataSourceManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository appUserRepository,
                       TenantConfigRepository tenantConfigRepository,
                       TenantDataSourceManager dataSourceManager,
                       JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.dataSourceManager = dataSourceManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 1) validate username/password against the COMMON user table
     * 2) read tenantId from the user, confirm tenant_config exists
     * 3) OPEN the tenant datasource right now (as required: DS opens on login)
     * 4) issue a JWT carrying the tenant so later requests route automatically
     */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String tenantId = user.getTenantId();

        tenantConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "User '" + user.getUsername() + "' has no tenant config for '"
                                + tenantId + "'"));

        // Open (warm up) the tenant datasource now, and verify it connects.
        openTenantDatasource(tenantId);

        String token = jwtUtil.generateToken(
                user.getUsername(), tenantId, user.getEmpId(), user.getRole());
        return new LoginResponse(token, tenantId, user.getRole());
    }

    private void openTenantDatasource(String tenantId) {
        TenantContext.setTenantId(tenantId);
        try {
            DataSource ds = dataSourceManager.getDataSource(tenantId);
            try (Connection ignored = ds.getConnection()) {
                log.info("Tenant '{}' datasource opened and verified on login", tenantId);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not open tenant database for '" + tenantId + "': "
                            + e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    /** Simple 401-mapped exception. */
    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String message) {
            super(message);
        }
    }
}
