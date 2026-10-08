package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** COMMON repository (bound to the common EntityManagerFactory by package). */
public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {

    Optional<TenantConfig> findByCompanyCode(String companyCode);
}
