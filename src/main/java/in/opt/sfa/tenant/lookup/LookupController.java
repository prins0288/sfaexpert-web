package in.opt.sfa.tenant.lookup;

import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.hqgroup.repository.HqGroupMasterRepository;
import in.opt.sfa.tenant.repository.*;
import in.opt.sfa.tenant.master.country.repository.CountryRepository;
import in.opt.sfa.tenant.master.degree.repository.DegreeMasterRepository;
import in.opt.sfa.tenant.master.speciality.repository.SpecialityMasterRepository;
import in.opt.sfa.tenant.master.category.repository.CategoryMasterRepository;
import in.opt.sfa.tenant.master.division.repository.DivisionRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import in.opt.sfa.tenant.master.zone.repository.ZoneRepository;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import in.opt.sfa.tenant.master.route.repository.RouteRepository;
import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.bank.repository.BankMasterRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * One call returns every dropdown list the master pages need, already filtered
 * to active (status='Y') rows and sorted. Tenant-routed automatically.
 */
@Tag(name = "Lookups", description = "Dropdown lists for the master forms")
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    /** empLevel is only meaningful for designations; omitted (null) for every other lookup.
     *  oid is Object because most masters key by a Long surrogate id, but employees now key
     *  by emp_id (a String business key) since the emp_detail v2 schema. */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public record Item(Object oid, String label, Integer empLevel) {
        public Item(Object oid, String label) { this(oid, label, null); }
    }

    private final DivisionRepository divisions;
    private final ZoneRepository zones;
    private final StateRepository states;
    private final HqRepository hqs;
    private final HqGroupMasterRepository hqGroups;
    private final DegreeMasterRepository degrees;
    private final SpecialityMasterRepository specialities;
    private final CategoryMasterRepository categories;
    private final ClientTypeRepository clientTypes;
    private final RouteRepository routes;
    private final AreaRepository areas;
    private final DesignationRepository designations;
    private final DistrictRepository districts;
    private final EmpDetailRepository employees;
    private final CountryRepository countries;
    private final BankMasterRepository banks;

    public LookupController(DivisionRepository divisions, ZoneRepository zones, StateRepository states,
                            HqRepository hqs, HqGroupMasterRepository hqGroups, DegreeMasterRepository degrees, SpecialityMasterRepository specialities,
                            CategoryMasterRepository categories, ClientTypeRepository clientTypes,
                            RouteRepository routes, AreaRepository areas,
                            DesignationRepository designations, DistrictRepository districts,
                            EmpDetailRepository employees, CountryRepository countries,
                            BankMasterRepository banks) {
        this.divisions = divisions;
        this.zones = zones;
        this.states = states;
        this.hqs = hqs;
        this.hqGroups = hqGroups;
        this.degrees = degrees;
        this.specialities = specialities;
        this.categories = categories;
        this.clientTypes = clientTypes;
        this.routes = routes;
        this.areas = areas;
        this.designations = designations;
        this.districts = districts;
        this.employees = employees;
        this.countries = countries;
        this.banks = banks;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> all(@RequestParam(name = "keys", required = false) String keys) {
        // Optional ?keys=a,b,c — query and return ONLY those lists, so a page loads
        // exactly what its dropdowns need (e.g. State Master -> ?keys=countries)
        // instead of all 15. No keys = every list (backward compatible).
        Set<String> want = (keys == null || keys.isBlank()) ? null
                : Arrays.stream(keys.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        Map<String, Object> res = new LinkedHashMap<>();
        if (want(want, "divisions")) res.put("divisions", divisions.findByStatusOrderByDivisionNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDivisionName())).toList());
        if (want(want, "zones")) res.put("zones", zones.findByStatusOrderByZoneNameAsc("Y").stream()
                .map(z -> new Item(z.getOid(), z.getZoneName())).toList());
        if (want(want, "states")) res.put("states", states.findByStatusOrderByStateNameAsc("Y").stream()
                .map(s -> new Item(s.getOid(), s.getStateName())).toList());
        if (want(want, "hqs")) res.put("hqs", hqs.findByIsActiveOrderByHqNameAsc(true).stream()
                .map(h -> new Item(h.getOid(), h.getHqName())).toList());
        if (want(want, "hqGroups")) res.put("hqGroups", hqGroups.findByIsActiveOrderByDisplayOrderAscGroupNameAsc(true).stream()
                .map(g -> new Item(g.getId(), g.getGroupName())).toList());
        if (want(want, "degrees")) res.put("degrees", degrees.findByIsActiveOrderByDisplayOrderAscDegreeNameAsc(true).stream()
                .map(d -> new Item(d.getId(), d.getDegreeName())).toList());
        if (want(want, "specialities")) res.put("specialities", specialities.findByStatusOrderBySpecialityNameAsc(true).stream()
                .map(s -> new Item(s.getOid(), s.getSpecialityName())).toList());
        if (want(want, "categories")) res.put("categories", categories.findByStatusOrderByCategoryNameAsc(true).stream()
                .map(c -> new Item(c.getOid(), c.getCategoryName())).toList());
        if (want(want, "clientTypes")) res.put("clientTypes", clientTypes.findByIsActiveOrderByTypeNameAsc(true).stream()
                .map(t -> new Item(t.getOid(), t.getTypeName())).toList());
        if (want(want, "routes")) res.put("routes", routes.findByStatusOrderByRouteNameAsc("Y").stream()
                .map(r -> new Item(r.getOid(), r.getRouteName())).toList());
        if (want(want, "areas")) res.put("areas", areas.findByStatusOrderByAreaNameAsc("Y").stream()
                .map(a -> new Item(a.getOid(), a.getAreaName())).toList());
        if (want(want, "designations")) res.put("designations", designations.findByStatusTrueOrderByDesignationNameAsc().stream()
                .map(d -> new Item(d.getOid(), d.getDesignationName(), d.getEmpLevel())).toList());
        if (want(want, "districts")) res.put("districts", districts.findByStatusOrderByDistrictNameAsc("Y").stream()
                .map(d -> new Item(d.getOid(), d.getDistrictName())).toList());
        if (want(want, "employees")) res.put("employees", employees.findByActiveTrueOrderByEmpNameAsc().stream()
                .map(e -> new Item(e.getEmpId(), e.getEmpName())).toList());
        if (want(want, "banks")) res.put("banks", banks.findByIsActiveOrderByDisplayOrderAscBankNameAsc(true).stream()
                .map(bk -> new Item(bk.getId(), bk.getBankName())).toList());
        if (want(want, "countries")) res.put("countries", countries.findByStatusOrderByCountryNameAsc(Boolean.TRUE).stream()
                .map(c -> new Item(c.getOid(), c.getCountryName())).toList());
        return res;
    }

    /** true when the caller wants this list (no ?keys filter = wants everything). */
    private static boolean want(Set<String> keys, String key) {
        return keys == null || keys.contains(key);
    }
}
