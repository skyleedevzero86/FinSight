package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetection;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetection.Chip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PortfolioShareDetectionService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioShareDetectionService.class);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(8);
    private static final long CACHE_MS = 60_000L;

    private final PortfolioShareJpaRepository shareRepository;
    private final PortfolioShareReportJpaRepository reportRepository;
    private final WebClient ollamaWebClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper mapper;
    private volatile PortfolioShareDetection cached;
    private volatile long cachedAt;

    public PortfolioShareDetectionService(
            PortfolioShareJpaRepository shareRepository,
            PortfolioShareReportJpaRepository reportRepository,
            @Qualifier("ollamaWebClient") WebClient ollamaWebClient,
            OllamaProperties ollamaProperties,
            ObjectMapper mapper) {
        this.shareRepository = shareRepository;
        this.reportRepository = reportRepository;
        this.ollamaWebClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
        this.mapper = mapper;
    }

    public PortfolioShareDetection detect() {
        long now = System.currentTimeMillis();
        PortfolioShareDetection current = cached;
        if (current != null && now - cachedAt < CACHE_MS) {
            return current;
        }
        PortfolioShareDetection next = load();
        cached = next;
        cachedAt = now;
        return next;
    }

    private PortfolioShareDetection load() {
        List<PortfolioShareJpaEntity> shares = shareRepository
                .findByVisibilityOrderByCreatedAtDescIdDesc("PUBLIC", PageRequest.of(0, 12))
                .getContent();
        Map<String, Set<Long>> hits = emptyHits();
        for (PortfolioShareJpaEntity share : shares) {
            long id = share.getId() == null ? 0L : share.getId();
            for (String label : PortfolioShareReasons.match(share.getMessage())) {
                hits.get(label).add(id);
            }
        }
        for (PortfolioShareReportJpaEntity report : reportRepository.findAll()) {
            Set<Long> bucket = hits.get(report.getReason());
            if (bucket != null && report.getShareId() != null) {
                bucket.add(report.getShareId());
            }
        }
        boolean llama = enrich(shares, hits);
        List<Chip> chips = chips(hits);
        String note = llama
                ? "Llama 2가 공개 공유 글을 보고, 신고 사유와 함께 집계했습니다."
                : "공개 공유 글의 문구와 신고 사유로 집계했습니다.";
        if (chips.isEmpty()) {
            note = "탐지된 항목이 없습니다. " + note;
        }
        return new PortfolioShareDetection(llama ? "LLAMA2" : "RULE", note, chips);
    }

    private boolean enrich(List<PortfolioShareJpaEntity> shares, Map<String, Set<Long>> hits) {
        if (!ollamaProperties.isEnabled() || shares.isEmpty()) {
            return false;
        }
        try {
            Set<Long> ids = new LinkedHashSet<>();
            for (PortfolioShareJpaEntity share : shares) {
                if (share.getId() != null) {
                    ids.add(share.getId());
                }
            }
            boolean accepted = PortfolioShareDetectionText.merge(call(prompt(shares)), ids, hits, mapper);
            if (accepted) {
                log.info("공유 자동 탐지 Llama 반영 model={}", model());
            }
            return accepted;
        } catch (RuntimeException exception) {
            log.warn("공유 자동 탐지 Llama 호출 실패 model={}: {}", model(), exception.getMessage());
            return false;
        }
    }

    private String call(String prompt) {
        String base = ollamaProperties.getBaseUrl() == null ? "" : ollamaProperties.getBaseUrl().replaceAll("/+$", "");
        Map<String, Object> body = Map.of(
                "model", model(),
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 한국어 금융 글 분류기다. JSON만 답한다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false,
                "options", Map.of("temperature", 0)
        );
        Map<String, Object> response = ollamaWebClient.post()
                .uri(base + "/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(CALL_TIMEOUT)
                .block();
        return contentOf(response);
    }

    private String prompt(List<PortfolioShareJpaEntity> shares) {
        StringBuilder text = new StringBuilder();
        text.append("라벨은 다음만 쓴다: ").append(String.join(", ", PortfolioShareReasons.LABELS)).append('\n');
        text.append("JSON만 출력. 형식 {\"hits\":[{\"id\":1,\"labels\":[\"오픈채팅 URL\"]}]}").append('\n');
        text.append("해당 없으면 labels 를 빈 배열로 둔다.\n");
        for (PortfolioShareJpaEntity share : shares) {
            String message = share.getMessage() == null ? "" : share.getMessage();
            if (message.length() > 180) {
                message = message.substring(0, 180);
            }
            text.append("id=").append(share.getId()).append(' ').append(message).append('\n');
        }
        return text.toString();
    }

    private String contentOf(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Ollama 응답이 비어 있습니다.");
        }
        Object message = response.get("message");
        if (!(message instanceof Map<?, ?> map) || !(map.get("content") instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Ollama 응답 content가 비어 있습니다.");
        }
        return text;
    }

    private String model() {
        String model = ollamaProperties.getPortfolioModel();
        return model == null || model.isBlank() ? "llama2" : model.trim();
    }

    private Map<String, Set<Long>> emptyHits() {
        Map<String, Set<Long>> hits = new LinkedHashMap<>();
        for (String label : PortfolioShareReasons.LABELS) {
            hits.put(label, new LinkedHashSet<>());
        }
        return hits;
    }

    private List<Chip> chips(Map<String, Set<Long>> hits) {
        List<Chip> chips = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : hits.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                chips.add(new Chip(entry.getKey(), entry.getValue().size()));
            }
        }
        return chips;
    }
}
