package in.opt.sfa.tenant.master.meetingtype.mapper;

import in.opt.sfa.tenant.master.meetingtype.dto.MeetingTypeMasterDto;
import in.opt.sfa.tenant.master.meetingtype.entity.MeetingTypeMaster;
import org.springframework.stereotype.Component;

@Component
public class MeetingTypeMasterMapper {

    public MeetingTypeMasterDto toDto(MeetingTypeMaster e) {
        MeetingTypeMasterDto d = new MeetingTypeMasterDto();
        d.setId(e.getId());
        d.setMeetingTypeCode(e.getMeetingTypeCode());
        d.setMeetingTypeName(e.getMeetingTypeName());
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
    public void applyTo(MeetingTypeMaster target, MeetingTypeMasterDto d) {
        target.setMeetingTypeCode(d.getMeetingTypeCode());
        target.setMeetingTypeName(d.getMeetingTypeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
