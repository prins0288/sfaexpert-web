package in.opt.sfa.tenant.master.document.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.document.dto.DocumentMasterDto;
import in.opt.sfa.tenant.master.document.entity.DocumentMaster;
import in.opt.sfa.tenant.master.document.mapper.DocumentMasterMapper;
import in.opt.sfa.tenant.master.document.repository.DocumentMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see DocumentMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class DocumentMasterService {

    private final DocumentMasterRepository repo;
    private final DocumentMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public DocumentMasterService(DocumentMasterRepository repo, DocumentMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.repo = repo;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DocumentMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return repo.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DocumentMasterDto get(Long id) {
        DocumentMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DocumentMasterDto save(DocumentMasterDto form) {
        DocumentMaster target = form.getId() != null ? find(form.getId()) : new DocumentMaster();
        form.setDocumentCode(Strings.isBlank(form.getDocumentCode()) ? null : form.getDocumentCode().trim());
        form.setDocumentName(Strings.isBlank(form.getDocumentName()) ? null : form.getDocumentName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(repo.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<DocumentMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (DocumentMasterDto f : forms) {
            String code = Strings.isBlank(f.getDocumentCode()) ? null : f.getDocumentCode().trim();
            String name = Strings.isBlank(f.getDocumentName()) ? null : f.getDocumentName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || repo.findByDocumentCode(code).isPresent()
                    || repo.findByDocumentName(name).isPresent()) {
                skipped++;
                continue;
            }
            DocumentMaster t = new DocumentMaster();
            t.setDocumentCode(code);
            t.setDocumentName(name);
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
    public DocumentMasterDto updateStatus(Long id, boolean isActive) {
        DocumentMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(repo.save(t));
    }

    private DocumentMaster find(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalStateException("Document not found: " + id));
    }
}
