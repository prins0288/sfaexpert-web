package in.opt.sfa.tenant.master.itemtype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.itemtype.dto.ItemTypeMasterDto;
import in.opt.sfa.tenant.master.itemtype.entity.ItemTypeMaster;
import in.opt.sfa.tenant.master.itemtype.mapper.ItemTypeMasterMapper;
import in.opt.sfa.tenant.master.itemtype.repository.ItemTypeMasterRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see ItemTypeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class ItemTypeMasterService {

    private final ItemTypeMasterRepository itemTypes;
    private final ItemTypeMasterMapper mapper;
    private final EmpNameResolver employeeNames;

    public ItemTypeMasterService(ItemTypeMasterRepository itemTypes, ItemTypeMasterMapper mapper, EmpNameResolver employeeNames) {
        this.itemTypes = itemTypes;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ItemTypeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return itemTypes.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ItemTypeMasterDto get(Long id) {
        ItemTypeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ItemTypeMasterDto save(ItemTypeMasterDto form) {
        ItemTypeMaster target = form.getId() != null ? find(form.getId()) : new ItemTypeMaster();
        form.setItemTypeCode(Strings.isBlank(form.getItemTypeCode()) ? null : form.getItemTypeCode().trim());
        form.setItemTypeName(Strings.isBlank(form.getItemTypeName()) ? null : form.getItemTypeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(itemTypes.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<ItemTypeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (ItemTypeMasterDto f : forms) {
            String code = Strings.isBlank(f.getItemTypeCode()) ? null : f.getItemTypeCode().trim();
            String name = Strings.isBlank(f.getItemTypeName()) ? null : f.getItemTypeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || itemTypes.findByItemTypeCode(code).isPresent()
                    || itemTypes.findByItemTypeName(name).isPresent()) {
                skipped++;
                continue;
            }
            ItemTypeMaster t = new ItemTypeMaster();
            t.setItemTypeCode(code);
            t.setItemTypeName(name);
            t.setShortName(f.getShortName());
            t.setDescription(f.getDescription());
            t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            t.setIsActive(Boolean.TRUE);
            itemTypes.save(t);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ItemTypeMasterDto updateStatus(Long id, boolean isActive) {
        ItemTypeMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(itemTypes.save(t));
    }

    private ItemTypeMaster find(Long id) {
        return itemTypes.findById(id).orElseThrow(() -> new IllegalStateException("Item type not found: " + id));
    }
}
