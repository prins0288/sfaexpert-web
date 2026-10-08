package in.opt.sfa.exception;

/** Authenticated but not allowed -> mapped to HTTP 403 by GlobalExceptionHandler. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
