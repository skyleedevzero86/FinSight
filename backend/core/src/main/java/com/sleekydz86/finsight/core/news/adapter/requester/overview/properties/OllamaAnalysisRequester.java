package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.llama.provider", havingValue = "ollama")
public class OllamaAnalysisRequester implements NewsAiRequester {

    private static final Logger log = LoggerFactory.getLogger(OllamaAnalysisRequester.class);

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper mapper;

    public OllamaAnalysisRequester(
            @Qualifier("ollamaWebClient") WebClient webClient,
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
                            Map.of("role", "system", "content", "당신은 금융 뉴스를 한국어로 번역하고 감정을 판단합니다. JSON만 반환합니다."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "stream", false,
                    "format", "json",
                    "options", Map.of("temperature", 0, "num_predict", 400)
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
        List<AiChatRequest.NewsItemRequest> items = aiChatRequest.getNewsItems() == null
                ? List.of()
                : aiChatRequest.getNewsItems();
        List<Map<String, String>> clipped = new ArrayList<>();
        for (AiChatRequest.NewsItemRequest item : items) {
            clipped.add(Map.of("title", clip(item.getTitle()), "content", clip(item.getContent())));
        }
        return String.join("\n",
                "제목과 본문을 자연스러운 한국어로 번역하고 감정을 판단하세요.",
                "translatedTitle, translatedContent, overView는 한글 문장이어야 합니다.",
                "sentimentType은 POSITIVE, NEUTRAL, NEGATIVE 중 하나.",
                "sentimentRatio는 0.0 이상 1.0 이하.",
                "categories는 [SPY, QQQ, BTC, AAPL, MSFT, NVDA, GOOGL, META, TSLA, NONE] 안에서만.",
                "JSON 배열만 반환하세요.",
                mapper.writeValueAsString(clipped)
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
            return Mono.just(new AiChatResponse(readAnalyses(content)));
        } catch (RuntimeException e) {
            return Mono.error(new RuntimeException("Ollama JSON 파싱 오류: " + e.getMessage(), e));
        }
    }

    private List<AiChatResponse.NewsAnalysis> readAnalyses(String content) {
        try {
            JsonNode root = mapper.readTree(jsonPayload(content));
            List<JsonNode> rows = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(rows::add);
            } else if (root.isObject()) {
                rows.add(root);
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Ollama 응답에 분석 결과가 없습니다");
            }
            List<AiChatResponse.NewsAnalysis> analyses = new ArrayList<>();
            for (JsonNode row : rows) {
                analyses.add(toAnalysis(row));
            }
            return analyses;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Ollama JSON을 읽지 못했습니다", e);
        }
    }

    private static AiChatResponse.NewsAnalysis toAnalysis(JsonNode row) {
        String overview = firstText(row, "overView", "overview");
        String title = firstText(row, "translatedTitle", "title");
        String body = firstText(row, "translatedContent", "content");
        if (body.isBlank()) {
            body = overview;
        }
        return new AiChatResponse.NewsAnalysis(
                overview,
                title,
                body,
                categoriesOf(row.get("categories")),
                sentimentOf(firstText(row, "sentimentType")),
                ratioOf(row.get("sentimentRatio")));
    }

    static String jsonPayload(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Ollama 응답이 비어 있습니다");
        }
        String trimmed = stripFence(content.trim());
        int arrayAt = trimmed.indexOf('[');
        int objectAt = trimmed.indexOf('{');
        if (arrayAt >= 0 && (objectAt < 0 || arrayAt < objectAt)) {
            int end = trimmed.lastIndexOf(']');
            if (end > arrayAt) {
                return trimmed.substring(arrayAt, end + 1);
            }
        }
        if (objectAt >= 0) {
            int end = trimmed.lastIndexOf('}');
            if (end > objectAt) {
                return trimmed.substring(objectAt, end + 1);
            }
        }
        throw new IllegalArgumentException("Ollama 응답에서 JSON을 찾을 수 없습니다");
    }

    private static String stripFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }
        int start = content.indexOf('\n');
        int end = content.lastIndexOf("```");
        if (start >= 0 && end > start) {
            return content.substring(start + 1, end).trim();
        }
        return content;
    }

    private static String clip(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 280 ? trimmed : trimmed.substring(0, 280);
    }

    private static String firstText(JsonNode row, String... names) {
        if (row == null) {
            return "";
        }
        for (String name : names) {
            JsonNode node = row.get(name);
            if (node != null && !node.isNull() && !node.asText("").isBlank()) {
                return node.asText("").trim();
            }
        }
        return "";
    }

    private static SentimentType sentimentOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String upper = raw.toUpperCase(Locale.ROOT);
        if (upper.contains("POSITIVE")) {
            return SentimentType.POSITIVE;
        }
        if (upper.contains("NEGATIVE")) {
            return SentimentType.NEGATIVE;
        }
        if (upper.contains("NEUTRAL")) {
            return SentimentType.NEUTRAL;
        }
        return null;
    }

    private static List<TargetCategory> categoriesOf(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<TargetCategory> categories = new ArrayList<>();
        for (JsonNode item : node) {
            TargetCategory category = categoryOf(item.asText(""));
            if (category != null) {
                categories.add(category);
            }
        }
        return categories;
    }

    private static TargetCategory categoryOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TargetCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Double ratioOf(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        double ratio = node.asDouble();
        if (ratio < 0 || ratio > 1) {
            return null;
        }
        return ratio;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
