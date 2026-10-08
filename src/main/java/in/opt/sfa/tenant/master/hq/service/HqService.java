package in.opt.sfa.tenant.master.hq.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.hq.dto.HqDto;
import in.opt.sfa.tenant.master.hq.entity.Hq;
import in.opt.sfa.tenant.master.hq.mapper.HqMapper;
import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.hqgroup.entity.HqGroupMaster;
import in.opt.sfa.tenant.master.hqgroup.repository.HqGroupMasterRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HqService {

    private final HqRepository hqs;
    private final StateRepository states;
    private final HqGroupMasterRepository hqGroups;
    private final HqMapper mapper;

    public HqService(HqRepository hqs, StateRepository states, HqGroupMasterRepository hqGroups, HqMapper mapper) {
        this.hqs = hqs;
        this.states = states;
        this.hqGroups = hqGroups;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<HqDto> list() {
        return hqs.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public HqDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public HqDto save(HqDto form) {
        if (form.getIsActive() == null) form.setIsActive(true);
        return mapper.toDto(hqs.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<HqDto> forms) {
        int saved = 0, skipped = 0;
        for (HqDto f : forms) {
            if (Strings.isBlank(f.getHqCode()) || Strings.isBlank(f.getHqName())
                    || hqs.findByHqCode(f.getHqCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setHqCode(f.getHqCode().trim());
            f.setHqName(f.getHqName().trim());
            f.setIsActive(true);
            hqs.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public HqDto updateStatus(Long oid, boolean isActive) {
        Hq h = find(oid);
        h.setIsActive(isActive);
        return mapper.toDto(hqs.save(h));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("HQ",
                List.of("hq_code", "hq_name", "state", "hq_group"),
                List.of("HQ-XXX", "Sample HQ", "MH", "Thane"));
    }

    /** Bulk upload = upsert by hq_code; state / hq_group resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("hq_code"), name = row.get("hq_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            Hq h = hqs.findByHqCode(code.trim()).orElseGet(Hq::new);
            boolean isNew = h.getOid() == null;
            h.setHqCode(code.trim());
            h.setHqName(name.trim());
            h.setStateOid(resolveState(row.get("state")));
            h.setHqGroupId(resolveHqGroup(row.get("hq_group")));
            h.setIsActive(true);
            hqs.save(h);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Hq find(Long oid) {
        return hqs.findById(oid).orElseThrow(() -> new IllegalStateException("HQ not found: " + oid));
    }

    private Long resolveState(String v) {
        return Strings.isBlank(v) ? null
                : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }

    private Long resolveHqGroup(String v) {
        return Strings.isBlank(v) ? null
                : hqGroups.findByGroupCode(v.trim()).map(HqGroupMaster::getId)
                        .or(() -> hqGroups.findByGroupName(v.trim()).map(HqGroupMaster::getId))
                        .orElse(null);
    }
}
