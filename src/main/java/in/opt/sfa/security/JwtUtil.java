package in.opt.sfa.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates JWTs. The subject (sub) is the employee id (emp_id); the
 * login username, tenant id, role and a small profile snapshot (designation,
 * emp_level, state, district) ride along as custom claims, so every later
 * request can be routed and identified without re-reading the databases.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
        // HS256 needs a key of at least 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * @param empId       becomes the subject (sub)
     * @param username    the login username (own claim, so sub can hold emp_id)
     * @param extraClaims optional profile snapshot (designation, emp_level, state,
     *                    district); null values are skipped to keep the token lean
     */
    public String generateToken(String empId, String username, String companyCode, String role,
                                Map<String, Object> extraClaims) {
        Date now = new Date();
        var builder = Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())   // jti — lets logout denylist this exact token
                .subject(empId)                               // sub = emp_id
                .claim("username", username)
                .claim("companyCode", companyCode)
                .claim("emp_id", empId)                       // kept alongside sub for clarity/back-compat
                .claim("role", role);
        if (extraClaims != null) {
            extraClaims.forEach((k, v) -> { if (v != null) builder.claim(k, v); });
        }
        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
