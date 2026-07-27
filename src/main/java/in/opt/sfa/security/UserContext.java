package in.opt.sfa.security;

/**
 * Per-request identity, taken from the VERIFIED JWT by TenantAuthFilter and
 * cleared after the request. Trustworthy because the token signature is checked
 * on every request — the client cannot forge these values.
 */
public final class UserContext {

    public record CurrentUser(String username, String role, String empId, String tenant) { }

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private UserContext() { }

    public static void set(CurrentUser user) { CURRENT.set(user); }
    public static CurrentUser get() { return CURRENT.get(); }
    public static String role() { CurrentUser u = CURRENT.get(); return u == null ? null : u.role(); }
    public static void clear() { CURRENT.remove(); }
}
