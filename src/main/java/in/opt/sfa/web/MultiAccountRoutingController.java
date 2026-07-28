package in.opt.sfa.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * Gmail-style multi-account URLs. The path segment /u/{index}/ selects WHICH
 * logged-in account is active (the browser holds one JWT per account in
 * localStorage; the frontend picks accounts[index] from the URL). The server
 * just forwards /u/{index}/whatever.html to the real static page /whatever.html
 * so deep links and refreshes work; all auth stays in the JWT the page sends.
 *
 * Static assets (/css, /js, /api, /favicon) are absolute, so they keep working
 * under the /u/{index}/ prefix without any change.
 */
@Hidden
@Controller
public class MultiAccountRoutingController {

    @GetMapping("/u/{index}/**")
    public void forwardToPage(@PathVariable String index,
                              HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();          // e.g. /u/1/master/route.html
        String prefix = "/u/" + index + "/";
        String rest = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
        if (rest.isBlank()) {
            rest = "dashboard.html";                     // /u/1/ -> dashboard
        }
        request.getRequestDispatcher("/" + rest).forward(request, response);
    }
}
