package in.opt.sfa.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * Replaces Spring Boot's default "whitelabel" error page. For things that never
 * reach a @RestControllerAdvice handler — an unmapped URL (404), a filter
 * sendError, a static-resource miss — this renders:
 *
 *   - a readable HTML error page (with the ACTUAL message + stack trace) for
 *     browser requests, and
 *   - a JSON body for API clients (Accept: application/json, or /api/* paths).
 *
 * The exception, message, binding errors and stack trace are all pulled from
 * Spring's ErrorAttributes so the real cause is always shown.
 */
@Hidden
@RestController
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        Map<String, Object> attrs = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request),
                ErrorAttributeOptions.of(
                        Include.EXCEPTION,
                        Include.MESSAGE,
                        Include.BINDING_ERRORS,
                        Include.STACK_TRACE));

        // Prefer the container's real error status (jakarta.servlet.error.status_code);
        // on Spring Boot 4 the ErrorAttributes "status" wasn't always populated for a
        // filter-initiated sendError, which defaulted this to 500 and hid real 401/403/404s.
        Object scAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = scAttr instanceof Integer sc ? sc
                : (attrs.get("status") instanceof Integer s ? s : 500);

        if (wantsJson(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(attrs);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(renderHtml(status, attrs));
    }

    /** JSON for API paths or clients that ask for JSON; HTML for browsers. */
    private boolean wantsJson(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (uri != null && uri.toString().startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null
                && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }

    private String renderHtml(int status, Map<String, Object> attrs) {
        String error = str(attrs.get("error"), "Error");
        String message = str(attrs.get("message"), "");
        String exception = str(attrs.get("exception"), "");
        String path = str(attrs.get("path"), "");
        String trace = str(attrs.get("trace"), "");
        String errors = attrs.get("errors") != null ? attrs.get("errors").toString() : "";

        StringBuilder details = new StringBuilder();
        if (!exception.isEmpty()) details.append(row("Exception", exception));
        if (!path.isEmpty())      details.append(row("Path", path));
        if (!errors.isEmpty())    details.append(row("Field errors", errors));

        String traceBlock = trace.isEmpty() ? "" :
                "<details><summary>Stack trace</summary><pre>" + esc(trace) + "</pre></details>";

        return """
            <!DOCTYPE html>
            <html lang="en"><head><meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%d %s</title>
            <style>
              * { box-sizing: border-box; }
              body { margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center;
                     font-family: system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
                     background: linear-gradient(135deg,#7f1d1d,#0f172a); color:#0f172a; padding:24px; }
              .card { background:#fff; max-width:720px; width:100%%; padding:32px; border-radius:14px;
                      box-shadow:0 20px 50px rgba(0,0,0,.35); }
              .code { font-size:64px; font-weight:800; line-height:1; color:#dc2626; }
              h1 { margin:6px 0 2px; font-size:22px; }
              .msg { margin:14px 0 20px; padding:12px 14px; background:#fef2f2; border-left:4px solid #dc2626;
                     border-radius:6px; color:#991b1b; font-size:14px; white-space:pre-wrap; word-break:break-word; }
              table { width:100%%; border-collapse:collapse; font-size:13px; }
              td { padding:8px 10px; border-top:1px solid #f1f5f9; vertical-align:top; word-break:break-word; }
              td.k { color:#64748b; width:120px; font-weight:600; }
              details { margin-top:16px; } summary { cursor:pointer; color:#2563eb; font-size:13px; }
              pre { margin-top:10px; background:#0f172a; color:#e2e8f0; padding:14px; border-radius:8px;
                    font-size:12px; overflow:auto; max-height:320px; }
              a.home { display:inline-block; margin-top:20px; color:#2563eb; font-size:14px; text-decoration:none; }
            </style></head>
            <body><div class="card">
              <div class="code">%d</div>
              <h1>%s</h1>
              <div class="msg">%s</div>
              <table>%s</table>
              %s
              <a class="home" href="/">&larr; Back to login</a>
            </div></body></html>
            """.formatted(
                status, esc(error),
                status, esc(error),
                message.isEmpty() ? "(no message)" : esc(message),
                details.toString(),
                traceBlock);
    }

    private String row(String k, String v) {
        return "<tr><td class=\"k\">" + esc(k) + "</td><td>" + esc(v) + "</td></tr>";
    }

    private String str(Object o, String def) {
        return o == null ? def : o.toString();
    }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
