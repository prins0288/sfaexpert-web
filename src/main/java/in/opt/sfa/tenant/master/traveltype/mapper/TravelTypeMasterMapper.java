package in.opt.sfa.tenant.master.traveltype.mapper;

import in.opt.sfa.tenant.master.traveltype.dto.TravelTypeMasterDto;
import in.opt.sfa.tenant.master.traveltype.entity.TravelTypeMaster;
import org.springframework.stereotype.Component;

@Component
public class TravelTypeMasterMapper {

    public TravelTypeMasterDto toDto(TravelTypeMaster e) {
        TravelTypeMasterDto d = new TravelTypeMasterDto();
        d.setId(e.getId());
        d.setTravelTypeCode(e.getTravelTypeCode());
        d.setTravelTypeName(e.getTravelTypeName());
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
    public void applyTo(TravelTypeMaster target, TravelTypeMasterDto d) {
        target.setTravelTypeCode(d.getTravelTypeCode());
        target.setTravelTypeName(d.getTravelTypeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
