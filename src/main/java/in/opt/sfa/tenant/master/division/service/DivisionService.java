package in.opt.sfa.tenant.master.division.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.division.dto.DivisionDto;
import in.opt.sfa.tenant.master.division.entity.Division;
import in.opt.sfa.tenant.master.division.mapper.DivisionMapper;
import in.opt.sfa.tenant.master.division.repository.DivisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DivisionService {

    private final DivisionRepository divisions;
    private final DivisionMapper mapper;

    public DivisionService(DivisionRepository divisions, DivisionMapper mapper) {
        this.divisions = divisions;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DivisionDto> list() {
        return divisions.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DivisionDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DivisionDto save(DivisionDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(divisions.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<DivisionDto> forms) {
        int saved = 0, skipped = 0;
        for (DivisionDto f : forms) {
            if (Strings.isBlank(f.getDivisionCode()) || Strings.isBlank(f.getDivisionName())
                    || divisions.findByDivisionCode(f.getDivisionCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setDivisionCode(f.getDivisionCode().trim());
            f.setDivisionName(f.getDivisionName().trim());
            f.setStatus("Y");
            divisions.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DivisionDto updateStatus(Long oid, String status) {
        Division d = find(oid);
        d.setStatus(status);
        return mapper.toDto(divisions.save(d));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Division",
                List.of("division_code", "division_name"),
                List.of("DIV-XXX", "Sample Division"));
    }

    /** Bulk upload = upsert by division_code. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("division_code"), name = row.get("division_name");
            if (Strings.isBlank(code) || Strings.isBlank(name)) { skipped++; continue; }
            Division d = divisions.findByDivisionCode(code.trim()).orElseGet(Division::new);
            boolean isNew = d.getOid() == null;
            d.setDivisionCode(code.trim());
            d.setDivisionName(name.trim());
            d.setStatus("Y");
            divisions.save(d);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Division find(Long oid) {
        return divisions.findById(oid).orElseThrow(() -> new IllegalStateException("Division not found: " + oid));
    }
}
