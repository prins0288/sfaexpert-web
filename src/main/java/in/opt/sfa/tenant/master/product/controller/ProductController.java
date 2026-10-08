package in.opt.sfa.tenant.master.product.controller;

import in.opt.sfa.tenant.master.product.dto.ProductDto;
import in.opt.sfa.tenant.master.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Demo endpoints that read/write the ACTIVE tenant's database. The tenant is
 * already bound by TenantAuthFilter, so ProductRepository (no qualifier) just
 * works and hits the right DB.
 */
@Tag(name = "Products", description = "Product master (tenant-routed)")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDto> list() {
        return service.list();
    }

    @PostMapping
    public ProductDto create(@RequestBody ProductDto product) {
        return service.create(product);
    }
}
