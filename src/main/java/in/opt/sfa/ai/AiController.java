package in.opt.sfa.ai;

import in.opt.sfa.ai.AiProvider.ChatMessage;
import in.opt.sfa.common.entity.AiChatLog;
import in.opt.sfa.common.repository.AiChatLogRepository;
import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.security.Authz;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI assistant API.
 *   GET  /api/ai/meta      -> providers/models + whether AI is ready (for the pages)
 *   GET  /api/ai/settings  -> current provider/model + whether a key is set (admin)
 *   POST /api/ai/settings  -> save provider/apiKey/model (admin; global = super-admin)
 *   POST /api/ai/chat      -> { messages, model? } -> { html }  (admin/manager)
 *
 * Config lives per tenant (with a global "*" fallback) in company_setting_master;
 * API keys are never sent back to the client.
 */
@Tag(name = "AI Assistant", description = "Ask questions about your data; answered from the tenant DB (read-only) as HTML")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiChatService chat;
    private final AiSettings settings;
    private final CompanySettingStore store;
    private final AiChatLogRepository chatLog;

    public AiController(AiChatService chat, AiSettings settings, CompanySettingStore store,
                        AiChatLogRepository chatLog) {
        this.chat = chat;
        this.settings = settings;
        this.store = store;
        this.chatLog = chatLog;
    }

    /** Providers + models + readiness (for the chat page's model picker and the settings page). */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        AiSettings.Config cfg = settings.current();
        List<Map<String, Object>> provs = new ArrayList<>();
        for (AiProvider p : chat.providers()) {
            provs.add(Map.of("id", p.id(), "label", p.label(), "models", p.models()));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ready", cfg.isReady());
        res.put("provider", cfg.provider());
        res.put("model", cfg.model());
        res.put("providers", provs);
        return res;
    }

    /** Current saved config (admin) — the API key is returned only as a boolean. */
    @GetMapping("/settings")
    public Map<String, Object> getSettings(@RequestParam(defaultValue = "tenant") String scope) {
        Authz.requireRole("ADMIN", "SUPER_ADMIN");
        String companyCode = scopeCompanyCode(scope);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", scope);
        res.put("provider", store.valueFor(companyCode, AiSettings.K_PROVIDER, null));
        res.put("model", store.valueFor(companyCode, AiSettings.K_MODEL, null));
        res.put("hasKey", store.valueFor(companyCode, AiSettings.K_API_KEY, "").length() > 0);
        return res;
    }

    /** Save provider/model/apiKey. Blank apiKey keeps the existing one. */
    @PostMapping("/settings")
    @Operation(summary = "Save AI provider/model/key (scope=tenant default, or global for super-admin)")
    public Map<String, Object> saveSettings(@RequestBody Map<String, String> body) {
        String scope = body.getOrDefault("scope", "tenant");
        String companyCode = scopeCompanyCode(scope);   // also enforces the right role

        String provider = trim(body.get("provider"));
        String model = trim(body.get("model"));
        String apiKey = body.get("apiKey");

        if (provider != null) store.putFor(companyCode, AiSettings.K_PROVIDER, provider);
        if (model != null) store.putFor(companyCode, AiSettings.K_MODEL, model);
        if (apiKey != null && !apiKey.isBlank()) store.putFor(companyCode, AiSettings.K_API_KEY, apiKey.trim());

        return Map.of("saved", true);
    }

    /** Ask a question. Body: { messages:[{role,content}], model? }. Returns { html }. */
    @PostMapping("/chat")
    @Operation(summary = "Ask the AI; answered from your data as HTML")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        Authz.requireRole("ADMIN", "MANAGER", "SUPER_ADMIN");
        List<ChatMessage> history = new ArrayList<>();
        Object msgs = body.get("messages");
        if (msgs instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    String role = str(m.get("role"));
                    String content = str(m.get("content"));
                    if (content != null && !content.isBlank()) {
                        history.add(new ChatMessage("assistant".equals(role) ? "assistant" : "user", content));
                    }
                }
            }
        }
        if (history.isEmpty()) throw new IllegalArgumentException("No message to answer");
        String model = str(body.get("model"));
        String html = chat.answer(history, model);

        // Persist this turn (prompt + response) to the common ai_chat log — best effort.
        String prompt = history.get(history.size() - 1).content();   // newest user turn
        saveLog(prompt, html, model);

        return Map.of("html", html);
    }

    /** Log one AI turn to sfa_central.ai_chat; never let a logging failure break the chat. */
    private void saveLog(String prompt, String response, String model) {
        try {
            UserContext.CurrentUser u = UserContext.get();
            AiChatLog row = new AiChatLog();
            row.setCompanyCode(TenantContext.getCompanyCode());
            row.setEmpId(u == null ? null : u.empId());
            row.setPrompt(prompt);
            row.setResponse(response);
            row.setModel(model);
            row.setCreatedAt(LocalDateTime.now());
            chatLog.save(row);
        } catch (Exception e) {
            log.warn("Could not save ai_chat log: {}", e.getMessage());
        }
    }

    // ---- helpers ------------------------------------------------------------

    /** Resolve the settings scope to a company code, enforcing the role for it. */
    private String scopeCompanyCode(String scope) {
        if ("global".equalsIgnoreCase(scope)) {
            Authz.requireRole("SUPER_ADMIN");
            return CompanySettingStore.GLOBAL;
        }
        Authz.requireRole("ADMIN", "SUPER_ADMIN");
        return TenantContext.getCompanyCode();
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
}
