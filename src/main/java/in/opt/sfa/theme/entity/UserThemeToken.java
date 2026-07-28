package in.opt.sfa.theme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single per-user colour/size override (COMMON db). Only tokens the user has
 * actually changed are stored — everything else falls back to the preset delta
 * and then the mode default at resolve time.
 *
 * Overrides are stored per mode, so a user can tune their light theme and dark
 * theme independently. Unique on (username, mode, token_key).
 */
@Entity
@Table(name = "user_theme_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_theme_token",
                columnNames = {"username", "mode", "token_key"}))
@Getter
@Setter
public class UserThemeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 128, nullable = false)
    private String username;

    /** "light" | "dark" */
    @Column(name = "mode", length = 16, nullable = false)
    private String mode;

    /** ThemeToken.key(), e.g. "sidebar-bg". */
    @Column(name = "token_key", length = 64, nullable = false)
    private String tokenKey;

    /** The CSS value, e.g. "#12294d" or "260px". */
    @Column(name = "token_value", length = 128, nullable = false)
    private String tokenValue;
}
