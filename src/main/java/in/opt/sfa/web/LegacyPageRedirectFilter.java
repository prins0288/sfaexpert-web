package in.opt.sfa.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * A page in a sub-folder opened WITHOUT the /u/{index}/ prefix
 * (e.g. /report/dcr-report.html from an old bookmark or a hand-typed URL)
 * makes the page's base-href script resolve css/js relative to /report/ —
 * every asset 404s (/report/js/app.js …) and API calls go to the wrong path.
 * Redirect such requests to /u/0/<same path> (account 0, as the app does
 * everywhere else), which MultiAccountRoutingController then serves.
 *
 * Only direct browser requests are touched: the internal forward from
 * /u/{index}/... to the real page is a FORWARD dispatch and passes through.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyPageRedirectFilter extends OncePerRequestFilter {

    private static final Pattern PAGE_IN_FOLDER =
            Pattern.compile("^/(master|report|settings|utilities|ai|help)/[^/]+\\.html$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) return true;
        if (!"GET".equals(request.getMethod())) return true;
        return !PAGE_IN_FOLDER.matcher(pathWithinApp(request)).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // RELATIVE Location, resolved by the browser against the URL it actually
        // used — keeps a reverse-proxy sub-path (/sfaexpert) without having to
        // know it here. Path is always /<folder>/<page>.html, so one level up.
        String target = "../u/0" + pathWithinApp(request);
        if (request.getQueryString() != null) target += "?" + request.getQueryString();
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", target);
    }

    private static String pathWithinApp(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
