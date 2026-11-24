package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AnthropicClient(
            ObjectMapper objectMapper,
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:" + DEFAULT_MODEL + "}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public AnalysisResult analyze(String systemPrompt, String userPrompt) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Anthropic API key is not configured. Set ANTHROPIC_API_KEY environment variable.");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 2000);
        requestBody.put("system", systemPrompt);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        String body = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        log.info("Sending analysis request to Anthropic API (model: {})", model);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Anthropic API error ({}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Anthropic API returned status " + response.statusCode());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson.path("content").get(0).path("text").asText();
        String responseModel = responseJson.path("model").asText(model);
        int promptTokens = responseJson.path("usage").path("input_tokens").asInt(0);
        int completionTokens = responseJson.path("usage").path("output_tokens").asInt(0);

        log.info("Anthropic API response received: {} prompt tokens, {} completion tokens", promptTokens, completionTokens);

        return new AnalysisResult(content, responseModel, promptTokens, completionTokens);
    }

    public record AnalysisResult(String content, String model, int promptTokens, int completionTokens) {}
}
