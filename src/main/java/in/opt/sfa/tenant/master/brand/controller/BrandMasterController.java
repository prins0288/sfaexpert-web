package in.opt.sfa.tenant.master.brand.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.brand.dto.BrandMasterDto;
import in.opt.sfa.tenant.master.brand.service.BrandMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Brand master — Masters > Catalog > Brand. Tenant-routed. */
@RestController
@RequestMapping("/api/master/brand-master")
@Tag(name = "Brand Master", description = "Brands with an optional image (thumbnail auto-created)")
public class BrandMasterController {

    private final BrandMasterService service;

    public BrandMasterController(BrandMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all brands (active + inactive)")
    public List<BrandMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one brand by id")
    public BrandMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("BRAND_SAVE")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create or update a brand, with an optional image",
            description = "Multipart: part 'data' = the brand JSON (send id to update; removeImage=true drops the image), "
                    + "part 'image' = optional image file (PNG/JPG/GIF/WebP/BMP). Thumbnail is created automatically.")
    public BrandMasterDto save(@RequestPart("data") BrandMasterDto form,
                               @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        return service.save(form, image);
    }

    @RequiresPermission("BRAND_SAVE")
    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create brands (no images; invalid / duplicate rows are skipped with a reason)")
    public Map<String, Object> saveMultiple(@RequestBody List<BrandMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @RequiresPermission("BRAND_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a brand (status = true or false)")
    public BrandMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
