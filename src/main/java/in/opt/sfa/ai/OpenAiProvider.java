package in.opt.sfa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAI Chat Completions adapter. */
@Component
public class OpenAiProvider implements AiProvider {

    @Override public String id() { return "openai"; }
    @Override public String label() { return "OpenAI"; }
    @Override public List<String> models() {
        return List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1", "o4-mini");
    }

    @Override
    public String complete(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws Exception {
        List<Map<String, Object>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage m : messages) msgs.add(Map.of("role", m.role(), "content", m.content()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("temperature", 0.1);

        JsonNode res = AiHttp.postJson(
                "https://api.openai.com/v1/chat/completions",
                Map.of("Authorization", "Bearer " + apiKey),
                body);
        JsonNode content = res.at("/choices/0/message/content");
        return content.isMissingNode() ? "" : content.asText();
    }
}
