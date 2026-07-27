package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findAllByOrderByOidAsc();
    List<Route> findByStatusOrderByRouteNameAsc(String status);
    Optional<Route> findByRouteCode(String routeCode);
}
