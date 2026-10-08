package in.opt.sfa.tenant.master.category.controller;

import in.opt.sfa.tenant.master.category.dto.CategoryMasterDto;
import in.opt.sfa.tenant.master.category.service.CategoryMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Category master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/category-master")
@Tag(name = "Category Master", description = "Categories with icon, soft-delete status and audit trail")
public class CategoryMasterController {

    private final CategoryMasterService service;

    public CategoryMasterController(CategoryMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all categories (active + inactive)")
    public List<CategoryMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    @Operation(summary = "Get one category by id")
    public CategoryMasterDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    @Operation(summary = "Create or update a category",
            description = "Send oid to update, omit it to create. Audit fields are set by the server.")
    public CategoryMasterDto save(@RequestBody CategoryMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create categories (blank / duplicate code rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<CategoryMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    @Operation(summary = "Activate / deactivate a category (status = true or false)")
    public CategoryMasterDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
