package in.opt.sfa.tenant.master.route.mapper;

import in.opt.sfa.tenant.master.route.dto.RouteDto;
import in.opt.sfa.tenant.master.route.entity.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteDto toDto(Route e) {
        RouteDto d = new RouteDto();
        d.setOid(e.getOid());
        d.setRouteCode(e.getRouteCode());
        d.setRouteName(e.getRouteName());
        d.setDivisionOid(e.getDivisionOid());
        d.setZoneOid(e.getZoneOid());
        d.setStateOid(e.getStateOid());
        d.setHqOid(e.getHqOid());
        d.setDistanceKm(e.getDistanceKm());
        d.setDescription(e.getDescription());
        d.setStatus(e.getStatus());
        d.setDivisionName(e.getDivisionName());
        d.setZoneName(e.getZoneName());
        d.setStateName(e.getStateName());
        d.setHqName(e.getHqName());
        return d;
    }

    public Route toEntity(RouteDto d) {
        Route e = new Route();
        e.setOid(d.getOid());
        e.setRouteCode(d.getRouteCode());
        e.setRouteName(d.getRouteName());
        e.setDivisionOid(d.getDivisionOid());
        e.setZoneOid(d.getZoneOid());
        e.setStateOid(d.getStateOid());
        e.setHqOid(d.getHqOid());
        e.setDistanceKm(d.getDistanceKm());
        e.setDescription(d.getDescription());
        e.setStatus(d.getStatus());
        return e;
    }
}
