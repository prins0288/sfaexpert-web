package in.opt.sfa.menu.repository;

import in.opt.sfa.menu.entity.UserMenuFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface UserMenuFavoriteRepository extends JpaRepository<UserMenuFavorite, Long> {

    List<UserMenuFavorite> findByUsername(String username);

    Optional<UserMenuFavorite> findByUsernameAndMenuItemId(String username, Long menuItemId);

    @Transactional(transactionManager = "tenantTransactionManager")
    void deleteByUsernameAndMenuItemId(String username, Long menuItemId);
}
