package in.opt.sfa.ai;

import java.util.List;

/**
 * A chat LLM provider (OpenAI / Gemini / Claude / ...). Kept deliberately thin:
 * one plain "given a system prompt + message history, return the assistant's
 * text" call. All the data-tooling (the JSON {sql}/{html} protocol, running the
 * guarded query, looping) lives provider-agnostically in {@link AiChatService},
 * so adding a provider is just one more adapter — no per-provider tool wiring.
 */
public interface AiProvider {

    /** One message in the conversation. role = "user" | "assistant". */
    record ChatMessage(String role, String content) { }

    /** Stable id used in config (ai.provider): "openai" | "gemini" | "claude". */
    String id();

    /** Human label for the picker. */
    String label();

    /** Suggested model ids for the picker (the user may still type another). */
    List<String> models();

    /**
     * Send the system prompt + history and return the assistant's raw text reply.
     * @throws Exception on transport / API errors (caller reports safely)
     */
    String complete(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws Exception;
}
