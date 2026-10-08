package in.opt.sfa.tenant.master.imagetype.controller;

import in.opt.sfa.tenant.master.imagetype.dto.ImageTypeMasterDto;
import in.opt.sfa.tenant.master.imagetype.service.ImageTypeMasterService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Image-type master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/image-type-master")
@Tag(name = "Image Type Master", description = "Field-image classifications (Meter Reading, Shop Photo, Hospital Photo...) with short name, display order, soft-delete status and audit trail")
public class ImageTypeMasterController {

    private final ImageTypeMasterService service;

    public ImageTypeMasterController(ImageTypeMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all image types (active + inactive)")
    public List<ImageTypeMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one image type by id")
    public ImageTypeMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("IMAGE_TYPE_SAVE")
    @PostMapping
    @Operation(summary = "Create or update an image type",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public ImageTypeMasterDto save(@RequestBody ImageTypeMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("IMAGE_TYPE_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create image types (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<ImageTypeMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("IMAGE_TYPE_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate an image type (status = true or false)")
    public ImageTypeMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
