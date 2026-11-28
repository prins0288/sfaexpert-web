package in.opt.sfa.tenant.master.degree.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.degree.dto.DegreeMasterDto;
import in.opt.sfa.tenant.master.degree.entity.DegreeMaster;
import in.opt.sfa.tenant.master.degree.mapper.DegreeMasterMapper;
import in.opt.sfa.tenant.master.degree.repository.DegreeMasterRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see DegreeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class DegreeMasterService {

    private final DegreeMasterRepository degrees;
    private final DegreeMasterMapper mapper;
    private final EmpNameResolver employeeNames;

    public DegreeMasterService(DegreeMasterRepository degrees, DegreeMasterMapper mapper, EmpNameResolver employeeNames) {
        this.degrees = degrees;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DegreeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return degrees.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DegreeMasterDto get(Long id) {
        DegreeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DegreeMasterDto save(DegreeMasterDto form) {
        DegreeMaster target = form.getId() != null ? find(form.getId()) : new DegreeMaster();
        form.setDegreeCode(Strings.isBlank(form.getDegreeCode()) ? null : form.getDegreeCode().trim());
        form.setDegreeName(Strings.isBlank(form.getDegreeName()) ? null : form.getDegreeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(degrees.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<DegreeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (DegreeMasterDto f : forms) {
            String code = Strings.isBlank(f.getDegreeCode()) ? null : f.getDegreeCode().trim();
            String name = Strings.isBlank(f.getDegreeName()) ? null : f.getDegreeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || degrees.findByDegreeCode(code).isPresent()
                    || degrees.findByDegreeName(name).isPresent()) {
                skipped++;
                continue;
            }
            DegreeMaster d = new DegreeMaster();
            d.setDegreeCode(code);
            d.setDegreeName(name);
            d.setShortName(f.getShortName());
            d.setDescription(f.getDescription());
            d.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            d.setIsActive(Boolean.TRUE);
            degrees.save(d);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DegreeMasterDto updateStatus(Long id, boolean isActive) {
        DegreeMaster d = find(id);
        d.setIsActive(isActive);
        return mapper.toDto(degrees.save(d));
    }

    private DegreeMaster find(Long id) {
        return degrees.findById(id).orElseThrow(() -> new IllegalStateException("Degree not found: " + id));
    }
}
