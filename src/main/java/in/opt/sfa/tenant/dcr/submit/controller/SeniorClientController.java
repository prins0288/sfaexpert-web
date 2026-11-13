package in.opt.sfa.tenant.dcr.submit.controller;

import in.opt.sfa.tenant.dcr.submit.service.SeniorClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Senior client access", description = "Clients of the team a senior may check in, with the geo check on / off per manager + client (admin)")
@RestController
@RequestMapping("/api/dcr/senior-clients")
public class SeniorClientController {
    private final SeniorClientService service;
    public SeniorClientController(SeniorClientService service) { this.service = service; }

    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) String empId) { return service.list(empId); }
    @GetMapping("/candidates") public List<Map<String, Object>> candidates(@RequestParam String empId) { return service.candidates(empId); }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, Object> add(@RequestBody Map<String, Object> b) {
        List<Long> ids = ((List<Object>) b.getOrDefault("clientIds", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList();
        return service.add(b.get("empId") == null ? null : b.get("empId").toString(), ids, !Boolean.FALSE.equals(b.get("geoEnabled")) && !"0".equals(String.valueOf(b.get("geoEnabled"))));
    }

    @PostMapping("/{id}/geo")
    public Map<String, Object> geo(@PathVariable long id, @RequestBody Map<String, Object> b) { service.setGeo(id, Boolean.TRUE.equals(b.get("enabled")) || "1".equals(String.valueOf(b.get("enabled")))); return Map.of("ok", true); }

    @PostMapping("/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> b) { return Map.of("deleted", service.remove(((List<Object>) b.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(o.toString())).toList())); }
}
