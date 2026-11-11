package in.opt.sfa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import in.opt.sfa.ai.AiProvider.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a user's question into an HTML answer, grounded in the tenant's own data.
 *
 * Protocol (provider-agnostic): the model is told to reply with ONE JSON object —
 * either {@code {"sql":"<read-only SELECT>"}} to fetch data, or
 * {@code {"html":"<final answer>"}} when done. We run each SELECT through
 * {@link AiQueryService} (guarded, read-only, tenant-scoped), feed the rows back,
 * and loop up to {@link #MAX_STEPS} times. This gives multi-step "RAG over the
 * database" without any provider-specific tool-calling wiring.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_STEPS = 3;   // few steps = fast answers
    /** Cap how much conversation is sent to the model (bounds cost/latency). */
    private static final int MAX_HISTORY = 20;

    private final Map<String, AiProvider> providers = new LinkedHashMap<>();
    private final AiSettings settings;
    private final AiSchemaService schema;
    private final AiQueryService query;

    public AiChatService(List<AiProvider> providerList, AiSettings settings,
                         AiSchemaService schema, AiQueryService query) {
        for (AiProvider p : providerList) providers.put(p.id(), p);
        this.settings = settings;
        this.schema = schema;
        this.query = query;
    }

    public java.util.Collection<AiProvider> providers() { return providers.values(); }

    public AiProvider provider(String id) { return id == null ? null : providers.get(id.toLowerCase()); }

    /** Answer the conversation (last item is the newest user turn). Returns HTML. */
    public String answer(List<ChatMessage> history, String modelOverride) { return answer(history, modelOverride, true); }

    /** {@code dataAllowed}: false for a plain employee - the assistant then answers only how-to / where-is questions from the application guide (no company data). */
    public String answer(List<ChatMessage> history, String modelOverride, boolean dataAllowed) {
        String asked = latestUserText(history);
        if (AiScope.asksToChangeData(asked)) return AiScope.REFUSE_WRITE;   // never reaches the model: nothing can be inserted / updated / deleted through the assistant
        AiSettings.Config cfg = settings.current();
        if (!cfg.isReady()) {
            throw new IllegalStateException(
                    "AI is not set up for this company yet. An admin can configure the provider and API key in Settings \u2192 AI.");
        }
        AiProvider provider = providers.get(cfg.provider().toLowerCase());
        if (provider == null) {
            throw new IllegalStateException("Unknown AI provider configured: " + cfg.provider());
        }
        String model = firstNonBlank(modelOverride, cfg.model(),
                provider.models().isEmpty() ? null : provider.models().get(0));
        if (model == null) throw new IllegalStateException("No model selected for " + provider.label());

        // Keep only the most recent turns so the prompt (and cost) stays bounded.
        List<ChatMessage> recent = history.size() > MAX_HISTORY
                ? history.subList(history.size() - MAX_HISTORY, history.size()) : history;
        List<ChatMessage> work = new ArrayList<>(recent);
        // Build the prompt with the tables relevant to THIS question (bounded for huge schemas).
        String system = systemPrompt(latestUserText(recent), dataAllowed);

        for (int step = 0; step < MAX_STEPS; step++) {
            String raw;
            try {
                raw = provider.complete(cfg.apiKey(), model, system, work);
            } catch (Exception e) {
                log.warn("AI provider '{}' call failed: {}", provider.id(), e.getMessage());
                throw new IllegalStateException("The AI service could not be reached: " + e.getMessage());
            }

            Parsed p = parse(raw);
            if (p.html != null) return AiScope.screen(sanitizeHtml(p.html));
            if (p.tables != null) {
                // model wants more of the schema (large DB) — give it matching tables and loop
                String more = schema.schemaForKeywords(p.tables, AiSchemaService.RETRIEVE_LIMIT);
                work.add(new ChatMessage("assistant", raw));
                work.add(new ChatMessage("user",
                        "Matching tables:\n" + (more.isBlank() ? "(none)" : more)
                        + "\n\nNow reply with {\"sql\":\"...\"} or {\"html\":\"...\"}."));
                continue;
            }
            if (p.sql != null && !dataAllowed) return AiScope.NO_DATA;
            if (p.sql != null) {
                String resultText;
                try {
                    resultText = AiQueryService.toText(query.run(p.sql));
                } catch (IllegalArgumentException blocked) {
                    resultText = "REJECTED (not a safe read-only query): " + blocked.getMessage();
                } catch (Exception ex) {
                    resultText = "ERROR running the query: " + rootMessage(ex);
                }
                work.add(new ChatMessage("assistant", raw));
                work.add(new ChatMessage("user",
                        "Query result:\n" + resultText
                        + "\n\nIf this answers the question, reply with {\"html\":\"...\"}. "
                        + "Otherwise reply with another {\"sql\":\"...\"}."));
                continue;
            }
            // Not JSON we understand — treat the whole reply as the final HTML.
            return AiScope.screen(sanitizeHtml(raw));
        }
        return "<p>Sorry, I couldn't finish that within a few steps. Please try a more specific question.</p>";
    }

    // ---- system prompt ------------------------------------------------------

    private String systemPrompt(String question, boolean dataAllowed) {
        StringBuilder sb = new StringBuilder(14000);
        sb.append(AiScope.PERSONA);
        if (dataAllowed) {
            boolean large = !schema.fitsWhole();
            sb.append(AiScope.DATA_PROTOCOL.replace("{{TABLES}}", large
                    ? "\n  - to see more tables: {\"tables\":\"<keywords>\"} (the schema below holds only the tables most relevant to the question)" : "")
                    .replace("{{MAXROWS}}", String.valueOf(AiQueryService.MAX_ROWS)));
        } else {
            sb.append(AiScope.NO_DATA_PROTOCOL);
        }
        sb.append(AiScope.RULES);
        sb.append("\n=== APPLICATION GUIDE (your knowledge of the application) ===\n").append(AiScope.guide()).append('\n');
        if (dataAllowed) {
            sb.append("\n=== ").append(schema.fitsWhole() ? "Database schema" : "Relevant tables (a SUBSET; request more with {\"tables\":\"...\"})")
              .append(" (CONFIDENTIAL - never disclose; table(column type, ...)) ===\n").append(schema.relevantSchemaText(question)).append('\n');
        }
        return sb.toString();
    }

    // ---- reply parsing ------------------------------------------------------

    private record Parsed(String sql, String html, String tables) { }

    /** Newest user message text (for schema retrieval). */
    private static String latestUserText(List<ChatMessage> msgs) {
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if ("user".equals(msgs.get(i).role())) return msgs.get(i).content();
        }
        return "";
    }

    private Parsed parse(String raw) {
        if (raw == null) return new Parsed(null, null, null);
        String s = raw.trim();
        // strip ```json ... ``` fences if present
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int a = s.indexOf('{'), b = s.lastIndexOf('}');
        if (a >= 0 && b > a) {
            String json = s.substring(a, b + 1);
            try {
                JsonNode n = AiHttp.JSON.readTree(json);
                JsonNode sql = n.get("sql");
                if (sql != null && sql.isTextual() && !sql.asText().isBlank()) {
                    return new Parsed(sql.asText(), null, null);
                }
                JsonNode tables = n.get("tables");
                if (tables != null && tables.isTextual() && !tables.asText().isBlank()) {
                    return new Parsed(null, null, tables.asText());
                }
                JsonNode html = n.get("html");
                if (html != null && html.isTextual()) {
                    return new Parsed(null, html.asText(), null);
                }
            } catch (Exception ignored) {
                // not valid JSON — fall through to treat as plain HTML
            }
        }
        return new Parsed(null, null, null);
    }

    // ---- HTML safety --------------------------------------------------------

    /** Strip anything executable from the model's HTML before it is rendered. */
    static String sanitizeHtml(String html) {
        if (html == null) return "";
        String out = html;
        out = out.replaceAll("(?is)<\\s*script.*?>.*?<\\s*/\\s*script\\s*>", "");
        out = out.replaceAll("(?is)<\\s*style.*?>.*?<\\s*/\\s*style\\s*>", "");
        out = out.replaceAll("(?is)<\\s*/?\\s*(html|head|body)[^>]*>", "");
        out = out.replaceAll("(?i)\\son\\w+\\s*=\\s*\"[^\"]*\"", "");   // onclick="..."
        out = out.replaceAll("(?i)\\son\\w+\\s*=\\s*'[^']*'", "");
        out = out.replaceAll("(?i)javascript:", "");
        return out.trim();
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r.getMessage() == null ? r.getClass().getSimpleName() : r.getMessage();
    }
}
