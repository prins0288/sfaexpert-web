package in.opt.sfa.tenant.master.routearea.mapper;

import in.opt.sfa.tenant.master.routearea.dto.RouteAreaMapDto;
import in.opt.sfa.tenant.master.routearea.entity.RouteAreaMap;
import org.springframework.stereotype.Component;

@Component
public class RouteAreaMapMapper {

    public RouteAreaMapDto toDto(RouteAreaMap e) {
        RouteAreaMapDto d = new RouteAreaMapDto();
        d.setOid(e.getOid());
        d.setRouteOid(e.getRouteOid());
        d.setAreaOid(e.getAreaOid());
        d.setVisitSequence(e.getVisitSequence());
        d.setStatus(e.getStatus());
        d.setRouteName(e.getRouteName());
        d.setRouteCode(e.getRouteCode());
        d.setAreaName(e.getAreaName());
        d.setAreaCode(e.getAreaCode());
        return d;
    }
}
