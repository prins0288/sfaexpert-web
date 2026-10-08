package in.opt.sfa.tenant.datasource;

import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The @Primary tenant datasource. It owns no real connection pool; on every
 * getConnection() it looks up the current tenant and delegates to that tenant's
 * pool (creating it lazily via the manager). If no tenant is bound to the
 * thread, that is a bug (someone hit a tenant repo without logging in), so we
 * fail loudly instead of silently connecting to the wrong DB.
 */
public class TenantRoutingDataSource extends AbstractDataSource {

    private final TenantDataSourceManager manager;

    public TenantRoutingDataSource(TenantDataSourceManager manager) {
        this.manager = manager;
    }

    private DataSource current() {
        String companyCode = TenantContext.getCompanyCode();
        if (companyCode == null) {
            throw new IllegalStateException(
                    "No tenant bound to the current thread. A tenant datasource "
                            + "can only be used inside an authenticated request.");
        }
        return manager.getDataSource(companyCode);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return current().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return current().getConnection(username, password);
    }
}
