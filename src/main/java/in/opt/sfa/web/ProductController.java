package in.opt.sfa.web;

import in.opt.sfa.tenant.entity.Product;
import in.opt.sfa.tenant.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Demo endpoints that read/write the ACTIVE tenant's database. The tenant is
 * already bound by TenantAuthFilter, so ProductRepository (no qualifier) just
 * works and hits the right DB. Note the tenant transaction manager.
 */
@Tag(name = "Products", description = "Product master (tenant-routed)")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Product> list() {
        return productRepository.findAll();
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Product create(@RequestBody Product product) {
        return productRepository.save(product);
    }
}
