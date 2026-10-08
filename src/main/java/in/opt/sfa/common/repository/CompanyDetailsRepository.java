package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.CompanyDetails;
import org.springframework.data.jpa.repository.JpaRepository;

/** COMMON-db repository for the per-tenant company_details row (keyed by company_code). */
public interface CompanyDetailsRepository extends JpaRepository<CompanyDetails, String> {
}
