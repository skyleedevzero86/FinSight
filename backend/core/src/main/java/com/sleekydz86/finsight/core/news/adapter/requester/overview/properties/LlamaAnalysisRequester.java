package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LlamaAnalysisRequester implements NewsAiRequester {

    private static final Logger log = LoggerFactory.getLogger(LlamaAnalysisRequester.class);

    private final WebClient webClient;
    private final HuggingFaceProperties huggingFaceProperties;
    private final ObjectMapper mapper;

    public LlamaAnalysisRequester(WebClient webClient,
                                  HuggingFaceProperties huggingFaceProperties,
                                  ObjectMapper mapper) {
        this.webClient = webClient;
        this.huggingFaceProperties = huggingFaceProperties;
        this.mapper = mapper;
    }

    @Override
    public AiModel supports() {
        return AiModel.LLAMA;
    }

    @Override
    public AiChatResponse request(AiChatRequest aiChatRequest) throws Exception {
        try {
            return requestAsync(aiChatRequest)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Hugging Face 요청 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    public Mono<AiChatResponse> requestAsync(AiChatRequest aiChatRequest) {
        try {
            String prompt = getPrompt(aiChatRequest);

            Map<String, Object> requestBody = Map.of("inputs", prompt);

            return webClient.post()
                    .uri(huggingFaceProperties.getModels().get("llama").getUrl())
                    .header("Authorization", "Bearer " + huggingFaceProperties.getToken())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(this::parseResponse)
                    .onErrorMap(throwable -> handleError(throwable));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("프롬프트 생성 중 오류: " + e.getMessage(), e));
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

    private Mono<AiChatResponse> parseResponse(String response) {
        try {
            String jsonPart = extractJsonFromResponse(response);

            List<AiChatResponse.NewsAnalysis> analyses = mapper.readValue(
                    jsonPart,
                    mapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.NewsAnalysis.class)
            );

            return Mono.just(new AiChatResponse(analyses));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Hugging Face 응답 파싱 오류: " + e.getMessage(), e));
        }
    }

    private String extractJsonFromResponse(String response) {
        int jsonStart = response.indexOf('[');
        int jsonEnd = response.lastIndexOf(']') + 1;

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd);
        }

        throw new RuntimeException("응답에서 JSON을 찾을 수 없습니다: " + response);
    }

    private Throwable handleError(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return throwable;
        }
        return new RuntimeException("Hugging Face API 호출 중 예상치 못한 오류가 발생했습니다.", throwable);
    }
}