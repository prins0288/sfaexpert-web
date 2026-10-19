package in.opt.sfa.tenant.master.area.mapper;

import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.entity.Area;
import org.springframework.stereotype.Component;

@Component
public class AreaMapper {

    /** Stored fields only — names, rights and routes are filled by AreaService. */
    public AreaDto toDto(Area e) {
        AreaDto d = new AreaDto();
        d.setOid(e.getOid());
        d.setAreaCode(e.getAreaCode());
        d.setAreaName(e.getAreaName());
        d.setCity(e.getCity());
        d.setStateOid(e.getStateOid());
        d.setHqOid(e.getHqOid());
        d.setEmpId(e.getEmpId());
        d.setPincode(e.getPincode());
        d.setAreaType(e.getAreaType());
        d.setDistanceKm(e.getDistanceKm());
        d.setStatus(e.getStatus());
        d.setStateName(e.getStateName());
        d.setHqName(e.getHqName());
        d.setEmpName(e.getEmpName());
        d.setRoutes(e.getRoutes());
        d.setApprovalStatus(e.getApprovalStatus());
        d.setManagerApprovedOn(e.getManagerApprovedOn());
        d.setApprovedOn(e.getApprovedOn());
        d.setRejectReason(e.getRejectReason());
        d.setDeleteRequested(e.getDeleteRequested());
        d.setDeleteStatus(e.getDeleteStatus());
        d.setDeleteRequestReason(e.getDeleteRequestReason());
        return d;
    }
}
