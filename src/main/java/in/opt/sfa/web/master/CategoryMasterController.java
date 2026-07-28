package in.opt.sfa.web.master;

import in.opt.sfa.tenant.entity.CategoryMaster;
import in.opt.sfa.tenant.repository.CategoryMasterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Category master — production-style CRUD with audit + soft-delete. Tenant-routed.
 *
 * On update the existing row is LOADED and only the editable fields are copied,
 * so the server-managed audit columns (created_at / created_by) are preserved;
 * the entity's @PreUpdate then stamps updated_at / updated_by.
 */
@RestController
@RequestMapping("/api/master/category-master")
@Tag(name = "Category Master", description = "Categories with icon, soft-delete status and audit trail")
public class CategoryMasterController {

    private final CategoryMasterRepository categories;

    public CategoryMasterController(CategoryMasterRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    @Operation(summary = "List all categories (active + inactive)")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<CategoryMaster> list() {
        return categories.findAllByOrderByOidAsc();
    }

    @GetMapping("/{oid}")
    @Operation(summary = "Get one category by id")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public CategoryMaster get(@PathVariable Long oid) {
        return categories.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Category not found: " + oid));
    }

    @PostMapping
    @Operation(summary = "Create or update a category",
            description = "Send oid to update, omit it to create. Audit fields are set by the server.")
    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryMaster save(@RequestBody CategoryMaster form) {
        CategoryMaster target = form.getOid() != null
                ? categories.findById(form.getOid())
                    .orElseThrow(() -> new IllegalStateException("Category not found: " + form.getOid()))
                : new CategoryMaster();

        target.setCategoryCode(trim(form.getCategoryCode()));
        target.setCategoryName(trim(form.getCategoryName()));
        target.setDescription(form.getDescription());
        target.setIcon(form.getIcon());
        if (form.getStatus() != null) {
            target.setStatus(form.getStatus());
        }
        return categories.save(target);   // @PrePersist / @PreUpdate fill the audit columns
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create categories (blank / duplicate code rows are skipped)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(@RequestBody List<CategoryMaster> forms) {
        int saved = 0, skipped = 0;
        for (CategoryMaster f : forms) {
            String code = trim(f.getCategoryCode());
            if (isBlank(code) || isBlank(f.getCategoryName())
                    || categories.findByCategoryCode(code).isPresent()) {
                skipped++;
                continue;
            }
            CategoryMaster c = new CategoryMaster();
            c.setCategoryCode(code);
            c.setCategoryName(trim(f.getCategoryName()));
            c.setDescription(f.getDescription());
            c.setIcon(f.getIcon());
            c.setStatus(Boolean.TRUE);
            categories.save(c);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @PostMapping("/{oid}/status")
    @Operation(summary = "Activate / deactivate a category (status = true or false)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryMaster status(@PathVariable Long oid, @RequestParam boolean status) {
        CategoryMaster c = categories.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Category not found: " + oid));
        c.setStatus(status);
        return categories.save(c);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
}
