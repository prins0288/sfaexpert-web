package in.opt.sfa.web;

import in.opt.sfa.auth.AuthService.BadCredentialsException;
import in.opt.sfa.security.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One place that turns every exception into a consistent JSON error body. The
 * body always carries the ACTUAL error message (and the root-cause message when
 * the exception is wrapped), so callers see what really went wrong instead of a
 * generic "500".
 *
 * Shape:
 * {
 *   "timestamp": "...", "status": 500, "error": "Internal Server Error",
 *   "exception": "org.springframework.dao...", "message": "...",
 *   "rootCause": "java.sql...: Table 'x' doesn't exist", "path": "/api/products"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 401 — bad username/password. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException e, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, e, req);
    }

    /** 404 — a missing static resource / favicon / unknown path. Not a real error. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(
            NoResourceFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, e, req);
    }

    /** 403 — authenticated but the token's role is not allowed for this action. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            ForbiddenException e, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, e, req);
    }

    /** 400 — tenant/config problems (missing tenant, cannot open tenant DB, no tenant bound). */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, e, req);
    }

    /** 400 — @Valid request body failed; return every field error. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, req);
        body.put("message", "Validation failed");
        body.put("fieldErrors", e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 500 — any DB/JPA failure; surfaces the real SQL error via root cause. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(
            DataAccessException e, HttpServletRequest req) {
        log.error("Data access error at {}", req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e, req);
    }

    /** 500 — catch-all so NOTHING escapes without a proper, real message. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e, HttpServletRequest req) {
        log.error("Unhandled error at {}", req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e, req);
    }

    // ------------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, Throwable e, HttpServletRequest req) {
        Map<String, Object> body = base(status, req);
        body.put("exception", e.getClass().getName());
        body.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

        Throwable root = rootCause(e);
        if (root != e && root.getMessage() != null) {
            body.put("rootCause", root.getClass().getName() + ": " + root.getMessage());
        }
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> base(HttpStatus status, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("path", req.getRequestURI());
        return body;
    }

    private Throwable rootCause(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
