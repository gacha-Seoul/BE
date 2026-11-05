package com.hackathon.seoul.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 호환 API 호출 (LLM_MOCK=false 일 때 활성화).
 * base-url만 바꾸면 OpenAI 호환 엔드포인트를 제공하는 다른 서비스도 사용 가능.
 * 타임아웃을 넘기면 예외 → ChatService가 기본 응답으로 대체한다.
 */
@Component
@ConditionalOnProperty(name = "llm.mock", havingValue = "false")
public class OpenAiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiLlmClient(@Value("${llm.base-url}") String baseUrl,
                           @Value("${llm.api-key}") String apiKey,
                           @Value("${llm.model}") String model,
                           @Value("${llm.timeout-seconds:15}") int timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM_MOCK=false 인데 LLM_API_KEY가 비어 있어요. .env를 확인하세요.");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String completeJson(LlmPurpose purpose, String systemPrompt, List<LlmMessage> messages) {
        List<Map<String, String>> body = new ArrayList<>();
        body.add(Map.of("role", "system", "content", systemPrompt));
        for (LlmMessage m : messages) {
            body.add(Map.of("role", m.role(), "content", m.content()));
        }

        JsonNode res = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "messages", body,
                        "temperature", 0.7,
                        "response_format", Map.of("type", "json_object")))
                .retrieve()
                .body(JsonNode.class);

        if (res == null) throw new IllegalStateException("LLM 응답이 비어 있음");
        return res.path("choices").path(0).path("message").path("content").asText();
    }
}
