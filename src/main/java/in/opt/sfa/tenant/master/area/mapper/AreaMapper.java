package in.opt.sfa.tenant.master.area.mapper;

import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.entity.Area;
import org.springframework.stereotype.Component;

@Component
public class AreaMapper {

    public AreaDto toDto(Area e) {
        AreaDto d = new AreaDto();
        d.setOid(e.getOid());
        d.setAreaCode(e.getAreaCode());
        d.setAreaName(e.getAreaName());
        d.setCity(e.getCity());
        d.setStateOid(e.getStateOid());
        d.setHqOid(e.getHqOid());
        d.setPincode(e.getPincode());
        d.setAreaType(e.getAreaType());
        d.setDistanceKm(e.getDistanceKm());
        d.setStatus(e.getStatus());
        d.setStateName(e.getStateName());
        d.setHqName(e.getHqName());
        d.setRoutes(e.getRoutes());
        return d;
    }

    public Area toEntity(AreaDto d) {
        Area e = new Area();
        e.setOid(d.getOid());
        e.setAreaCode(d.getAreaCode());
        e.setAreaName(d.getAreaName());
        e.setCity(d.getCity());
        e.setStateOid(d.getStateOid());
        e.setHqOid(d.getHqOid());
        e.setPincode(d.getPincode());
        e.setAreaType(d.getAreaType());
        e.setDistanceKm(d.getDistanceKm());
        e.setStatus(d.getStatus());
        return e;
    }
}
