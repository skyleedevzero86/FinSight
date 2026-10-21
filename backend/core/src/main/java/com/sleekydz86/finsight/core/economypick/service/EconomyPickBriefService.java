package com.sleekydz86.finsight.core.economypick.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.domain.BriefMath;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickBrief;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EconomyPickBriefService {

    private static final Logger log = LoggerFactory.getLogger(EconomyPickBriefService.class);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(12);

    private final WebClient ollamaWebClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper mapper;

    public EconomyPickBriefService(
            @Qualifier("ollamaWebClient") WebClient ollamaWebClient,
            OllamaProperties ollamaProperties,
            ObjectMapper mapper) {
        this.ollamaWebClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
        this.mapper = mapper;
    }

    public EconomyPickBrief brief(String name, String symbol) {
        EconomyPickBrief rules = BriefMath.rules(name, symbol);
        if (!ollamaProperties.isEnabled()) {
            return rules;
        }
        try {
            EconomyPickBrief parsed = BriefMath.ollama(call(prompt(name, symbol, rules)), mapper);
            if (parsed == null) {
                log.warn("경제PICK Llama 응답을 해석하지 못했습니다. symbol={}", symbol);
                return rules;
            }
            log.info("경제PICK Llama 추천 완료 symbol={}", symbol);
            return parsed;
        } catch (RuntimeException exception) {
            log.warn("경제PICK Llama 호출 실패 symbol={}: {}", symbol, exception.getMessage());
            return rules;
        }
    }

    private String prompt(String name, String symbol, EconomyPickBrief rules) {
        return String.join("\n",
                "아래 추정만 사용해 한국어 추천을 작성하라. 없는 실적 숫자는 만들지 마라.",
                "JSON만 출력하라. 키는 headline, body, points.",
                "headline은 60자 이내, body는 120자 이내, points는 2개 이상 4개 이하.",
                "종목: " + name + " (" + symbol + ")",
                "추정: " + rules.headline(),
                "구성: " + String.join(", ", rules.points()));
    }

    private String call(String prompt) {
        String model = model();
        String base = ollamaProperties.getBaseUrl() == null ? "" : ollamaProperties.getBaseUrl().replaceAll("/+$", "");
        String raw = ollamaWebClient.post()
                .uri(base + "/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body(model, prompt))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(CALL_TIMEOUT)
                .block();
        return content(raw, model);
    }

    private Map<String, Object> body(String model, String prompt) {
        return Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 한국어 시황 해설가다. JSON만 답한다."),
                        Map.of("role", "user", "content", prompt)),
                "options", Map.of("temperature", 0.2));
    }

    private String content(String raw, String model) {
        try {
            JsonNode message = mapper.readTree(raw == null ? "" : raw).path("message").path("content");
            if (message.asText("").isBlank()) {
                throw new IllegalStateException("Ollama 응답 content가 비어 있습니다. model=" + model);
            }
            return message.asText();
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Ollama 응답을 읽지 못했습니다. model=" + model, exception);
        }
    }

    private String model() {
        String model = ollamaProperties.getModel();
        return model == null || model.isBlank() ? "llama3.2" : model.trim();
    }
}
