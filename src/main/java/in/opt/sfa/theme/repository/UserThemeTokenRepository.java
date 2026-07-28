package in.opt.sfa.theme.repository;

import in.opt.sfa.theme.entity.UserThemeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** COMMON-db repository (bound to the common EMF via package location). */
public interface UserThemeTokenRepository extends JpaRepository<UserThemeToken, Long> {

    List<UserThemeToken> findByUsername(String username);

    List<UserThemeToken> findByUsernameAndMode(String username, String mode);

    @Transactional(transactionManager = "commonTransactionManager")
    void deleteByUsername(String username);

    @Transactional(transactionManager = "commonTransactionManager")
    void deleteByUsernameAndMode(String username, String mode);
}
