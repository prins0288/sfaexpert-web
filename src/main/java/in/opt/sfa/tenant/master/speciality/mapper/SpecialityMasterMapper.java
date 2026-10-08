package in.opt.sfa.tenant.master.speciality.mapper;

import in.opt.sfa.tenant.master.speciality.dto.SpecialityMasterDto;
import in.opt.sfa.tenant.master.speciality.entity.SpecialityMaster;
import org.springframework.stereotype.Component;

@Component
public class SpecialityMasterMapper {

    public SpecialityMasterDto toDto(SpecialityMaster e) {
        SpecialityMasterDto d = new SpecialityMasterDto();
        d.setOid(e.getOid());
        d.setSpecialityCode(e.getSpecialityCode());
        d.setSpecialityName(e.getSpecialityName());
        d.setDescription(e.getDescription());
        d.setIcon(e.getIcon());
        d.setStatus(e.getStatus());
        d.setCreatedAt(e.getCreatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }

    /** Copies only the editable fields onto an existing/new entity — audit columns are server-managed (see entity lifecycle callbacks). */
    public void applyTo(SpecialityMaster target, SpecialityMasterDto d) {
        target.setSpecialityCode(d.getSpecialityCode());
        target.setSpecialityName(d.getSpecialityName());
        target.setDescription(d.getDescription());
        target.setIcon(d.getIcon());
        if (d.getStatus() != null) target.setStatus(d.getStatus());
    }
}
