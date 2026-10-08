package in.opt.sfa.tenant.master.degree.mapper;

import in.opt.sfa.tenant.master.degree.dto.DegreeMasterDto;
import in.opt.sfa.tenant.master.degree.entity.DegreeMaster;
import org.springframework.stereotype.Component;

@Component
public class DegreeMasterMapper {

    public DegreeMasterDto toDto(DegreeMaster e) {
        DegreeMasterDto d = new DegreeMasterDto();
        d.setId(e.getId());
        d.setDegreeCode(e.getDegreeCode());
        d.setDegreeName(e.getDegreeName());
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
    public void applyTo(DegreeMaster target, DegreeMasterDto d) {
        target.setDegreeCode(d.getDegreeCode());
        target.setDegreeName(d.getDegreeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
