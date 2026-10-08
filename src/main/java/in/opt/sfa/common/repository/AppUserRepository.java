package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * COMMON repository. It is bound to the "common" EntityManagerFactory purely by
 * living under package in.opt.sfa.common.repository (see CommonDataSourceConfig's
 * @EnableJpaRepositories). No @Qualifier needed on the interface itself.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmpIdAndCompanyCode(String empId, String companyCode);
    List<AppUser> findByCompanyCode(String companyCode);
}
