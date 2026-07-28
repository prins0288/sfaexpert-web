package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/** TENANT-db repository for the single company_profile row (id = 1). */
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Integer> {
}
