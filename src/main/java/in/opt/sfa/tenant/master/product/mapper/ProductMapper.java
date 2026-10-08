package in.opt.sfa.tenant.master.product.mapper;

import in.opt.sfa.tenant.master.product.dto.ProductDto;
import in.opt.sfa.tenant.master.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDto toDto(Product e) {
        ProductDto d = new ProductDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setPrice(e.getPrice());
        return d;
    }

    public Product toEntity(ProductDto d) {
        Product e = new Product();
        e.setId(d.getId());
        e.setName(d.getName());
        e.setPrice(d.getPrice());
        return e;
    }
}
