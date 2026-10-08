package in.opt.sfa.tenant.dashboard.view.service;

import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.client.repository.ClientRepository;
import in.opt.sfa.tenant.master.product.repository.ProductRepository;
import in.opt.sfa.tenant.master.route.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Row counts for the dashboard's stat cards — ONE lightweight query per count
 * (COUNT(*), not a full row fetch) instead of the dashboard downloading all
 * four master lists in full just to read their .length client-side.
 */
@Service
public class DashboardService {

    private final RouteRepository routes;
    private final AreaRepository areas;
    private final ClientRepository clients;
    private final ProductRepository products;

    public DashboardService(RouteRepository routes, AreaRepository areas,
                             ClientRepository clients, ProductRepository products) {
        this.routes = routes;
        this.areas = areas;
        this.clients = clients;
        this.products = products;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Long> counts() {
        Map<String, Long> res = new LinkedHashMap<>();
        res.put("routes", routes.count());
        res.put("areas", areas.count());
        res.put("clients", clients.count());
        res.put("products", products.count());
        return res;
    }
}
