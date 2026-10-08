package in.opt.sfa.tenant.master.sponsorshiptype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.sponsorshiptype.dto.SponsorshipTypeMasterDto;
import in.opt.sfa.tenant.master.sponsorshiptype.entity.SponsorshipTypeMaster;
import in.opt.sfa.tenant.master.sponsorshiptype.mapper.SponsorshipTypeMasterMapper;
import in.opt.sfa.tenant.master.sponsorshiptype.repository.SponsorshipTypeMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see SponsorshipTypeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class SponsorshipTypeMasterService {

    private final SponsorshipTypeMasterRepository repo;
    private final SponsorshipTypeMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public SponsorshipTypeMasterService(SponsorshipTypeMasterRepository repo, SponsorshipTypeMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.repo = repo;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<SponsorshipTypeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return repo.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public SponsorshipTypeMasterDto get(Long id) {
        SponsorshipTypeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SponsorshipTypeMasterDto save(SponsorshipTypeMasterDto form) {
        SponsorshipTypeMaster target = form.getId() != null ? find(form.getId()) : new SponsorshipTypeMaster();
        form.setSponsorshipTypeCode(Strings.isBlank(form.getSponsorshipTypeCode()) ? null : form.getSponsorshipTypeCode().trim());
        form.setSponsorshipTypeName(Strings.isBlank(form.getSponsorshipTypeName()) ? null : form.getSponsorshipTypeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(repo.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<SponsorshipTypeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (SponsorshipTypeMasterDto f : forms) {
            String code = Strings.isBlank(f.getSponsorshipTypeCode()) ? null : f.getSponsorshipTypeCode().trim();
            String name = Strings.isBlank(f.getSponsorshipTypeName()) ? null : f.getSponsorshipTypeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || repo.findBySponsorshipTypeCode(code).isPresent()
                    || repo.findBySponsorshipTypeName(name).isPresent()) {
                skipped++;
                continue;
            }
            SponsorshipTypeMaster t = new SponsorshipTypeMaster();
            t.setSponsorshipTypeCode(code);
            t.setSponsorshipTypeName(name);
            t.setShortName(f.getShortName());
            t.setDescription(f.getDescription());
            t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            t.setIsActive(Boolean.TRUE);
            repo.save(t);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SponsorshipTypeMasterDto updateStatus(Long id, boolean isActive) {
        SponsorshipTypeMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(repo.save(t));
    }

    private SponsorshipTypeMaster find(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalStateException("Sponsorship Type not found: " + id));
    }
}
