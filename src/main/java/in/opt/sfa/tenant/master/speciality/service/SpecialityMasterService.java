package in.opt.sfa.tenant.master.speciality.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.speciality.dto.SpecialityMasterDto;
import in.opt.sfa.tenant.master.speciality.entity.SpecialityMaster;
import in.opt.sfa.tenant.master.speciality.mapper.SpecialityMasterMapper;
import in.opt.sfa.tenant.master.speciality.repository.SpecialityMasterRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see SpecialityMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class SpecialityMasterService {

    private final SpecialityMasterRepository specialities;
    private final SpecialityMasterMapper mapper;
    private final EmpNameResolver employeeNames;

    public SpecialityMasterService(SpecialityMasterRepository specialities, SpecialityMasterMapper mapper, EmpNameResolver employeeNames) {
        this.specialities = specialities;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<SpecialityMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return specialities.findAllByOrderByOidAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public SpecialityMasterDto get(Long oid) {
        SpecialityMasterDto d = mapper.toDto(find(oid));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SpecialityMasterDto save(SpecialityMasterDto form) {
        SpecialityMaster target = form.getOid() != null ? find(form.getOid()) : new SpecialityMaster();
        form.setSpecialityCode(Strings.isBlank(form.getSpecialityCode()) ? null : form.getSpecialityCode().trim());
        form.setSpecialityName(Strings.isBlank(form.getSpecialityName()) ? null : form.getSpecialityName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(specialities.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<SpecialityMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (SpecialityMasterDto f : forms) {
            String code = Strings.isBlank(f.getSpecialityCode()) ? null : f.getSpecialityCode().trim();
            if (Strings.isBlank(code) || Strings.isBlank(f.getSpecialityName())
                    || specialities.findBySpecialityCode(code).isPresent()) {
                skipped++;
                continue;
            }
            SpecialityMaster s = new SpecialityMaster();
            s.setSpecialityCode(code);
            s.setSpecialityName(f.getSpecialityName().trim());
            s.setDescription(f.getDescription());
            s.setIcon(f.getIcon());
            s.setStatus(Boolean.TRUE);
            specialities.save(s);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SpecialityMasterDto updateStatus(Long oid, boolean status) {
        SpecialityMaster s = find(oid);
        s.setStatus(status);
        return mapper.toDto(specialities.save(s));
    }

    private SpecialityMaster find(Long oid) {
        return specialities.findById(oid).orElseThrow(() -> new IllegalStateException("Speciality not found: " + oid));
    }
}
