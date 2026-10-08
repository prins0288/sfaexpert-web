package in.opt.sfa.tenant.master.routearea.controller;

import in.opt.sfa.tenant.master.routearea.dto.RouteAreaMapDto;
import in.opt.sfa.tenant.master.routearea.dto.RouteAreaMapRequest;
import in.opt.sfa.tenant.master.routearea.service.RouteAreaMapService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Route&ndash;Area mapping master — maps one route to many areas with visit sequence. */
@Tag(name = "Route-Area Mapping", description = "Map areas to routes")
@RestController
@RequestMapping("/api/master/route-area")
public class RouteAreaController {

    private final RouteAreaMapService service;

    public RouteAreaController(RouteAreaMapService service) {
        this.service = service;
    }

    @GetMapping
    public List<RouteAreaMapDto> list() {
        return service.list();
    }

    @PostMapping
    public Map<String, Object> save(@RequestBody RouteAreaMapRequest req) {
        return service.save(req);
    }

    @PostMapping("/{oid}/status")
    public RouteAreaMapDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }
}
