package in.opt.sfa.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Denylist of access-token IDs (JWT "jti") that were revoked before their natural
 * expiry — i.e. on logout. A stateless JWT normally stays valid until it expires;
 * this lets a logout invalidate the access token IMMEDIATELY too (the refresh
 * token is revoked in the DB separately).
 *
 * Kept in memory and keyed by jti with the token's own expiry, so it stays tiny:
 * a scheduled sweep drops entries once they'd have expired anyway (a plain
 * ConcurrentHashMap lookup per request — no DB hit, matching the filter's design).
 *
 * Note: in-memory means it is per-instance and cleared on restart (a revoked
 * token would work again after a restart, until it expires). For a multi-instance
 * deployment, back this with a shared store (DB/Redis).
 */
@Component
public class TokenDenylist {

    /** jti -> token expiry (epoch millis). */
    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    /** Revoke a token by its jti until its natural expiry. */
    public void revoke(String jti, long expiryEpochMs) {
        if (jti != null && !jti.isBlank()) revoked.put(jti, expiryEpochMs);
    }

    /** True if this jti was revoked (and hasn't expired yet). */
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Long exp = revoked.get(jti);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) { revoked.remove(jti); return false; }
        return true;
    }

    /** Drop entries whose tokens have expired anyway (keeps the map small). */
    @Scheduled(fixedDelay = 600_000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        revoked.entrySet().removeIf(e -> e.getValue() < now);
    }
}
