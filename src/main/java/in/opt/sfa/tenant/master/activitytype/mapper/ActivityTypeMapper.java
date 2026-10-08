package in.opt.sfa.tenant.master.activitytype.mapper;

import in.opt.sfa.tenant.master.activitytype.dto.ActivityTypeDto;
import in.opt.sfa.tenant.master.activitytype.entity.ActivityType;
import org.springframework.stereotype.Component;

@Component
public class ActivityTypeMapper {

    public ActivityTypeDto toDto(ActivityType e) {
        ActivityTypeDto d = new ActivityTypeDto();
        d.setOid(e.getOid());
        d.setTypeCode(e.getTypeCode());
        d.setTypeName(e.getTypeName());
        d.setClientTypeId(e.getClientTypeId());
        d.setIsActive(e.getIsActive());
        return d;
    }

    public ActivityType toEntity(ActivityTypeDto d) {
        ActivityType e = new ActivityType();
        e.setOid(d.getOid());
        e.setTypeCode(d.getTypeCode());
        e.setTypeName(d.getTypeName());
        e.setClientTypeId(d.getClientTypeId());
        e.setIsActive(d.getIsActive());
        return e;
    }
}
