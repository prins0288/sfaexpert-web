package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.AppBranding;
import org.springframework.data.jpa.repository.JpaRepository;

/** COMMON-db repository for the single app_branding row (id = 1). */
public interface AppBrandingRepository extends JpaRepository<AppBranding, Integer> {
}
