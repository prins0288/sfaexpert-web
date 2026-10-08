package in.opt.sfa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Google Gemini generateContent adapter. */
@Component
public class GeminiProvider implements AiProvider {

    @Override public String id() { return "gemini"; }
    @Override public String label() { return "Google Gemini"; }
    @Override public List<String> models() {
        // gemini-2.5-flash first: fast + reliable default. (gemini-2.5-pro dropped —
        // Google now returns "not available to new users" for it.) "*-latest" aliases
        // auto-track the newest model; explicit ids follow for pinning.
        return List.of(
                "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-latest",
                "gemini-pro-latest", "gemini-3.1-pro-preview", "gemini-3.6-flash");
    }

    @Override
    public String complete(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws Exception {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage m : messages) {
            String role = "assistant".equals(m.role()) ? "model" : "user";   // Gemini uses "model"
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", m.content()))));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", contents);
        body.put("generationConfig", Map.of("temperature", 0.1));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + URLEncoder.encode(model, StandardCharsets.UTF_8)
                + ":generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        JsonNode res = AiHttp.postJson(url, Map.of(), body);
        JsonNode text = res.at("/candidates/0/content/parts/0/text");
        return text.isMissingNode() ? "" : text.asText();
    }
}
