package in.opt.sfa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API versioning without touching a single controller.
 *
 * A request to {@code /api/v1/...} is forwarded internally to {@code /api/...},
 * so the existing (unversioned) endpoints answer it. The web UI keeps calling
 * {@code /api/...} unchanged; the mobile app pins {@code /api/v1/...} so a future
 * change can ship as {@code /api/v2/...} while old app versions keep working on v1.
 *
 * Runs FIRST (highest precedence): it forwards before the auth filter runs, and on
 * the forwarded {@code /api/...} dispatch the auth filter runs normally — so
 * {@code /api/v1/auth/**} and {@code /api/v1/public/**} stay open exactly like their
 * unversioned counterparts, and everything else stays JWT-protected.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionRewriteFilter extends OncePerRequestFilter {

    private static final String V1_PREFIX = "/api/v1/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();   // context-path-relative, e.g. "/api/v1/master/hq"
        if (path != null && path.startsWith(V1_PREFIX)) {
            String target = "/api/" + path.substring(V1_PREFIX.length());   // -> "/api/master/hq"
            request.getRequestDispatcher(target).forward(request, response); // query params are preserved
            return;
        }
        chain.doFilter(request, response);
    }

    /** Also apply on FORWARD/ASYNC dispatches so nothing slips past. */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }
}
