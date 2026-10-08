package in.opt.sfa.tenant.master.routearea.repository;

import in.opt.sfa.tenant.master.routearea.entity.RouteAreaMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteAreaMapRepository extends JpaRepository<RouteAreaMap, Long> {
    List<RouteAreaMap> findAllByOrderByOidAsc();
    Optional<RouteAreaMap> findByRouteOidAndAreaOid(Long routeOid, Long areaOid);
    List<RouteAreaMap> findByAreaOidAndStatus(Long areaOid, String status);
}
