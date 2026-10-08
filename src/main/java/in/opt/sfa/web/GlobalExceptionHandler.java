package in.opt.sfa.web;

import in.opt.sfa.common.response.ResponseFormat;
import in.opt.sfa.exception.BadCredentialsException;
import in.opt.sfa.exception.ForbiddenException;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns every exception into the standard {@link ResponseFormat} ERROR envelope
 * (success=false) — so an error response has the exact same shape as a success
 * one, only with the right HTTP status and, for validation, per-field messages.
 *
 * Note: internal details (exception class, SQL, stack trace) are NOT sent to the
 * client — only a clean message; the full cause is logged server-side.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 401 — bad username/password or invalid/expired token. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseFormat<Object>> handleBadCredentials(BadCredentialsException e) {
        return ResponseFormat.error(HttpStatus.UNAUTHORIZED, msg(e));
    }

    /** 404 — a missing static resource / unknown path. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseFormat<Object>> handleNoResource(NoResourceFoundException e) {
        return ResponseFormat.error(HttpStatus.NOT_FOUND, "Resource not found");
    }

    /** 403 — authenticated but the role is not allowed. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseFormat<Object>> handleForbidden(ForbiddenException e) {
        return ResponseFormat.error(HttpStatus.FORBIDDEN, msg(e));
    }

    /** 400 — bad argument. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseFormat<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseFormat.error(HttpStatus.BAD_REQUEST, msg(e));
    }

    /** 400 — business/validation state problems raised by services. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseFormat<Object>> handleIllegalState(IllegalStateException e) {
        return ResponseFormat.error(HttpStatus.BAD_REQUEST, msg(e));
    }

    /** 400 — @Valid body failed; per-field messages go in `errors`. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseFormat<Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a, LinkedHashMap::new));
        return ResponseFormat.error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /** 500 — any DB/JPA failure. The real SQL is logged, not returned. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ResponseFormat<Object>> handleDataAccess(DataAccessException e, HttpServletRequest req) {
        log.error("Data access error at {}", req.getRequestURI(), e);
        return ResponseFormat.error(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred");
    }

    /** 500 — catch-all so nothing escapes un-enveloped. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseFormat<Object>> handleAll(Exception e, HttpServletRequest req) {
        log.error("Unhandled error at {}", req.getRequestURI(), e);
        return ResponseFormat.error(HttpStatus.INTERNAL_SERVER_ERROR, msg(e));
    }

    private static String msg(Throwable e) {
        return (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : e.getClass().getSimpleName();
    }
}
