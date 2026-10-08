package in.opt.sfa.tenant.master.hqgroup.mapper;

import in.opt.sfa.tenant.master.hqgroup.dto.HqGroupMasterDto;
import in.opt.sfa.tenant.master.hqgroup.entity.HqGroupMaster;
import org.springframework.stereotype.Component;

@Component
public class HqGroupMasterMapper {

    public HqGroupMasterDto toDto(HqGroupMaster e) {
        HqGroupMasterDto d = new HqGroupMasterDto();
        d.setId(e.getId());
        d.setGroupCode(e.getGroupCode());
        d.setGroupName(e.getGroupName());
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
    public void applyTo(HqGroupMaster target, HqGroupMasterDto d) {
        target.setGroupCode(d.getGroupCode());
        target.setGroupName(d.getGroupName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
