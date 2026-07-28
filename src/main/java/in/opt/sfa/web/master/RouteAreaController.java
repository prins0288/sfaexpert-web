package in.opt.sfa.web.master;

import in.opt.sfa.tenant.entity.RouteAreaMap;
import in.opt.sfa.tenant.repository.RouteAreaMapRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Route&ndash;Area mapping master — maps one route to many areas with visit sequence. */
@Tag(name = "Route-Area Mapping", description = "Map areas to routes")
@RestController
@RequestMapping("/api/master/route-area")
public class RouteAreaController {

    public record MapRequest(Long routeOid, List<Long> areaOids, Integer startSequence) {}

    private final RouteAreaMapRepository maps;

    public RouteAreaController(RouteAreaMapRepository maps) {
        this.maps = maps;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<RouteAreaMap> list() {
        return maps.findAllByOrderByOidAsc();
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(@RequestBody MapRequest req) {
        if (req.routeOid() == null || req.areaOids() == null || req.areaOids().isEmpty()) {
            throw new IllegalStateException("Select a route and at least one area");
        }
        int seq = req.startSequence() != null ? req.startSequence() : 1;
        int mapped = 0;
        for (Long areaOid : req.areaOids()) {
            RouteAreaMap m = maps.findByRouteOidAndAreaOid(req.routeOid(), areaOid)
                    .orElseGet(RouteAreaMap::new);
            m.setRouteOid(req.routeOid());
            m.setAreaOid(areaOid);
            m.setVisitSequence(seq++);
            m.setStatus("Y");
            maps.save(m);
            mapped++;
        }
        return Map.of("mapped", mapped);
    }

    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public RouteAreaMap status(@PathVariable Long oid, @RequestParam String status) {
        RouteAreaMap m = maps.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Mapping not found: " + oid));
        m.setStatus(status);
        return maps.save(m);
    }
}
