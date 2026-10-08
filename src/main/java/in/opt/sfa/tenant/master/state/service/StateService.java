package in.opt.sfa.tenant.master.state.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.country.repository.CountryRepository;
import in.opt.sfa.tenant.master.state.dto.StateDto;
import in.opt.sfa.tenant.master.state.entity.State;
import in.opt.sfa.tenant.master.state.mapper.StateMapper;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StateService {

    private final StateRepository states;
    private final CountryRepository countries;
    private final StateMapper mapper;

    public StateService(StateRepository states, CountryRepository countries, StateMapper mapper) {
        this.states = states;
        this.countries = countries;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<StateDto> list() {
        return states.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public StateDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public StateDto save(StateDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(states.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<StateDto> forms) {
        int saved = 0, skipped = 0;
        for (StateDto f : forms) {
            if (Strings.isBlank(f.getStateCode()) || Strings.isBlank(f.getStateName())
                    || states.findByStateCode(f.getStateCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setStateCode(f.getStateCode().trim());
            f.setStateName(f.getStateName().trim());
            f.setStatus("Y");
            states.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public StateDto updateStatus(Long oid, String status) {
        State s = find(oid);
        s.setStatus(status);
        return mapper.toDto(states.save(s));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("State",
                List.of("state_code", "state_name", "country"),
                List.of("ST-XXX", "Sample State", "IN"));
    }

    /** Bulk upload = upsert by state_code; country resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("state_code"), name = row.get("state_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            State s = states.findByStateCode(code.trim()).orElseGet(State::new);
            boolean isNew = s.getOid() == null;
            s.setStateCode(code.trim());
            s.setStateName(name.trim());
            s.setCountryOid(resolveCountry(row.get("country")));
            s.setStatus("Y");
            states.save(s);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private State find(Long oid) {
        return states.findById(oid).orElseThrow(() -> new IllegalStateException("State not found: " + oid));
    }

    private Long resolveCountry(String v) {
        return Strings.isBlank(v) ? null
                : countries.findFirstByCountryCodeOrCountryName(v.trim(), v.trim()).map(c -> c.getOid()).orElse(null);
    }
}
