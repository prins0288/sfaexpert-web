package in.opt.sfa.tenant.master.zone.mapper;

import in.opt.sfa.tenant.master.zone.dto.ZoneDto;
import in.opt.sfa.tenant.master.zone.entity.Zone;
import org.springframework.stereotype.Component;

@Component
public class ZoneMapper {

    public ZoneDto toDto(Zone e) {
        ZoneDto d = new ZoneDto();
        d.setOid(e.getOid());
        d.setZoneCode(e.getZoneCode());
        d.setZoneName(e.getZoneName());
        d.setDivisionOid(e.getDivisionOid());
        d.setStatus(e.getStatus());
        d.setDivisionName(e.getDivisionName());
        return d;
    }

    public Zone toEntity(ZoneDto d) {
        Zone e = new Zone();
        e.setOid(d.getOid());
        e.setZoneCode(d.getZoneCode());
        e.setZoneName(d.getZoneName());
        e.setDivisionOid(d.getDivisionOid());
        e.setStatus(d.getStatus());
        return e;
    }
}
