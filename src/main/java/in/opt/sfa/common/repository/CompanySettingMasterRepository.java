package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.CompanySettingMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * COMMON-db repository for company settings. Every method is tenant-scoped —
 * callers pass the current tenant id (via {@link in.opt.sfa.common.service.CompanySettingStore})
 * so one company never touches another's rows.
 */
public interface CompanySettingMasterRepository extends JpaRepository<CompanySettingMaster, Long> {

    List<CompanySettingMaster> findByCompanyCode(String companyCode);

    Optional<CompanySettingMaster> findByCompanyCodeAndSettingKey(String companyCode, String settingKey);
}
