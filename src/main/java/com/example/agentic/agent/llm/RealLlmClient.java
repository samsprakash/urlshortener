package com.example.agentic.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Real Claude-backed implementation, enabled via agentic.agent.llm.mode=real
 * (ADR-5). Talks to the Anthropic Messages API directly over HTTP so no vendor
 * SDK dependency is required for a prototype. Not on the CI/default demo path —
 * see 02-decisions.md ADR-5 for why.
 */
@Component
@Conditional(RealLlmModeCondition.class)
public class RealLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RealLlmClient.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AgentLlmProperties properties;

    public RealLlmClient(ObjectMapper objectMapper, AgentLlmProperties properties) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String apiKey = properties.real().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "agentic.agent.llm.mode=real requires ANTHROPIC_API_KEY to be set");
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.real().model(),
                    "max_tokens", 2048,
                    "system", systemPrompt,
                    "messages", java.util.List.of(Map.of("role", "user", "content", userPrompt))
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Anthropic API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/content/0/text").asText();
        } catch (Exception e) {
            log.error("RealLlmClient call failed", e);
            throw new IllegalStateException("LLM call failed: " + e.getMessage(), e);
        }
    }
}
