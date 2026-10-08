package in.opt.sfa.ai;

import java.util.regex.Pattern;

/**
 * Security gate for AI-generated SQL. The model is allowed to READ the tenant's
 * data to answer questions, and nothing more. Every query it proposes passes
 * through here before it is ever sent to the database.
 *
 * Rules (defence in depth — the read-only connection + row cap in
 * {@link AiQueryService} are the second and third layers):
 *   - exactly ONE statement (no ';' except a single optional trailing one);
 *   - must START with SELECT or WITH (a CTE that ends in a SELECT);
 *   - no data-changing / DDL / privilege / file / routine keywords;
 *   - no access to system schemas (mysql / information_schema / performance_schema / sys);
 *   - no SELECT ... INTO (OUTFILE/DUMPFILE/variable).
 *
 * On any violation it throws {@link IllegalArgumentException}; the caller turns
 * that into a safe, non-leaky message.
 */
public final class AiSqlGuard {

    /** Whole-word forbidden tokens (case-insensitive). */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|MERGE|REPLACE|UPSERT|DROP|ALTER|CREATE|TRUNCATE|"
            + "GRANT|REVOKE|RENAME|CALL|EXEC|EXECUTE|PREPARE|DEALLOCATE|HANDLER|LOCK|UNLOCK|"
            + "SET|USE|COMMIT|ROLLBACK|SAVEPOINT|LOAD|OUTFILE|DUMPFILE|INTO|"
            + "INFORMATION_SCHEMA|PERFORMANCE_SCHEMA|MYSQL|SYS)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STARTS_OK = Pattern.compile("^\\s*(SELECT|WITH)\\b", Pattern.CASE_INSENSITIVE);

    private AiSqlGuard() { }

    /**
     * Validate and normalise. Returns the single, clean SELECT ready to run.
     * @throws IllegalArgumentException if the query is not a safe read.
     */
    public static String sanitize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Empty query");
        }
        String q = stripComments(sql).trim();
        // allow a single trailing semicolon, then forbid any others
        if (q.endsWith(";")) q = q.substring(0, q.length() - 1).trim();
        if (q.contains(";")) {
            throw new IllegalArgumentException("Only a single statement is allowed");
        }
        if (!STARTS_OK.matcher(q).find()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        if (FORBIDDEN.matcher(q).find()) {
            throw new IllegalArgumentException("The query uses a disallowed keyword; only read-only SELECTs are permitted");
        }
        return q;
    }

    /** Remove SQL line and block comments so they cannot hide anything. */
    private static String stripComments(String sql) {
        String noBlock = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        StringBuilder out = new StringBuilder(noBlock.length());
        for (String line : noBlock.split("\n", -1)) {
            int i = line.indexOf("--");
            out.append(i >= 0 ? line.substring(0, i) : line).append('\n');
        }
        return out.toString();
    }
}
