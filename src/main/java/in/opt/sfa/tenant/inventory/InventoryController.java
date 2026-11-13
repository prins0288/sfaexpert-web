package in.opt.sfa.tenant.inventory;

import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Inventory", description = "Stock & sales of a party / stockist, month wise (party_sales_parent / party_sales_child)")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    private final FileUploadService uploads;

    public InventoryController(InventoryService service, FileUploadService uploads) { this.service = service; this.uploads = uploads; }

    @GetMapping("/config") public Map<String, Object> config() { return service.config(); }
    @PostMapping("/config") public Map<String, Object> saveConfig(@RequestBody Map<String, Object> b) { service.saveConfig(b); return Map.of("saved", true); }
    @GetMapping("/parties") public List<Map<String, Object>> parties(@RequestParam String empId) { return service.parties(empId); }

    @GetMapping("/list")
    public List<Map<String, Object>> list(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year, @RequestParam(required = false) Long stateId,
                                          @RequestParam(required = false) String empId, @RequestParam(required = false) Long partyId) { return service.list(month, year, stateId, empId, partyId); }

    @GetMapping("/entry")
    public Map<String, Object> entry(@RequestParam String empId, @RequestParam long partyId, @RequestParam int month, @RequestParam int year) { return service.entry(empId, partyId, month, year); }

    @PostMapping("/save") public Map<String, Object> save(@RequestBody Map<String, Object> b) { return service.save(b); }
    @PostMapping("/{id}/delete") public Map<String, Object> delete(@PathVariable long id) { return Map.of("deleted", service.delete(id)); }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        StoredFile f = uploads.upload("inventory", file, FileUploadService.Kind.DOCUMENT, true);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("path", f.path()); m.put("name", f.originalName()); m.put("type", f.type());
        return m;
    }
}
