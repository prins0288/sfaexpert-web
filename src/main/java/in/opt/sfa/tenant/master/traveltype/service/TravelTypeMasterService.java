package in.opt.sfa.tenant.master.traveltype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.traveltype.dto.TravelTypeMasterDto;
import in.opt.sfa.tenant.master.traveltype.entity.TravelTypeMaster;
import in.opt.sfa.tenant.master.traveltype.mapper.TravelTypeMasterMapper;
import in.opt.sfa.tenant.master.traveltype.repository.TravelTypeMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see TravelTypeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class TravelTypeMasterService {

    private final TravelTypeMasterRepository travelTypes;
    private final TravelTypeMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public TravelTypeMasterService(TravelTypeMasterRepository travelTypes, TravelTypeMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.travelTypes = travelTypes;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<TravelTypeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return travelTypes.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public TravelTypeMasterDto get(Long id) {
        TravelTypeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public TravelTypeMasterDto save(TravelTypeMasterDto form) {
        TravelTypeMaster target = form.getId() != null ? find(form.getId()) : new TravelTypeMaster();
        form.setTravelTypeCode(Strings.isBlank(form.getTravelTypeCode()) ? null : form.getTravelTypeCode().trim());
        form.setTravelTypeName(Strings.isBlank(form.getTravelTypeName()) ? null : form.getTravelTypeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(travelTypes.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<TravelTypeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (TravelTypeMasterDto f : forms) {
            String code = Strings.isBlank(f.getTravelTypeCode()) ? null : f.getTravelTypeCode().trim();
            String name = Strings.isBlank(f.getTravelTypeName()) ? null : f.getTravelTypeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || travelTypes.findByTravelTypeCode(code).isPresent()
                    || travelTypes.findByTravelTypeName(name).isPresent()) {
                skipped++;
                continue;
            }
            TravelTypeMaster t = new TravelTypeMaster();
            t.setTravelTypeCode(code);
            t.setTravelTypeName(name);
            t.setShortName(f.getShortName());
            t.setDescription(f.getDescription());
            t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            t.setIsActive(Boolean.TRUE);
            travelTypes.save(t);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public TravelTypeMasterDto updateStatus(Long id, boolean isActive) {
        TravelTypeMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(travelTypes.save(t));
    }

    private TravelTypeMaster find(Long id) {
        return travelTypes.findById(id).orElseThrow(() -> new IllegalStateException("Travel type not found: " + id));
    }
}
