package in.opt.sfa.tenant.master.routearea.dto;

import java.util.List;

/** Body for POST /api/master/route-area — map one route to many areas, sequentially numbered from startSequence. */
public record RouteAreaMapRequest(Long routeOid, List<Long> areaOids, Integer startSequence) {}
