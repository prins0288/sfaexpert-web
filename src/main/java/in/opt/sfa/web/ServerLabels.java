package in.opt.sfa.web;

import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.sql.SQLTransientConnectionException;

/**
 * When a database cannot be reached the user must see one friendly message instead of a
 * JPA / SQL error. The real cause is only logged.
 */
@Component
public class ServerLabels {

    public static final String MESSAGE = "We're having trouble connecting right now. Please try again in a few minutes.";

    /** The user-facing message when {@code e} means "a database is down", else null. */
    public String messageFor(Throwable e) {
        return isConnectionFailure(e) ? MESSAGE : null;
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
}
