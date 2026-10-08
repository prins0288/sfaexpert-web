package in.opt.sfa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Anthropic Claude Messages API adapter. */
@Component
public class ClaudeProvider implements AiProvider {

    @Override public String id() { return "claude"; }
    @Override public String label() { return "Claude (Anthropic)"; }
    @Override public List<String> models() {
        return List.of("claude-sonnet-4-5", "claude-opus-4-1", "claude-haiku-4-5", "claude-3-5-sonnet-latest");
    }

    @Override
    public String complete(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws Exception {
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (ChatMessage m : messages) msgs.add(Map.of("role", m.role(), "content", m.content()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 2048);
        body.put("system", systemPrompt);
        body.put("messages", msgs);
        body.put("temperature", 0.1);

        JsonNode res = AiHttp.postJson(
                "https://api.anthropic.com/v1/messages",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"),
                body);
        JsonNode text = res.at("/content/0/text");
        return text.isMissingNode() ? "" : text.asText();
    }
}
