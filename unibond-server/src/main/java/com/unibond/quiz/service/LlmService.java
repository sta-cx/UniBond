package com.unibond.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class LlmService {
    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;
    private final boolean enabled;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmService(@Value("${app.llm.base-url:https://api.openai.com}") String baseUrl,
                      @Value("${app.llm.api-key:}") String apiKey,
                      @Value("${app.llm.model:gpt-4o-mini}") String model,
                      @Value("${app.llm.max-tokens:2000}") int maxTokens,
                      @Value("${app.llm.timeout-seconds:30}") int timeoutSeconds,
                      @Value("${app.llm.enabled:false}") boolean enabled) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = enabled;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }

    public boolean isEnabled() { return enabled && apiKey != null && !apiKey.isBlank(); }

    public String generate(String prompt) {
        if (!isEnabled()) return null;
        try {
            var requestBody = java.util.Map.of(
                "model", model,
                "messages", java.util.List.of(
                    java.util.Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.8,
                "max_tokens", maxTokens
            );
            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("LLM API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.at("/choices/0/message/content").asText();
            if (content == null || content.isBlank()) return null;

            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            } else if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            return content.trim();
        } catch (Exception e) {
            log.warn("LLM call failed: {}", e.getMessage());
            return null;
        }
    }
}
