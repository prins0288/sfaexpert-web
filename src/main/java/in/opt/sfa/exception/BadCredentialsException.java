package in.opt.sfa.exception;

/** Bad username/password (or invalid/expired refresh token) -> mapped to HTTP 401. */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
