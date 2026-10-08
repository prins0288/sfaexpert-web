package in.opt.sfa.tenant.master.bank.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.bank.dto.BankMasterDto;
import in.opt.sfa.tenant.master.bank.entity.BankMaster;
import in.opt.sfa.tenant.master.bank.mapper.BankMasterMapper;
import in.opt.sfa.tenant.master.bank.repository.BankMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see BankMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class BankMasterService {

    private final BankMasterRepository banks;
    private final BankMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public BankMasterService(BankMasterRepository banks, BankMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.banks = banks;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<BankMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return banks.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public BankMasterDto get(Long id) {
        BankMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public BankMasterDto save(BankMasterDto form) {
        BankMaster target = form.getId() != null ? find(form.getId()) : new BankMaster();
        form.setBankCode(Strings.isBlank(form.getBankCode()) ? null : form.getBankCode().trim());
        form.setBankName(Strings.isBlank(form.getBankName()) ? null : form.getBankName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(banks.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<BankMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (BankMasterDto f : forms) {
            String code = Strings.isBlank(f.getBankCode()) ? null : f.getBankCode().trim();
            String name = Strings.isBlank(f.getBankName()) ? null : f.getBankName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || banks.findByBankCode(code).isPresent()
                    || banks.findByBankName(name).isPresent()) {
                skipped++;
                continue;
            }
            BankMaster b = new BankMaster();
            b.setBankCode(code);
            b.setBankName(name);
            b.setShortName(f.getShortName());
            b.setDescription(f.getDescription());
            b.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            b.setIsActive(Boolean.TRUE);
            banks.save(b);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public BankMasterDto updateStatus(Long id, boolean isActive) {
        BankMaster b = find(id);
        b.setIsActive(isActive);
        return mapper.toDto(banks.save(b));
    }

    private BankMaster find(Long id) {
        return banks.findById(id).orElseThrow(() -> new IllegalStateException("Bank not found: " + id));
    }
}
