package in.opt.sfa.tenant.master.zone.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.division.repository.DivisionRepository;
import in.opt.sfa.tenant.master.zone.dto.ZoneDto;
import in.opt.sfa.tenant.master.zone.entity.Zone;
import in.opt.sfa.tenant.master.zone.mapper.ZoneMapper;
import in.opt.sfa.tenant.master.zone.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ZoneRepository zones;
    private final DivisionRepository divisions;
    private final ZoneMapper mapper;

    public ZoneService(ZoneRepository zones, DivisionRepository divisions, ZoneMapper mapper) {
        this.zones = zones;
        this.divisions = divisions;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ZoneDto> list() {
        return zones.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ZoneDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ZoneDto save(ZoneDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(zones.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<ZoneDto> forms) {
        int saved = 0, skipped = 0;
        for (ZoneDto f : forms) {
            if (Strings.isBlank(f.getZoneCode()) || Strings.isBlank(f.getZoneName())
                    || zones.findByZoneCode(f.getZoneCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setZoneCode(f.getZoneCode().trim());
            f.setZoneName(f.getZoneName().trim());
            f.setStatus("Y");
            zones.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ZoneDto updateStatus(Long oid, String status) {
        Zone z = find(oid);
        z.setStatus(status);
        return mapper.toDto(zones.save(z));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Zone",
                List.of("zone_code", "zone_name", "division"),
                List.of("ZN-XXX", "Sample Zone", "DIV-XXX"));
    }

    /** Bulk upload = upsert by zone_code; division resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("zone_code"), name = row.get("zone_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            Zone z = zones.findByZoneCode(code.trim()).orElseGet(Zone::new);
            boolean isNew = z.getOid() == null;
            z.setZoneCode(code.trim());
            z.setZoneName(name.trim());
            z.setDivisionOid(resolveDivision(row.get("division")));
            z.setStatus("Y");
            zones.save(z);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Zone find(Long oid) {
        return zones.findById(oid).orElseThrow(() -> new IllegalStateException("Zone not found: " + oid));
    }

    private Long resolveDivision(String v) {
        return Strings.isBlank(v) ? null
                : divisions.findFirstByDivisionCodeOrDivisionName(v.trim(), v.trim()).map(d -> d.getOid()).orElse(null);
    }
}
