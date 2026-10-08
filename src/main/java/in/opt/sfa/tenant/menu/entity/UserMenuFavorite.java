package in.opt.sfa.tenant.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A user's starred menu item (TENANT db). One row per (user, menu item). The
 * shell shows these under a "Favorites" group for quick access. Keyed by username within the tenant's own database.
 */
@Entity
@Table(name = "user_menu_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_menu_favorite",
                columnNames = {"username", "menu_item_id"}))
@Getter
@Setter
public class UserMenuFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 128, nullable = false)
    private String username;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;
}
