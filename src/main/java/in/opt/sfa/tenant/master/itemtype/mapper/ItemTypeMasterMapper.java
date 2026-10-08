package in.opt.sfa.tenant.master.itemtype.mapper;

import in.opt.sfa.tenant.master.itemtype.dto.ItemTypeMasterDto;
import in.opt.sfa.tenant.master.itemtype.entity.ItemTypeMaster;
import org.springframework.stereotype.Component;

@Component
public class ItemTypeMasterMapper {

    public ItemTypeMasterDto toDto(ItemTypeMaster e) {
        ItemTypeMasterDto d = new ItemTypeMasterDto();
        d.setId(e.getId());
        d.setItemTypeCode(e.getItemTypeCode());
        d.setItemTypeName(e.getItemTypeName());
        d.setShortName(e.getShortName());
        d.setDescription(e.getDescription());
        d.setDisplayOrder(e.getDisplayOrder());
        d.setIsActive(e.getIsActive());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    /** Copies only the editable fields onto an existing/new entity — audit columns are server-managed (see entity lifecycle callbacks). */
    public void applyTo(ItemTypeMaster target, ItemTypeMasterDto d) {
        target.setItemTypeCode(d.getItemTypeCode());
        target.setItemTypeName(d.getItemTypeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
