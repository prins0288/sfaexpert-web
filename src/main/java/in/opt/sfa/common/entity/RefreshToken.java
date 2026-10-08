package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lives in the COMMON database. An opaque (non-JWT) long-lived credential that
 * lets the client silently obtain a new short-lived access JWT without asking
 * the user to log in again. Rotated on every use (see AuthService.refresh):
 * the token used is revoked and a fresh one issued, so a stolen-and-replayed
 * old token is immediately rejected.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "app_user_id", nullable = false)
    private Long appUserId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
