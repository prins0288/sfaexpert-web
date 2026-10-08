package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.UserTablePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Every lookup is scoped by company_code — there is deliberately no finder without it. */
public interface UserTablePreferenceRepository extends JpaRepository<UserTablePreference, Long> {
    Optional<UserTablePreference> findByCompanyCodeAndScreenKey(String companyCode, String screenKey);
    long deleteByCompanyCodeAndScreenKey(String companyCode, String screenKey);
}
