package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.CompanyTablePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Every lookup is scoped by company_code — there is deliberately no finder without it. */
public interface CompanyTablePreferenceRepository extends JpaRepository<CompanyTablePreference, Long> {
    Optional<CompanyTablePreference> findByCompanyCodeAndScreenKey(String companyCode, String screenKey);
    long deleteByCompanyCodeAndScreenKey(String companyCode, String screenKey);
}
