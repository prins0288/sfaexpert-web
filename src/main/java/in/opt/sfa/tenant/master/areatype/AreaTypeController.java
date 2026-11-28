package in.opt.sfa.tenant.master.areatype;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Area type master", description = "HQ / EX / OUT EX / OUT with the km range of each")
@RestController
@RequestMapping("/api/master/area-type")
public class AreaTypeController {
    private final AreaTypeService service;
    public AreaTypeController(AreaTypeService service) { this.service = service; }
    @GetMapping public List<Map<String, Object>> list() { return service.list(); }
    @PostMapping public Map<String, Object> save(@RequestBody Map<String, Object> b) { return service.save(b); }
    @PostMapping("/{id}/delete") public Map<String, Object> delete(@PathVariable long id) { service.delete(id); return Map.of("deleted", true); }
}
