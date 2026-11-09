package in.opt.sfa.web;

import in.opt.sfa.common.entity.TenantConfig;
import in.opt.sfa.common.repository.TenantConfigRepository;
import in.opt.sfa.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.sql.SQLTransientConnectionException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When a database cannot be reached the user must see "Server N is not responding"
 * instead of a JPA / SQL error. Server 1 = the central database (logins, companies,
 * settings); Server 2, 3 … = one per company, in the order they were configured. There is
 * really one server — the numbers only make it look like each company has its own.
 */
@Component
public class ServerLabels {

    private static final Logger log = LoggerFactory.getLogger(ServerLabels.class);
    private static final String CENTRAL_POOL = "common-pool";

    private final TenantConfigRepository configs;
    private final Map<String, Integer> serverOfCompany = new ConcurrentHashMap<>();

    public ServerLabels(TenantConfigRepository configs) {
        this.configs = configs;
    }

    /** The user-facing message when {@code e} means "a database is down", else null. */
    public String messageFor(Throwable e) {
        if (!isConnectionFailure(e)) return null;
        return "Server " + serverNumber(e) + " is not responding. Please try again after some time.";
    }

    /** Is this (or something it wraps) a failure to get a database connection? */
    public static boolean isConnectionFailure(Throwable e) {
        int depth = 0;
        for (Throwable t = e; t != null && depth++ < 12; t = t.getCause()) {
            String n = t.getClass().getName();
            String m = t.getMessage() == null ? "" : t.getMessage();
            if (t instanceof SQLTransientConnectionException || t instanceof ConnectException
                    || n.endsWith("CommunicationsException") || n.endsWith("CannotGetJdbcConnectionException")
                    || n.endsWith("CannotCreateTransactionException") || n.endsWith("PoolInitializationException")
                    || n.endsWith("JDBCConnectionException") || n.endsWith("MySQLNonTransientConnectionException")
                    || m.contains("Unable to acquire JDBC Connection") || m.contains("Communications link failure")
                    || m.contains("Connection is not available") || m.contains("Could not open JPA EntityManager")) {
                return true;
            }
        }
        return false;
    }

    private int serverNumber(Throwable e) {
        // which pool failed? "common-pool - Connection is not available…" / "tenant-<code> - …"
        boolean central = false;
        String company = null;
        int depth = 0;
        for (Throwable t = e; t != null && depth++ < 12; t = t.getCause()) {
            String m = t.getMessage();
            if (m == null) continue;
            if (m.contains(CENTRAL_POOL)) central = true;
            int k = m.indexOf("tenant database for '");   // login: "Could not open tenant database for 'acme'"
            if (k >= 0 && company == null) {
                int from = k + 21, to = m.indexOf('\'', from);
                if (to > from) company = m.substring(from, to);
            }
            int i = m.indexOf("tenant-");
            if (i >= 0 && company == null) {
                int end = i + 7;
                while (end < m.length() && (Character.isLetterOrDigit(m.charAt(end)) || m.charAt(end) == '_' || m.charAt(end) == '.')) end++;
                company = m.substring(i + 7, end);
            }
        }
        if (central) return 1;
        if (company == null || company.isBlank()) company = TenantContext.getCompanyCode();
        if (company == null || company.isBlank()) return 1;
        Integer n = serverOfCompany.get(company);
        if (n == null) { refresh(); n = serverOfCompany.get(company); }
        return n == null ? 2 : n;
    }

    /** (Re)number the companies: one "server" each, in configuration order, starting at 2 (all run on one real server). */
    private void refresh() {
        try {
            List<TenantConfig> all = new ArrayList<>(configs.findAll());
            all.sort(Comparator.comparing(TenantConfig::getId));
            int n = 2;
            for (TenantConfig c : all) serverOfCompany.put(c.getCompanyCode(), n++);
        } catch (Exception ex) {
            log.warn("Could not number the servers (central database unreachable?): {}", ex.getMessage());
        }
    }
}
