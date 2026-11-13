package in.opt.sfa.tenant.master.item.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.item.dto.ItemMasterDto;
import in.opt.sfa.tenant.master.item.dto.ItemRateDto;
import in.opt.sfa.tenant.master.item.service.ItemMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Item master — Masters > Catalog > Item. Tenant-routed. */
@RestController
@RequestMapping("/api/master/item-master")
@Tag(name = "Item Master", description = "Items with state / date-wise rates and multiple salts (composition)")
public class ItemMasterController {

    private final ItemMasterService service;

    public ItemMasterController(ItemMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all items (active + inactive) with today's rate; rates / composition lines not included")
    public List<ItemMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "One item with all its rates and composition lines")
    public ItemMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/molecules")
    @Operation(summary = "Salt / molecule names already used — suggestions while typing a composition")
    public List<String> molecules() {
        return service.moleculeNames();
    }

    @GetMapping("/sample-candidates")
    @Operation(summary = "Create Multiple Sample: active Product items with pack size, today's PTS and their existing sample code")
    public List<Map<String, Object>> sampleCandidates() {
        return service.sampleCandidates();
    }

    @RequiresPermission("ITEM_SAVE")
    @PostMapping("/create-samples")
    @Operation(summary = "Create one Sample item per ticked product (item_code_main = product code); no duplicates",
            description = "Body: { items: [{productId, packSize, pts}], stateIds: [..] } — no states = All-India rate. "
                    + "A product that already has a sample only gets the missing state rates.")
    public Map<String, Object> createSamples(@RequestBody in.opt.sfa.tenant.master.item.dto.SampleCreateRequest req) {
        return service.createSamples(req);
    }

    @GetMapping("/next-code")
    @Operation(summary = "Preview of the code the next new item of this type gets (PD01 / GP01 / IP01 / SP01 ...); taken for real on save")
    public Map<String, String> nextCode(@RequestParam Long itemTypeId) {
        return Map.of("itemCode", service.nextCode(itemTypeId));
    }

    @GetMapping("/{id}/rate")
    @Operation(summary = "Rate of an item for a state on a date (the state's own rate, else All India); empty = no rate")
    public ItemRateDto rate(@PathVariable Long id, @RequestParam(required = false) Long stateId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.rateOn(id, stateId, date);
    }

    @RequiresPermission("ITEM_SAVE")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create or update an item with its rates and composition, and an optional image",
            description = "Multipart: part 'data' = item JSON (send id + version to update; rates / compositions lists replace "
                    + "the stored ones, null = unchanged), part 'image' = optional image file (the file name is kept).")
    public ItemMasterDto save(@RequestPart("data") ItemMasterDto form,
                              @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        return service.save(form, image);
    }

    @RequiresPermission("ITEM_STATUS")
    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate an item (status = true or false)")
    public ItemMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }

    @RequiresPermission("ITEM_UPLOAD")
    @GetMapping("/template")
    @Operation(summary = "Download the bulk template (sheets Items, Rates, Composition, Help)")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("item_master_template.xlsx", service.template());
    }

    @RequiresPermission("ITEM_UPLOAD")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk upload Items / Rates / Composition (each sheet optional; bad rows skipped with the reason)")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
