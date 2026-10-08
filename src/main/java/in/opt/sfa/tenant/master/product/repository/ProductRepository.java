package in.opt.sfa.tenant.master.product.repository;

import in.opt.sfa.tenant.master.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TENANT repository. Notice: NO @Qualifier anywhere. It is bound to the primary
 * tenant EntityManagerFactory by living under in.opt.sfa.tenant.master (see
 * TenantDataSourceConfig), and it routes to whichever tenant is active on the
 * current request.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
