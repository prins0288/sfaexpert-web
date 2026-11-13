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
    private final InventoryReportService report;
    private final FileUploadService uploads;

    public InventoryController(InventoryService service, InventoryReportService report, FileUploadService uploads) { this.service = service; this.report = report; this.uploads = uploads; }

    @GetMapping("/config") public Map<String, Object> config() { return service.config(); }
    @PostMapping("/config") public Map<String, Object> saveConfig(@RequestBody Map<String, Object> b) { service.saveConfig(b); return Map.of("saved", true); }
    @GetMapping("/parties") public List<Map<String, Object>> parties(@RequestParam String empId) { return service.parties(empId); }

    @GetMapping("/list")
    public List<Map<String, Object>> list(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year, @RequestParam(required = false) Long stateId,
                                          @RequestParam(required = false) String empId, @RequestParam(required = false) Long partyId) { return service.list(month, year, stateId, empId, partyId); }

    @GetMapping("/entry")
    public Map<String, Object> entry(@RequestParam String empId, @RequestParam long partyId, @RequestParam int month, @RequestParam int year) { return service.entry(empId, partyId, month, year); }

    @PostMapping("/save") public Map<String, Object> save(@RequestBody Map<String, Object> b) throws Exception { return service.save(b); }
    @PostMapping("/config/field/{code}/delete") public Map<String, Object> deleteField(@PathVariable String code) { service.deleteField(code); return Map.of("deleted", true); }
    @PostMapping("/{id}/delete") public Map<String, Object> delete(@PathVariable long id) { return Map.of("deleted", service.delete(id)); }

    // ---- company stock report (items x state / HQ / designation / employee / party x O P S FG R E C) ----
    private static List<Integer> ints(String csv) { return csv == null || csv.isBlank() ? List.of() : java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(Integer::valueOf).toList(); }
    private InventoryReportService.Q q(Integer year, String months, Long stateId, Long hqId, Integer designationId, String empId, Long partyId, String groupBy, String types, String priceType) {
        return new InventoryReportService.Q(year, ints(months), stateId, hqId, designationId, empId, partyId, groupBy, types == null || types.isBlank() ? List.of() : java.util.Arrays.asList(types.split(",")), priceType);
    }
    @GetMapping("/report/parties") public List<Map<String, Object>> partyOptions(@RequestParam(required = false) String empId, @RequestParam(required = false) Long stateId) { return report.partyOptions(empId, stateId); }
    @GetMapping("/report")
    public Map<String, Object> report(@RequestParam(required = false) Integer year, @RequestParam(required = false) String months, @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long hqId,
                                      @RequestParam(required = false) Integer designationId, @RequestParam(required = false) String empId, @RequestParam(required = false) Long partyId,
                                      @RequestParam(defaultValue = "PARTY") String groupBy, @RequestParam(required = false) String types, @RequestParam(defaultValue = "APPLIED") String priceType) {
        return report.pivot(q(year, months, stateId, hqId, designationId, empId, partyId, groupBy, types, priceType));
    }
    @GetMapping("/report/excel")
    public org.springframework.http.ResponseEntity<byte[]> excel(@RequestParam(required = false) Integer year, @RequestParam(required = false) String months, @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long hqId,
                                      @RequestParam(required = false) Integer designationId, @RequestParam(required = false) String empId, @RequestParam(required = false) Long partyId,
                                      @RequestParam(defaultValue = "PARTY") String groupBy, @RequestParam(required = false) String types, @RequestParam(defaultValue = "APPLIED") String priceType, @RequestParam(defaultValue = "BOTH") String view) throws Exception {
        byte[] b = report.excel(q(year, months, stateId, hqId, designationId, empId, partyId, groupBy, types, priceType), view);
        return org.springframework.http.ResponseEntity.ok().header("Content-Disposition", "attachment; filename=company-stock.xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(b);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        StoredFile f = uploads.upload("inventory", file, FileUploadService.Kind.DOCUMENT, true);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("path", f.path()); m.put("name", f.originalName()); m.put("type", f.type());
        return m;
    }
}
