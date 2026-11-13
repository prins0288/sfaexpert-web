package in.opt.sfa.tenant.dcr.submit.controller;

import in.opt.sfa.tenant.dcr.submit.service.DcrPermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "DCR permissions", description = "Back-date submit and DCR edit permissions per employee (admin)")
@RestController
@RequestMapping("/api/dcr/permissions")
public class DcrPermissionController {
    private final DcrPermissionService service;
    public DcrPermissionController(DcrPermissionService service) { this.service = service; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String type, @RequestParam(required = false) String empId, @RequestParam(defaultValue = "false") boolean expired) {
        return service.list(type, empId, expired);
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, Object> grant(@RequestBody Map<String, Object> b) {
        List<String> emps = ((List<Object>) b.getOrDefault("empIds", List.of())).stream().map(Object::toString).toList();
        int n = service.grant(String.valueOf(b.get("type")), emps, d(b, "dcrFrom"), d(b, "dcrTo"), d(b, "allowFrom"), d(b, "allowTo"), b.get("reason") == null ? null : b.get("reason").toString());
        return Map.of("created", n);
    }

    @PostMapping("/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> b) {
        return Map.of("deleted", service.remove(((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList()));
    }

    private static LocalDate d(Map<String, Object> b, String k) { Object v = b.get(k); return v == null || v.toString().isBlank() ? null : LocalDate.parse(v.toString()); }
}
