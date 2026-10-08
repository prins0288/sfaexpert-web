package in.opt.sfa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal JSON-over-HTTP helper shared by the AI provider adapters. Uses the JDK
 * HttpClient and a private Jackson 2 ObjectMapper (stable on the classpath),
 * independent of Spring's configured mapper.
 */
final class AiHttp {

    static final ObjectMapper JSON = new ObjectMapper();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private AiHttp() { }

    /** POST a JSON body with the given headers; parse and return the JSON response. */
    static JsonNode postJson(String url, Map<String, String> headers, Object body) throws Exception {
        byte[] payload = JSON.writeValueAsBytes(body);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
        if (headers != null) headers.forEach(b::header);

        HttpResponse<String> res = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json;
        try {
            json = JSON.readTree(res.body());
        } catch (Exception parseError) {
            throw new RuntimeException("AI provider returned a non-JSON response (HTTP " + res.statusCode() + ")");
        }
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("AI provider error (HTTP " + res.statusCode() + "): " + errorMessage(json, res.body()));
        }
        return json;
    }

    /** Best-effort extraction of a provider's error message. */
    private static String errorMessage(JsonNode json, String raw) {
        if (json != null) {
            JsonNode err = json.get("error");
            if (err != null) {
                if (err.isTextual()) return err.asText();
                JsonNode msg = err.get("message");
                if (msg != null) return msg.asText();
            }
            JsonNode msg = json.get("message");
            if (msg != null) return msg.asText();
        }
        return raw == null ? "unknown error" : (raw.length() > 300 ? raw.substring(0, 300) : raw);
    }
}
