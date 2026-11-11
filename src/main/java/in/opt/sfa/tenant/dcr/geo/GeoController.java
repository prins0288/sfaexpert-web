package in.opt.sfa.tenant.dcr.geo;

import in.opt.sfa.tenant.team.TeamScope;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "DCR geo fencing", description = "Location check at client check-in, retag requests and their approval")
@RestController
@RequestMapping("/api/dcr/geo")
public class GeoController {

    private final GeoService geo;

    public GeoController(GeoService geo) { this.geo = geo; }

    private static Double d(Map<String, Object> b, String k) { Object v = b.get(k); return v == null || v.toString().isBlank() ? null : Double.valueOf(v.toString()); }
    private static String s(Map<String, Object> b, String k) { Object v = b == null ? null : b.get(k); return v == null ? null : v.toString(); }

    @GetMapping("/config") public Map<String, Object> config() { return geo.config(); }
    @GetMapping("/map-config") public Map<String, Object> mapConfig() { return geo.mapConfig(); }
    @PostMapping("/check")
    public Map<String, Object> check(@RequestBody Map<String, Object> b) { return geo.checkListed(TeamScope.me(), Long.parseLong(s(b, "clientId")), d(b, "lat"), d(b, "lng")); }
    @PostMapping("/retag")
    public Map<String, Object> retag(@RequestBody Map<String, Object> b) { return geo.requestRetag(Long.parseLong(s(b, "clientId")), d(b, "lat"), d(b, "lng"), s(b, "reason")); }
    @GetMapping("/retag") public List<Map<String, Object>> list(@RequestParam(defaultValue = "false") boolean all) { return geo.toApprove(all); }
    @PostMapping("/retag/{id}/approve") public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { geo.decide(id, true, s(b, "remarks")); return Map.of("ok", true); }
    @PostMapping("/retag/{id}/reject") public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b) { geo.decide(id, false, s(b, "remarks")); return Map.of("ok", true); }
}
