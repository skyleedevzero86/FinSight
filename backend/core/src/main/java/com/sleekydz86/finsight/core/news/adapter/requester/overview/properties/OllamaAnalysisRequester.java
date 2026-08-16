package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.llama.provider", havingValue = "ollama")
public class OllamaAnalysisRequester implements NewsAiRequester {

    private static final Logger log = LoggerFactory.getLogger(OllamaAnalysisRequester.class);

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper mapper;

    public OllamaAnalysisRequester(
            WebClient webClient,
            OllamaProperties ollamaProperties,
            ObjectMapper mapper) {
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
        this.mapper = mapper;
    }

    @Override
    public AiModel supports() {
        return AiModel.LLAMA;
    }

    @Override
    public AiChatResponse request(AiChatRequest aiChatRequest) {
        if (!ollamaProperties.isEnabled()) {
            throw new IllegalStateException("Ollama가 비활성화되어 있습니다 (ai.ollama.enabled=false)");
        }
        try {
            return requestAsync(aiChatRequest)
                    .timeout(Duration.ofSeconds(ollamaProperties.getTimeoutSeconds()))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Ollama 요청 처리 중 오류: " + e.getMessage(), e);
        }
    }

    Mono<AiChatResponse> requestAsync(AiChatRequest aiChatRequest) {
        try {
            String prompt = buildPrompt(aiChatRequest);
            String chatUrl = normalizeBaseUrl(ollamaProperties.getBaseUrl()) + "/api/chat";

            Map<String, Object> body = Map.of(
                    "model", ollamaProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a Korean financial news analyst. Reply with JSON only."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "stream", false,
                    "options", Map.of("temperature", 0)
            );

            return webClient.post()
                    .uri(chatUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .flatMap(this::parseResponse)
                    .doOnError(e -> log.warn("Ollama API 오류: {}", e.getMessage()));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Ollama 프롬프트 생성 오류: " + e.getMessage(), e));
        }
    }

    private String buildPrompt(AiChatRequest aiChatRequest) throws JsonProcessingException {
        String inputJson = mapper.writeValueAsString(aiChatRequest.getNewsItems());
        return String.join("\n",
                "Analyze each news item. Return ONLY a JSON array.",
                "Each object keys: overView, translatedTitle, translatedContent, categories, sentimentType, sentimentRatio.",
                "categories: subset of [SPY, QQQ, BTC, AAPL, MSFT, NVDA, GOOGL, META, TSLA, NONE].",
                "sentimentType: POSITIVE | NEUTRAL | NEGATIVE.",
                "Input:",
                inputJson
        );
    }

    @SuppressWarnings("unchecked")
    private Mono<AiChatResponse> parseResponse(Map<String, Object> responseBody) {
        try {
            Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
            if (message == null) {
                return Mono.error(new RuntimeException("Ollama 응답에 message 필드가 없습니다"));
            }
            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                return Mono.error(new RuntimeException("Ollama 응답 content가 비어 있습니다"));
            }
            String json = extractJsonArray(content);
            List<AiChatResponse.NewsAnalysis> analyses = mapper.readValue(
                    json,
                    mapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.NewsAnalysis.class)
            );
            return Mono.just(new AiChatResponse(analyses));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Ollama JSON 파싱 오류: " + e.getMessage(), e));
        }
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        throw new IllegalArgumentException("Ollama 응답에서 JSON 배열을 찾을 수 없습니다");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
