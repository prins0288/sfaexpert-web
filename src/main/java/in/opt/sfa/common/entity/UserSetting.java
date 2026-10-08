package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A generic PER-USER preference (COMMON db) as a key/value pair — the per-user
 * counterpart to the per-tenant company_setting_master. Keyed by username
 * (globally unique) + setting_key, so a user's choice follows them across
 * tenants and devices. Today it holds the chosen UI language ("lang"); any
 * future per-user, non-theme preference can reuse it with a new key.
 */
@Entity
@Table(name = "user_setting",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_setting", columnNames = {"username", "setting_key"}))
@Getter
@Setter
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(name = "setting_key", nullable = false, length = 64)
    private String settingKey;

    @Column(name = "setting_value", length = 255)
    private String settingValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
