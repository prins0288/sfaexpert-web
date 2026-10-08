package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.UserTablePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Every lookup is scoped by company_code — there is deliberately no finder without it. */
public interface UserTablePreferenceRepository extends JpaRepository<UserTablePreference, Long> {
    Optional<UserTablePreference> findByCompanyCodeAndUserIdAndScreenKey(String companyCode, String userId, String screenKey);
    long deleteByCompanyCodeAndUserIdAndScreenKey(String companyCode, String userId, String screenKey);
}
