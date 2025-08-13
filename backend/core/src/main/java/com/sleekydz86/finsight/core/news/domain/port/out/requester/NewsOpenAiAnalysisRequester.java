package com.sleekydz86.finsight.core.news.domain.port.out.requester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.properties.OpenAiProperties;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class NewsOpenAiAnalysisRequester implements NewsAiRequester {

    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper mapper;

    public NewsOpenAiAnalysisRequester(WebClient webClient, OpenAiProperties openAiProperties, ObjectMapper mapper) {
        this.webClient = webClient;
        this.openAiProperties = openAiProperties;
        this.mapper = mapper;
    }

    @Override
    public AiModel supports() {
        return AiModel.CHATGPT;
    }

    @Override
    public AiChatResponse request(AiChatRequest aiChatRequest) {
        try {
            return requestAsync(aiChatRequest)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 요청 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 비동기 요청 처리를 위한 메서드
     */
    /**
     * 비동기 요청 처리를 위한 메서드
     */
    public Mono<AiChatResponse> requestAsync(AiChatRequest aiChatRequest) {
        try {
            String prompt = getPrompt(aiChatRequest);

            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a professional Korean translator and sentiment analyzer."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0
            );

            return webClient.post()
                    .uri(openAiProperties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .flatMap(this::parseResponse)
                    .onErrorMap(throwable -> handleError(throwable));

        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("프롬프트 생성 중 JSON 처리 오류: " + e.getMessage(), e));
        }
    }

    private String getPrompt(AiChatRequest aiChatRequest) throws JsonProcessingException {
        String inputJson = mapper.writeValueAsString(aiChatRequest.getNewsItems());

        return String.join("\n",
                "You are a Korean translator, summarizer, categorizer, and sentiment analyzer.",
                "",
                "You MUST return the result in **valid JSON** with the exact keys and value rules below.",
                "Do not include any explanations or extra text, only return JSON.",
                "",
                "Rules for fields for each news item:",
                "- overView: 1~3 sentence summary in Korean.",
                "- translatedTitle: Korean translation of the original title.",
                "- translatedContent: Korean translation of the original content.",
                "- categories: A list of strings, each must be one of the following exactly: [SPY, QQQ, BTC, AAPL, MSFT, NVDA, GOOGL, META, TSLA, NONE].",
                "  If no relevant ticker exists, return [\"NONE\"].",
                "- sentimentType: One of exactly [POSITIVE, NEUTRAL, NEGATIVE].",
                "  If sentiment is unclear, return \"NEUTRAL\".",
                "- sentimentRatio: A number between 0.0 and 1.0 indicating confidence.",
                "",
                "Input will be a JSON array of news objects in this format:",
                "[",
                "  { \"title\": \"...\", \"content\": \"...\" },",
                "  ...",
                "]",
                "",
                "Return JSON in this format (array of responses):",
                "[",
                "  {",
                "    \"overView\": \"...\",",
                "    \"translatedTitle\": \"...\",",
                "    \"translatedContent\": \"...\",",
                "    \"categories\": [\"...\"],",
                "    \"sentimentType\": \"POSITIVE | NEUTRAL | NEGATIVE\",",
                "    \"sentimentRatio\": 0.0",
                "  },",
                "  ...",
                "]",
                "",
                "Here is the input:",
                inputJson
        );
    }

    private Mono<AiChatResponse> parseResponse(Map<String, Object> responseBody) {
        try {
            String content = extractContent(responseBody);

            List<AiChatResponse.NewsAnalysis> analyses = mapper.readValue(
                    content,
                    mapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.NewsAnalysis.class)
            );

            return Mono.just(new AiChatResponse(analyses));

        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("JSON 파싱 오류: " + e.getMessage(), e));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("응답 처리 중 오류: " + e.getMessage(), e));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 choices가 없거나 비어 있습니다.");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

            if (message == null) {
                throw new RuntimeException("OpenAI 응답에 message가 없습니다.");
            }

            String content = (String) message.get("content");
            return Objects.requireNonNull(content, "OpenAI 응답 content가 null입니다.");

        } catch (ClassCastException e) {
            throw new RuntimeException("OpenAI 응답 형식이 예상과 다릅니다: " + responseBody, e);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 응답 파싱 중 오류가 발생했습니다: " + responseBody, e);
        }
    }

    private Throwable handleError(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return throwable;
        }
        return new RuntimeException("OpenAI API 호출 중 예상치 못한 오류가 발생했습니다.", throwable);
    }
}