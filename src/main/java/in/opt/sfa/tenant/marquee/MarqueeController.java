package in.opt.sfa.tenant.marquee;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Marquee", description = "Scrolling message in the page header")
@RestController
@RequestMapping("/api/marquee")
public class MarqueeController {

    private final MarqueeService service;

    public MarqueeController(MarqueeService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    @Operation(summary = "The marquee messages for the signed-in employee, placeholders already filled")
    public List<Map<String, Object>> mine() { return service.mine(); }

    @GetMapping("/config")
    public Map<String, Object> config() { return service.config(); }

    @GetMapping
    public List<Map<String, Object>> list() { return service.list(); }

    @PostMapping
    public Map<String, Object> save(@RequestBody Map<String, Object> body) { return service.save(body); }

    @PostMapping("/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> body) {
        return Map.of("deleted", service.delete(((List<Object>) body.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList()));
    }
}
