package in.opt.sfa.auth;

import in.opt.sfa.auth.dto.LoginRequest;
import in.opt.sfa.auth.dto.LoginResponse;
import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.entity.RefreshToken;
import in.opt.sfa.common.repository.AppUserRepository;
import in.opt.sfa.common.repository.RefreshTokenRepository;
import in.opt.sfa.common.repository.TenantConfigRepository;
import in.opt.sfa.exception.BadCredentialsException;
import in.opt.sfa.common.util.RoleLevelMapper;
import in.opt.sfa.security.JwtUtil;
import in.opt.sfa.security.TokenDenylist;
import org.springframework.jdbc.core.JdbcTemplate;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.datasource.TenantDataSourceManager;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class AuthService {


    private final AppUserRepository appUserRepository;         // common
    private final TenantConfigRepository tenantConfigRepository; // common
    private final RefreshTokenRepository refreshTokenRepository; // common
    private final TenantDataSourceManager dataSourceManager;
    private final JwtUtil jwtUtil;
    private final TokenDenylist denylist;
    private final JdbcTemplate tenantJdbc;   // @Primary (tenant-routed) — reads emp_detail.emp_level
    private final SecureRandom secureRandom = new SecureRandom();

    /** How many DAYS a refresh token stays valid — set this in application.yml (app.jwt.refresh-expiration-days). */
    private final long refreshExpirationDays;

    public AuthService(AppUserRepository appUserRepository,
                       TenantConfigRepository tenantConfigRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TenantDataSourceManager dataSourceManager,
                       JwtUtil jwtUtil,
                       TokenDenylist denylist,
                       JdbcTemplate tenantJdbc,
                       @Value("${app.jwt.refresh-expiration-days:30}") long refreshExpirationDays) {
        this.appUserRepository = appUserRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.dataSourceManager = dataSourceManager;
        this.jwtUtil = jwtUtil;
        this.denylist = denylist;
        this.tenantJdbc = tenantJdbc;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    /**
     * 1) validate username/password against the COMMON user table
     * 2) read companyCode from the user, confirm tenant_config exists
     * 3) OPEN the tenant datasource right now (as required: DS opens on login)
     * 4) issue a short-lived access JWT + a long-lived refresh token
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isActive() || !request.password().equals(user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String companyCode = user.getCompanyCode();

        tenantConfigRepository.findByCompanyCode(companyCode)
                .orElseThrow(() -> new IllegalStateException(
                        "User '" + user.getUsername() + "' has no tenant config for '"
                                + companyCode + "'"));

        // Open (warm up) the tenant datasource now, and verify it connects.
        openTenantDatasource(companyCode);

        // Role + profile snapshot (designation, emp_level, state) from emp_detail.
        EmpInfo info = resolveEmpInfo(companyCode, user.getEmpId(), null);
        String role = info.role();

        // Read out the PREVIOUS login (null on a first-ever login) before overwriting it,
        // so the client can show "last login: ..." for the session that just ended.
        LocalDateTime previousLogin = user.getLastLoginAt();
        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);

        String accessToken = jwtUtil.generateToken(
                user.getEmpId(), user.getUsername(), companyCode, role, info.claims());
        String refreshToken = issueRefreshToken(user.getId());
        return new LoginResponse(accessToken, companyCode, role, previousLogin, refreshToken);
    }

    /** Role + JWT profile claims. The login row's emp_id links to emp_detail.emp_id
     *  (the developer/system stable key). Role comes from emp_detail.emp_level via
     *  RoleLevelMapper; designation/emp_level/state ride along as token
     *  claims. Falls back to RoleLevelMapper.USER (and no extra claims) if the user
     *  has no emp_detail row yet. */
    private record EmpInfo(String role, Map<String, Object> claims) { }

    private EmpInfo resolveEmpInfo(String companyCode, String empId, String fallbackRole) {
        String role = (fallbackRole == null || fallbackRole.isBlank()) ? RoleLevelMapper.USER : fallbackRole;
        Map<String, Object> claims = new LinkedHashMap<>();
        if (empId != null && !empId.isBlank()) {
            try {
                TenantContext.setCompanyCode(companyCode);
                Map<String, Object> row = tenantJdbc.query(
                        "SELECT e.emp_level, e.state_id, d.designation_name, d.designation_code " +
                        "FROM emp_detail e LEFT JOIN designation_master d ON d.oid = e.designation " +
                        "WHERE e.emp_id = ?",
                        rs -> {
                            if (!rs.next()) return null;
                            Map<String, Object> m = new LinkedHashMap<>();
                            Object lvl = rs.getObject("emp_level");
                            m.put("emp_level", lvl == null ? null : ((Number) lvl).intValue());
                            m.put("designation", rs.getString("designation_name"));
                            m.put("designation_code", rs.getString("designation_code"));
                            m.put("state", rs.getObject("state_id"));
                            return m;
                        }, empId);
                if (row != null) {
                    Integer level = (Integer) row.get("emp_level");
                    if (level != null) role = RoleLevelMapper.roleFor(level);
                    claims.put("emp_level", level);
                    claims.put("designation", row.get("designation"));
                    claims.put("designation_code", row.get("designation_code"));
                    claims.put("state", row.get("state"));
                }
            } catch (Exception e) {
                log.warn("Could not read emp_detail profile for emp_id '{}': {}", empId, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        return new EmpInfo(role, claims);
    }

    /**
     * Exchanges a still-valid refresh token for a brand-new access token —
     * WITHOUT the user re-entering credentials. Rotates the refresh token
     * (revokes the one just used, issues a new one) so a leaked-and-replayed
     * old token stops working the moment the legitimate client refreshes.
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public LoginResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BadCredentialsException("Missing refresh token");
        }
        RefreshToken existing = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (existing.isRevoked() || existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked — please log in again");
        }
        AppUser user = appUserRepository.findById(existing.getAppUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account disabled");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Re-read the profile so a refreshed token carries the current role/claims.
        EmpInfo info = resolveEmpInfo(user.getCompanyCode(), user.getEmpId(), null);
        String accessToken = jwtUtil.generateToken(
                user.getEmpId(), user.getUsername(), user.getCompanyCode(), info.role(), info.claims());
        String newRefreshToken = issueRefreshToken(user.getId());
        return new LoginResponse(accessToken, user.getCompanyCode(), info.role(), user.getLastLoginAt(), newRefreshToken);
    }

    /**
     * Sign-out: revokes the refresh token (so it can never be exchanged again) AND
     * denylists the current access token by its jti (so the still-unexpired access
     * token is rejected immediately too, not only after it expires). Idempotent;
     * safe if either token is null/blank/already-invalid.
     */
    @Transactional(transactionManager = "commonTransactionManager")
    public void logout(String refreshTokenValue, String accessToken) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .ifPresent(rt -> { rt.setRevoked(true); refreshTokenRepository.save(rt); });
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                var c = jwtUtil.parse(accessToken);
                denylist.revoke(c.getId(), c.getExpiration().getTime());
            } catch (Exception ignored) {
                // token already invalid/expired — nothing left to deny
            }
        }
    }

    private String issueRefreshToken(Long appUserId) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setAppUserId(appUserId);
        rt.setCreatedAt(LocalDateTime.now());
        rt.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);
        return token;
    }

    private void openTenantDatasource(String companyCode) {
        TenantContext.setCompanyCode(companyCode);
        try {
            DataSource ds = dataSourceManager.getDataSource(companyCode);
            try (Connection ignored = ds.getConnection()) {
                log.info("Tenant '{}' datasource opened and verified on login", companyCode);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not open tenant database for '" + companyCode + "': "
                            + e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
