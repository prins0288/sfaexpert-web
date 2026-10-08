package in.opt.sfa.ai;

import in.opt.sfa.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes the CURRENT tenant's database schema (tables + columns) for the AI,
 * and — crucially for large databases — retrieves only the tables RELEVANT to a
 * question so the prompt stays bounded no matter how many tables exist.
 *
 *   - small schema (<= {@link #FULL_SCHEMA_LIMIT} tables): send everything;
 *   - large schema (1000+ tables): send the top-scoring tables for the question
 *     (lexical match on table + column names), and let the model pull more on
 *     demand via the {"tables":"keywords"} protocol.
 *
 * Read from information_schema (our own trusted query, not model-proposed) and
 * cached per tenant; call {@link #evict(String)} after a migration.
 */
@Service
@Slf4j 
public class AiSchemaService {

    /** At or below this many tables, just send the whole schema (no retrieval needed). */
    public static final int FULL_SCHEMA_LIMIT = 60;
    /** How many tables to include when retrieving for a large schema. */
    public static final int RETRIEVE_LIMIT = 40;

    private final JdbcTemplate jdbc;   // tenant-routed (@Primary)
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    public AiSchemaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** table -> "col type, col type, ..." for the current tenant (cached). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, String> tables() {
        String companyCode = TenantContext.getCompanyCode();
        if (companyCode == null) companyCode = "?";
        return cache.computeIfAbsent(companyCode, t -> load());
    }

    public int tableCount() { return tables().size(); }

    public void evict(String companyCode) { if (companyCode != null) cache.remove(companyCode); }

    /** Whether the whole schema fits (so no per-question retrieval is needed). */
    public boolean fitsWhole() { return tableCount() <= FULL_SCHEMA_LIMIT; }

    /** Formatted schema block for the whole database. */
    public String fullSchemaText() { return format(tables().keySet()); }

    /**
     * Schema block relevant to a question. Whole schema when it is small; otherwise
     * the top-{@link #RETRIEVE_LIMIT} tables scored against the question text.
     */
    public String relevantSchemaText(String question) {
        Map<String, String> all = tables();
        if (all.size() <= FULL_SCHEMA_LIMIT) return format(all.keySet());
        return format(searchTables(question, RETRIEVE_LIMIT));
    }

    /** Formatted schema block for tables matching free-text keywords (model-requested). */
    public String schemaForKeywords(String keywords, int limit) {
        return format(searchTables(keywords, limit));
    }

    // ---- retrieval ----------------------------------------------------------

    /** Rank tables by lexical overlap of the query with table + column names. */
    public List<String> searchTables(String query, int limit) {
        Map<String, String> all = tables();
        List<String> terms = terms(query);
        if (terms.isEmpty()) {                       // nothing to match on -> first N by name
            return all.keySet().stream().sorted().limit(limit).toList();
        }
        record Scored(String table, int score) { }
        List<Scored> scored = new ArrayList<>();
        all.forEach((table, cols) -> {
            String tname = table.toLowerCase(Locale.ROOT);
            String colBlob = cols.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String term : terms) {
                if (tname.contains(term)) score += 5;         // table-name hit weighs most
                if (colBlob.contains(term)) score += 1;       // column hit
            }
            if (score > 0) scored.add(new Scored(table, score));
        });
        scored.sort((a, b) -> b.score() - a.score());
        List<String> out = new ArrayList<>();
        for (Scored s : scored) { if (out.size() >= limit) break; out.add(s.table()); }
        // if very few matched, pad with some tables so the model still has context
        if (out.size() < Math.min(limit, 10)) {
            for (String t : all.keySet().stream().sorted().toList()) {
                if (out.size() >= limit) break;
                if (!out.contains(t)) out.add(t);
            }
        }
        return out;
    }

    /** Split a query into lowercase word stems (drop tiny/stopwords, singularise). */
    private static List<String> terms(String q) {
        List<String> out = new ArrayList<>();
        if (q == null) return out;
        for (String raw : q.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.length() < 3) continue;
            if (STOP.contains(raw)) continue;
            String t = raw.endsWith("s") && raw.length() > 3 ? raw.substring(0, raw.length() - 1) : raw;
            out.add(t);
        }
        return out;
    }

    private static final java.util.Set<String> STOP = java.util.Set.of(
            "the", "and", "for", "with", "how", "many", "list", "give", "show", "count", "all",
            "get", "what", "which", "are", "there", "please", "report", "data", "from", "want",
            "need", "total", "summary", "each", "per", "into", "this", "that", "have", "has");

    // ---- formatting / loading -----------------------------------------------

    private String format(Collection<String> tableNames) {
        Map<String, String> all = tables();
        StringBuilder out = new StringBuilder();
        for (String t : tableNames) {
            String cols = all.get(t);
            if (cols != null) out.append(t).append('(').append(cols).append(")\n");
        }
        return out.toString();
    }

    private Map<String, String> load() {
        Map<String, StringBuilder> tables = new LinkedHashMap<>();
        jdbc.query(
                "SELECT table_name, column_name, data_type " +
                "FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() " +
                "ORDER BY table_name, ordinal_position",
                rs -> {
                    String tbl = rs.getString(1);
                    String col = rs.getString(2);
                    String type = rs.getString(3);
                    StringBuilder sb = tables.computeIfAbsent(tbl, k -> new StringBuilder());
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(col).append(' ').append(type);
                });
        Map<String, String> out = new LinkedHashMap<>();
        tables.forEach((k, v) -> out.put(k, v.toString()));
       // log.info("Rag table data print = {}",out);
        return out;
    }
}
