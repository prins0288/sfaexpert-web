package in.opt.sfa.auth.dto;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        String companyCode,
        String role,
        String tokenType,
        /** The PREVIOUS successful login's timestamp (null on a user's first-ever login). */
        LocalDateTime lastLoginAt,
        /** Long-lived opaque token — send to /api/auth/refresh to get a new access token without re-login. */
        String refreshToken) {

    public LoginResponse(String token, String companyCode, String role, LocalDateTime lastLoginAt, String refreshToken) {
        this(token, companyCode, role, "Bearer", lastLoginAt, refreshToken);
    }
}
