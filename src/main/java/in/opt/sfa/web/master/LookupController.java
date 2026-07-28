package in.opt.sfa.web.master;

import in.opt.sfa.tenant.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * One call returns every dropdown list the master pages need, already filtered
 * to active (status='Y') rows and sorted. Tenant-routed automatically.
 */
@Tag(name = "Lookups", description = "Dropdown lists for the master forms")
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    public record Item(Long oid, String label) {}

    private final DivisionRepository divisions;
    private final ZoneRepository zones;
    private final StateRepository states;
    private final HqRepository hqs;
    private final DegreeRepository degrees;
    private final SpecialityRepository specialities;
    private final CategoryRepository categories;
    private final ClientTypeRepository clientTypes;
    private final RouteRepository routes;
    private final AreaRepository areas;
    private final DesignationRepository designations;
    private final DistrictRepository districts;

    public LookupController(DivisionRepository divisions, ZoneRepository zones, StateRepository states,
                            HqRepository hqs, DegreeRepository degrees, SpecialityRepository specialities,
                            CategoryRepository categories, ClientTypeRepository clientTypes,
                            RouteRepository routes, AreaRepository areas,
                            DesignationRepository designations, DistrictRepository districts) {
        this.divisions = divisions;
        this.zones = zones;
        this.states = states;
        this.hqs = hqs;
        this.degrees = degrees;
        this.specialities = specialities;
        this.categories = categories;
        this.clientTypes = clientTypes;
        this.routes = routes;
        this.areas = areas;
        this.designations = designations;
        this.districts = districts;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> all() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("divisions", divisions.findByStatusOrderByDivisionNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDivisionName())).toList());
        res.put("zones", zones.findByStatusOrderByZoneNameAsc("Y").stream()
                .map(z -> new Item(z.getOid(), z.getZoneName())).toList());
        res.put("states", states.findByStatusOrderByStateNameAsc("Y").stream()
                .map(s -> new Item(s.getOid(), s.getStateName())).toList());
        res.put("hqs", hqs.findByStatusOrderByHqNameAsc("Y").stream()
                .map(h -> new Item(h.getOid(), h.getHqName())).toList());
        res.put("degrees", degrees.findByStatusOrderByDegreeNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDegreeName())).toList());
        res.put("specialities", specialities.findByStatusOrderBySpecialityNameAsc("Y").stream()
                .map(s -> new Item(s.getOid(), s.getSpecialityName())).toList());
        res.put("categories", categories.findByStatusOrderByCategoryNameAsc("Y").stream()
                .map(c -> new Item(c.getOid(), c.getCategoryName())).toList());
        res.put("clientTypes", clientTypes.findByStatusOrderByTypeNameAsc("Y").stream()
                .map(t -> new Item(t.getOid(), t.getTypeName())).toList());
        res.put("routes", routes.findByStatusOrderByRouteNameAsc("Y").stream()
                .map(r -> new Item(r.getOid(), r.getRouteName())).toList());
        res.put("areas", areas.findByStatusOrderByAreaNameAsc("Y").stream()
                .map(a -> new Item(a.getOid(), a.getAreaName())).toList());
        res.put("designations", designations.findByStatusOrderByDesignationNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDesignationName())).toList());
        res.put("districts", districts.findByStatusOrderByDistrictNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDistrictName())).toList());
        return res;
    }
}
