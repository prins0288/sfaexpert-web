package in.opt.sfa.security;

import in.opt.sfa.tenant.context.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs on every request except /api/auth/**. It reads the Bearer token, pulls
 * the tenant claim, and binds it to the thread so tenant repositories route to
 * the correct database. The context is always cleared in finally() so threads
 * are never reused with a stale tenant.
 */
@Component
public class TenantAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenDenylist denylist;

    public TenantAuthFilter(JwtUtil jwtUtil, TokenDenylist denylist) {
        this.jwtUtil = jwtUtil;
        this.denylist = denylist;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Login has no token yet; /api/public/** is intentionally open (e.g. the
        // login page's default logo); anything outside /api/ (login page, static
        // assets, favicon) is public so the page itself can load.
        return path.startsWith("/api/auth") || path.startsWith("/api/public") || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "Missing bearer token");
            return;
        }

        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            // revoked on logout? (access-token denylist) — reject immediately
            if (denylist.isRevoked(claims.getId())) {
                unauthorized(response, "Session ended. Please log in again.");
                return;
            }
            String companyCode = claims.get("companyCode", String.class);
            if (companyCode == null || companyCode.isBlank()) {
                unauthorized(response, "No company code in token");
                return;
            }
            // sub now holds emp_id and username is its own claim; fall back to the
            // old layout (sub == username) for any token issued before this change.
            String username = claims.get("username", String.class);
            if (username == null || username.isBlank()) username = claims.getSubject();
            String empId = claims.get("emp_id", String.class);
            if (empId == null || empId.isBlank()) empId = claims.getSubject();

            Integer empLevel = claims.get("emp_level", Integer.class);
            TenantContext.setCompanyCode(companyCode);
            UserContext.set(new UserContext.CurrentUser(
                    username,
                    claims.get("role", String.class),
                    empId,
                    companyCode,
                    claims.get("designation_code", String.class),
                    empLevel));
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            unauthorized(response, "Invalid token");
        } finally {
            TenantContext.clear();
            UserContext.clear();
        }
    }

    /**
     * Write a real 401 JSON response DIRECTLY (status + body), instead of
     * response.sendError(). sendError() dispatches to the /error page, and on
     * Spring Boot 4 that round-trip surfaced auth failures to the client as 500
     * instead of 401 — which broke the front-end's "401 -> refresh, else log out
     * and go to the login page" handling (js/core/http.js). Writing 401 here keeps
     * the status intact so an invalid/expired token cleanly logs the user out.
     * The message is a fixed literal, so embedding it in the JSON is safe.
     */
    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) return;
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
