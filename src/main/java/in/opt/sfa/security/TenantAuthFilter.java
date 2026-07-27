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

    public TenantAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Login has no token yet; anything outside /api/ (login page, static
        // assets, favicon) is public so the page itself can load.
        return path.startsWith("/api/auth") || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
            return;
        }

        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            String tenantId = claims.get("tenant", String.class);
            if (tenantId == null || tenantId.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No tenant in token");
                return;
            }
            TenantContext.setTenantId(tenantId);
            UserContext.set(new UserContext.CurrentUser(
                    claims.getSubject(),
                    claims.get("role", String.class),
                    claims.get("emp_id", String.class),
                    tenantId));
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } finally {
            TenantContext.clear();
            UserContext.clear();
        }
    }
}
