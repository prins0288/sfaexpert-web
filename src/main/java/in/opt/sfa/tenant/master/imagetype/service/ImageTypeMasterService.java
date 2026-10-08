package in.opt.sfa.tenant.master.imagetype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.imagetype.dto.ImageTypeMasterDto;
import in.opt.sfa.tenant.master.imagetype.entity.ImageTypeMaster;
import in.opt.sfa.tenant.master.imagetype.mapper.ImageTypeMasterMapper;
import in.opt.sfa.tenant.master.imagetype.repository.ImageTypeMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see ImageTypeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class ImageTypeMasterService {

    private final ImageTypeMasterRepository imageTypes;
    private final ImageTypeMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public ImageTypeMasterService(ImageTypeMasterRepository imageTypes, ImageTypeMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.imageTypes = imageTypes;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ImageTypeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return imageTypes.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ImageTypeMasterDto get(Long id) {
        ImageTypeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ImageTypeMasterDto save(ImageTypeMasterDto form) {
        ImageTypeMaster target = form.getId() != null ? find(form.getId()) : new ImageTypeMaster();
        form.setImageTypeCode(Strings.isBlank(form.getImageTypeCode()) ? null : form.getImageTypeCode().trim());
        form.setImageTypeName(Strings.isBlank(form.getImageTypeName()) ? null : form.getImageTypeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(imageTypes.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<ImageTypeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (ImageTypeMasterDto f : forms) {
            String code = Strings.isBlank(f.getImageTypeCode()) ? null : f.getImageTypeCode().trim();
            String name = Strings.isBlank(f.getImageTypeName()) ? null : f.getImageTypeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || imageTypes.findByImageTypeCode(code).isPresent()
                    || imageTypes.findByImageTypeName(name).isPresent()) {
                skipped++;
                continue;
            }
            ImageTypeMaster t = new ImageTypeMaster();
            t.setImageTypeCode(code);
            t.setImageTypeName(name);
            t.setShortName(f.getShortName());
            t.setDescription(f.getDescription());
            t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            t.setIsActive(Boolean.TRUE);
            imageTypes.save(t);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ImageTypeMasterDto updateStatus(Long id, boolean isActive) {
        ImageTypeMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(imageTypes.save(t));
    }

    private ImageTypeMaster find(Long id) {
        return imageTypes.findById(id).orElseThrow(() -> new IllegalStateException("Image type not found: " + id));
    }
}
