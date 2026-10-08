package in.opt.sfa.tenant.master.route.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Numbers;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.route.dto.RouteDto;
import in.opt.sfa.tenant.master.route.entity.Route;
import in.opt.sfa.tenant.master.route.mapper.RouteMapper;
import in.opt.sfa.tenant.master.route.repository.RouteRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RouteService {

    private final RouteRepository routes;
    private final HqRepository hqs;
    private final StateRepository states;
    private final RouteMapper mapper;

    public RouteService(RouteRepository routes, HqRepository hqs, StateRepository states, RouteMapper mapper) {
        this.routes = routes;
        this.hqs = hqs;
        this.states = states;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<RouteDto> list() {
        return routes.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public RouteDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RouteDto save(RouteDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(routes.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<RouteDto> forms) {
        int saved = 0, skipped = 0;
        for (RouteDto f : forms) {
            if (Strings.isBlank(f.getRouteCode()) || Strings.isBlank(f.getRouteName())
                    || routes.findByRouteCode(f.getRouteCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setRouteCode(f.getRouteCode().trim());
            f.setRouteName(f.getRouteName().trim());
            f.setStatus("Y");
            routes.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RouteDto updateStatus(Long oid, String status) {
        Route r = find(oid);
        r.setStatus(status);
        return mapper.toDto(routes.save(r));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Route",
                List.of("route_code", "route_name", "hq", "state", "distance_km", "description"),
                List.of("RT-XXX-01", "Sample Beat", "HQ-MUM", "MH", "10", "notes"));
    }

    /** Bulk upload = upsert by route_code; hq/state resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("route_code"), name = row.get("route_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            Route r = routes.findByRouteCode(code.trim()).orElseGet(Route::new);
            boolean isNew = r.getOid() == null;
            r.setRouteCode(code.trim());
            r.setRouteName(name.trim());
            r.setHqOid(resolveHq(row.get("hq")));
            r.setStateOid(resolveState(row.get("state")));
            r.setDistanceKm(Numbers.toBigDecimal(row.get("distance_km")));
            r.setDescription(row.get("description"));
            r.setStatus("Y");
            routes.save(r);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Route find(Long oid) {
        return routes.findById(oid).orElseThrow(() -> new IllegalStateException("Route not found: " + oid));
    }

    private Long resolveHq(String v) {
        return Strings.isBlank(v) ? null : hqs.findFirstByHqCodeOrHqName(v.trim(), v.trim()).map(h -> h.getOid()).orElse(null);
    }
    private Long resolveState(String v) {
        return Strings.isBlank(v) ? null : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }
}
