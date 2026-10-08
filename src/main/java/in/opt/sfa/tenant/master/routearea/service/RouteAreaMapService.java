package in.opt.sfa.tenant.master.routearea.service;

import in.opt.sfa.tenant.master.routearea.dto.RouteAreaMapDto;
import in.opt.sfa.tenant.master.routearea.dto.RouteAreaMapRequest;
import in.opt.sfa.tenant.master.routearea.entity.RouteAreaMap;
import in.opt.sfa.tenant.master.routearea.mapper.RouteAreaMapMapper;
import in.opt.sfa.tenant.master.routearea.repository.RouteAreaMapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RouteAreaMapService {

    private final RouteAreaMapRepository maps;
    private final RouteAreaMapMapper mapper;

    public RouteAreaMapService(RouteAreaMapRepository maps, RouteAreaMapMapper mapper) {
        this.maps = maps;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<RouteAreaMapDto> list() {
        return maps.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(RouteAreaMapRequest req) {
        if (req.routeOid() == null || req.areaOids() == null || req.areaOids().isEmpty()) {
            throw new IllegalStateException("Select a route and at least one area");
        }
        int seq = req.startSequence() != null ? req.startSequence() : 1;
        int mapped = 0;
        for (Long areaOid : req.areaOids()) {
            RouteAreaMap m = maps.findByRouteOidAndAreaOid(req.routeOid(), areaOid)
                    .orElseGet(RouteAreaMap::new);
            m.setRouteOid(req.routeOid());
            m.setAreaOid(areaOid);
            m.setVisitSequence(seq++);
            m.setStatus("Y");
            maps.save(m);
            mapped++;
        }
        return Map.of("mapped", mapped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RouteAreaMapDto updateStatus(Long oid, String status) {
        RouteAreaMap m = maps.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Mapping not found: " + oid));
        m.setStatus(status);
        return mapper.toDto(maps.save(m));
    }
}
