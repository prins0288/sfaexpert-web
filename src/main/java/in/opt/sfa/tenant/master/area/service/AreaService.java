package in.opt.sfa.tenant.master.area.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Numbers;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.entity.Area;
import in.opt.sfa.tenant.master.area.mapper.AreaMapper;
import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.routearea.entity.RouteAreaMap;
import in.opt.sfa.tenant.master.routearea.repository.RouteAreaMapRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AreaService {

    private final AreaRepository areas;
    private final RouteAreaMapRepository maps;
    private final HqRepository hqs;
    private final StateRepository states;
    private final AreaMapper mapper;

    public AreaService(AreaRepository areas, RouteAreaMapRepository maps,
                        HqRepository hqs, StateRepository states, AreaMapper mapper) {
        this.areas = areas;
        this.maps = maps;
        this.hqs = hqs;
        this.states = states;
        this.mapper = mapper;
        
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<AreaDto> list() {
        List<Area> rows = areas.findAllByOrderByOidAsc();
        for (Area a : rows) {
            a.setRoutes(maps.findByAreaOidAndStatus(a.getOid(), "Y").stream()
                    .map(RouteAreaMap::getRouteName)
                    .collect(Collectors.joining(", ")));
        }
        return rows.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public AreaDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto save(AreaDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(areas.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<AreaDto> forms) {
        int saved = 0, skipped = 0;
        for (AreaDto f : forms) {
            if (Strings.isBlank(f.getAreaCode()) || Strings.isBlank(f.getAreaName())
                    || areas.findByAreaCode(f.getAreaCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setAreaCode(f.getAreaCode().trim());
            f.setAreaName(f.getAreaName().trim());
            f.setStatus("Y");
            areas.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto updateStatus(Long oid, String status) {
        Area a = find(oid);
        a.setStatus(status);
        return mapper.toDto(areas.save(a));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Area",
                List.of("area_code", "area_name", "city", "hq", "state", "pincode", "area_type", "distance_km"),
                List.of("AR-XXX", "Sample Area", "Mumbai", "HQ-MUM", "MH", "400001", "Core", "5"));
    }

    /** Bulk upload = upsert by area_code; hq/state resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("area_code"), name = row.get("area_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            Area a = areas.findByAreaCode(code.trim()).orElseGet(Area::new);
            boolean isNew = a.getOid() == null;
            a.setAreaCode(code.trim());
            a.setAreaName(name.trim());
            a.setCity(row.get("city"));
            a.setHqOid(resolveHq(row.get("hq")));
            a.setStateOid(resolveState(row.get("state")));
            a.setPincode(row.get("pincode"));
            a.setAreaType(row.get("area_type"));
            a.setDistanceKm(Numbers.toBigDecimal(row.get("distance_km")));
            a.setStatus("Y");
            areas.save(a);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Area find(Long oid) {
        return areas.findById(oid).orElseThrow(() -> new IllegalStateException("Area not found: " + oid));
    }

    private Long resolveHq(String v) {
        return Strings.isBlank(v) ? null : hqs.findFirstByHqCodeOrHqName(v.trim(), v.trim()).map(h -> h.getOid()).orElse(null);
    }
    private Long resolveState(String v) {
        return Strings.isBlank(v) ? null : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }
}
