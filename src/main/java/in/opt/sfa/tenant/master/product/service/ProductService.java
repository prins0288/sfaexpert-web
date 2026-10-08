package in.opt.sfa.tenant.master.product.service;

import in.opt.sfa.tenant.master.product.dto.ProductDto;
import in.opt.sfa.tenant.master.product.mapper.ProductMapper;
import in.opt.sfa.tenant.master.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository products;
    private final ProductMapper mapper;

    public ProductService(ProductRepository products, ProductMapper mapper) {
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ProductDto> list() {
        return products.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ProductDto create(ProductDto form) {
        return mapper.toDto(products.save(mapper.toEntity(form)));
    }
}
