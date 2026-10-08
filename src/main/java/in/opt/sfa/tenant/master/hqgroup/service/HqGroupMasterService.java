package in.opt.sfa.tenant.master.hqgroup.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.hqgroup.dto.HqGroupMasterDto;
import in.opt.sfa.tenant.master.hqgroup.entity.HqGroupMaster;
import in.opt.sfa.tenant.master.hqgroup.mapper.HqGroupMasterMapper;
import in.opt.sfa.tenant.master.hqgroup.repository.HqGroupMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see HqGroupMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class HqGroupMasterService {

    private final HqGroupMasterRepository repo;
    private final HqGroupMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public HqGroupMasterService(HqGroupMasterRepository repo, HqGroupMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.repo = repo;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<HqGroupMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return repo.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public HqGroupMasterDto get(Long id) {
        HqGroupMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public HqGroupMasterDto save(HqGroupMasterDto form) {
        HqGroupMaster target = form.getId() != null ? find(form.getId()) : new HqGroupMaster();
        form.setGroupCode(Strings.isBlank(form.getGroupCode()) ? null : form.getGroupCode().trim());
        form.setGroupName(Strings.isBlank(form.getGroupName()) ? null : form.getGroupName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(repo.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<HqGroupMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (HqGroupMasterDto f : forms) {
            String code = Strings.isBlank(f.getGroupCode()) ? null : f.getGroupCode().trim();
            String name = Strings.isBlank(f.getGroupName()) ? null : f.getGroupName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || repo.findByGroupCode(code).isPresent()
                    || repo.findByGroupName(name).isPresent()) {
                skipped++;
                continue;
            }
            HqGroupMaster g = new HqGroupMaster();
            g.setGroupCode(code);
            g.setGroupName(name);
            g.setShortName(f.getShortName());
            g.setDescription(f.getDescription());
            g.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            g.setIsActive(Boolean.TRUE);
            repo.save(g);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public HqGroupMasterDto updateStatus(Long id, boolean isActive) {
        HqGroupMaster g = find(id);
        g.setIsActive(isActive);
        return mapper.toDto(repo.save(g));
    }

    private HqGroupMaster find(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalStateException("HQ group not found: " + id));
    }
}
