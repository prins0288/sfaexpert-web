package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TENANT repository. Notice: NO @Qualifier anywhere. It is bound to the primary
 * tenant EntityManagerFactory by living under in.opt.sfa.tenant.repository, and it
 * routes to whichever tenant is active on the current request.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
