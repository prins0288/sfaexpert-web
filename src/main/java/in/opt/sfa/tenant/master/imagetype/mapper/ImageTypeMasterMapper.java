package in.opt.sfa.tenant.master.imagetype.mapper;

import in.opt.sfa.tenant.master.imagetype.dto.ImageTypeMasterDto;
import in.opt.sfa.tenant.master.imagetype.entity.ImageTypeMaster;
import org.springframework.stereotype.Component;

@Component
public class ImageTypeMasterMapper {

    public ImageTypeMasterDto toDto(ImageTypeMaster e) {
        ImageTypeMasterDto d = new ImageTypeMasterDto();
        d.setId(e.getId());
        d.setImageTypeCode(e.getImageTypeCode());
        d.setImageTypeName(e.getImageTypeName());
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
    public void applyTo(ImageTypeMaster target, ImageTypeMasterDto d) {
        target.setImageTypeCode(d.getImageTypeCode());
        target.setImageTypeName(d.getImageTypeName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
