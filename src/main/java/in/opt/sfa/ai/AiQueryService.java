package in.opt.sfa.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs an AI-proposed query against the CURRENT tenant's database, safely:
 *   1. {@link AiSqlGuard} first proves it is a single read-only SELECT;
 *   2. the JDBC connection is flipped to read-only, so a write would fail anyway;
 *   3. a hard row cap ({@link #MAX_ROWS}) and a query timeout bound the work.
 *
 * Uses the @Primary (tenant-routed) JdbcTemplate, so it automatically hits the
 * logged-in user's tenant DB via TenantContext — one tenant can never read
 * another's data.
 */
@Service
public class AiQueryService {

    /** Hard ceiling on rows returned to the model, whatever the query says. */
    public static final int MAX_ROWS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private final JdbcTemplate jdbc;   // tenant-routed (@Primary)

    public AiQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record QueryResult(List<String> columns, List<List<Object>> rows, boolean truncated) {
        public int rowCount() { return rows.size(); }
    }

    /** Validate + execute a read-only SELECT on the current tenant DB. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public QueryResult run(String proposedSql) {
        String sql = AiSqlGuard.sanitize(proposedSql);   // throws on anything unsafe

        return jdbc.execute((java.sql.Connection con) -> {
            boolean prevRo = con.isReadOnly();
            try {
                con.setReadOnly(true);
            } catch (Exception ignored) { /* some drivers no-op; the guard already blocked writes */ }
            try (Statement st = con.createStatement()) {
                st.setMaxRows(MAX_ROWS + 1);            // +1 so we can detect truncation
                try { st.setQueryTimeout(QUERY_TIMEOUT_SECONDS); } catch (Exception ignored) { }
                try (ResultSet rs = st.executeQuery(sql)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int n = md.getColumnCount();
                    List<String> cols = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));

                    List<List<Object>> rows = new ArrayList<>();
                    boolean truncated = false;
                    while (rs.next()) {
                        if (rows.size() >= MAX_ROWS) { truncated = true; break; }
                        List<Object> row = new ArrayList<>(n);
                        for (int i = 1; i <= n; i++) row.add(rs.getObject(i));
                        rows.add(row);
                    }
                    return new QueryResult(cols, rows, truncated);
                }
            } finally {
                try { con.setReadOnly(prevRo); } catch (Exception ignored) { }
            }
        });
    }

    /** Compact tabular text of a result, for feeding back to the model. */
    public static String toText(QueryResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", r.columns())).append('\n');
        for (List<Object> row : r.rows()) {
            List<String> cells = new ArrayList<>(row.size());
            for (Object o : row) cells.add(o == null ? "" : String.valueOf(o));
            sb.append(String.join(" | ", cells)).append('\n');
        }
        if (r.truncated()) sb.append("... (truncated at ").append(MAX_ROWS).append(" rows)\n");
        return sb.toString();
    }

    /** Result as a list of {column: value} maps (for JSON / direct rendering). */
    public static List<Map<String, Object>> toMaps(QueryResult r) {
        List<Map<String, Object>> out = new ArrayList<>(r.rows().size());
        for (List<Object> row : r.rows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < r.columns().size(); i++) m.put(r.columns().get(i), row.get(i));
            out.add(m);
        }
        return out;
    }
}
