package in.opt.sfa.tenant.master.category.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.category.dto.CategoryMasterDto;
import in.opt.sfa.tenant.master.category.entity.CategoryMaster;
import in.opt.sfa.tenant.master.category.mapper.CategoryMasterMapper;
import in.opt.sfa.tenant.master.category.repository.CategoryMasterRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see CategoryMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class CategoryMasterService {

    private final CategoryMasterRepository categories;
    private final CategoryMasterMapper mapper;
    private final EmpNameResolver employeeNames;

    public CategoryMasterService(CategoryMasterRepository categories, CategoryMasterMapper mapper, EmpNameResolver employeeNames) {
        this.categories = categories;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<CategoryMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return categories.findAllByOrderByOidAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public CategoryMasterDto get(Long oid) {
        CategoryMasterDto d = mapper.toDto(find(oid));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryMasterDto save(CategoryMasterDto form) {
        CategoryMaster target = form.getOid() != null ? find(form.getOid()) : new CategoryMaster();
        form.setCategoryCode(Strings.isBlank(form.getCategoryCode()) ? null : form.getCategoryCode().trim());
        form.setCategoryName(Strings.isBlank(form.getCategoryName()) ? null : form.getCategoryName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(categories.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<CategoryMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (CategoryMasterDto f : forms) {
            String code = Strings.isBlank(f.getCategoryCode()) ? null : f.getCategoryCode().trim();
            if (Strings.isBlank(code) || Strings.isBlank(f.getCategoryName())
                    || categories.findByCategoryCode(code).isPresent()) {
                skipped++;
                continue;
            }
            CategoryMaster c = new CategoryMaster();
            c.setCategoryCode(code);
            c.setCategoryName(f.getCategoryName().trim());
            c.setDescription(f.getDescription());
            c.setIcon(f.getIcon());
            c.setStatus(Boolean.TRUE);
            categories.save(c);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryMasterDto updateStatus(Long oid, boolean status) {
        CategoryMaster c = find(oid);
        c.setStatus(status);
        return mapper.toDto(categories.save(c));
    }

    private CategoryMaster find(Long oid) {
        return categories.findById(oid).orElseThrow(() -> new IllegalStateException("Category not found: " + oid));
    }
}
