package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.AllocationSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioLlamaNarrator {

    private static final Logger log = LoggerFactory.getLogger(PortfolioLlamaNarrator.class);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(70);

    private final WebClient ollamaWebClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper mapper;

    public PortfolioLlamaNarrator(
            @Qualifier("ollamaWebClient") WebClient ollamaWebClient,
            OllamaProperties ollamaProperties,
            ObjectMapper mapper) {
        this.ollamaWebClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
        this.mapper = mapper;
    }

    public PortfolioDiagnosis narrate(PortfolioSummary summary) {
        PortfolioDiagnosis rules = PortfolioDiagnosis.fromRules(summary);
        if (!canAsk(summary)) {
            return rules;
        }
        String model = portfolioModel();
        try {
            PortfolioDiagnosis parsed = PortfolioDiagnosisText.parse(call(model, prompt(summary)), mapper);
            if (parsed == null) {
                log.warn("자산 진단 Llama 응답을 해석하지 못했습니다. model={}", model);
                return rules;
            }
            log.info("자산 진단 Llama 작성 완료 model={}", model);
            return parsed;
        } catch (RuntimeException exception) {
            log.warn("자산 진단 Llama 호출 실패 model={}: {}", model, exception.getMessage());
            return rules;
        }
    }

    private boolean canAsk(PortfolioSummary summary) {
        return ollamaProperties.isEnabled()
                && summary.totalAssets() > 0
                && summary.assets() != null
                && !summary.assets().isEmpty();
    }

    private String portfolioModel() {
        String model = ollamaProperties.getPortfolioModel();
        if (model == null || model.isBlank()) {
            return "llama2";
        }
        return model.trim();
    }

    private String prompt(PortfolioSummary summary) {
        return String.join("\n",
                "아래 숫자만 사용해 자산 진단을 작성하라. 없는 상품이나 수익률을 만들지 마라.",
                "JSON만 출력하라. 키는 insights, advice.",
                "insights는 1개 이상 4개 이하. 각 항목은 tone(GOOD 또는 WARN)과 text(한국어 한 문장, 80자 이내).",
                "advice는 한국어 한 문장, 120자 이내.",
                "사실:",
                facts(summary));
    }

    private String facts(PortfolioSummary summary) {
        StringBuilder facts = new StringBuilder();
        facts.append("순자산 ").append(summary.netWorth()).append("원\n");
        facts.append("총자산 ").append(summary.totalAssets()).append("원\n");
        facts.append("부채 ").append(summary.totalLiabilities()).append("원\n");
        facts.append("1억 목표 진행 ").append(summary.goalPercentTenths() / 10.0).append("%\n");
        for (AllocationSlice slice : summary.allocation()) {
            if (slice.amount() > 0) {
                facts.append(slice.label()).append(' ').append(slice.amount()).append("원 ")
                        .append(slice.percentTenths() / 10.0).append("%\n");
            }
        }
        return facts.toString();
    }

    private String call(String model, String prompt) {
        String base = ollamaProperties.getBaseUrl() == null ? "" : ollamaProperties.getBaseUrl().replaceAll("/+$", "");
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 한국어 가계 재무 진단가다. JSON만 답한다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false,
                "options", Map.of("temperature", 0.2)
        );
        Map<String, Object> response = ollamaWebClient.post()
                .uri(base + "/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(CALL_TIMEOUT)
                .block();
        return contentOf(response, model);
    }

    private String contentOf(Map<String, Object> response, String model) {
        if (response == null) {
            throw new IllegalStateException("Ollama 응답이 비어 있습니다. model=" + model);
        }
        Object message = response.get("message");
        if (!(message instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Ollama 응답 message가 없습니다. model=" + model);
        }
        Object content = map.get("content");
        if (!(content instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Ollama 응답 content가 비어 있습니다. model=" + model);
        }
        return text;
    }
}
