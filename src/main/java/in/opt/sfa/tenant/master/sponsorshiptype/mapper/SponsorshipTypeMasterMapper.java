package in.opt.sfa.tenant.master.sponsorshiptype.mapper;

import in.opt.sfa.tenant.master.sponsorshiptype.dto.SponsorshipTypeMasterDto;
import in.opt.sfa.tenant.master.sponsorshiptype.entity.SponsorshipTypeMaster;
import org.springframework.stereotype.Component;

@Component
public class SponsorshipTypeMasterMapper {

    public SponsorshipTypeMasterDto toDto(SponsorshipTypeMaster e) {
        SponsorshipTypeMasterDto d = new SponsorshipTypeMasterDto();
        d.setId(e.getId());
        d.setSponsorshipTypeCode(e.getSponsorshipTypeCode());
        d.setSponsorshipTypeName(e.getSponsorshipTypeName());
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
    public void applyTo(SponsorshipTypeMaster target, SponsorshipTypeMasterDto d) {
        target.setSponsorshipTypeCode(d.getSponsorshipTypeCode());
        target.setSponsorshipTypeName(d.getSponsorshipTypeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
