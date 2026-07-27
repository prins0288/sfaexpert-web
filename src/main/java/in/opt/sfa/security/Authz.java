package in.opt.sfa.security;

/**
 * Tiny server-side authorization helper. Reads the role from the VERIFIED token
 * (UserContext) — never from anything the client can edit — and rejects with 403
 * if it is not one of the allowed roles.
 *
 * Usage in a controller:  Authz.requireRole("ADMIN", "MANAGER");
 */
public final class Authz {

    private Authz() { }

    public static void requireRole(String... allowed) {
        String role = UserContext.role();
        for (String a : allowed) {
            if (a.equals(role)) return;
        }
        throw new ForbiddenException(
                "Requires role " + String.join(" or ", allowed) + " (you are " + role + ")");
    }
}
