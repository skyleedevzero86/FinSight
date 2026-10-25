package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.vo.*;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class NewsAiAnalysisRequesterAdapter implements NewsAiAnalysisRequesterPort {

    private static final Logger log = LoggerFactory.getLogger(NewsAiAnalysisRequesterAdapter.class);

    private final Map<AiModel, NewsAiRequester> aiRequesters;
    private final AiModelSelectionService aiModelSelectionService;
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);

    public NewsAiAnalysisRequesterAdapter(
            List<NewsAiRequester> aiRequesters,
            AiModelSelectionService aiModelSelectionService) {
        this.aiRequesters = aiRequesters.stream()
                .collect(Collectors.toMap(
                        NewsAiRequester::supports,
                        requester -> requester));
        this.aiModelSelectionService = aiModelSelectionService;
    }

    @Override
    @CircuitBreaker(name = "newsAiCircuitBreaker", fallbackMethod = "analyseNewsesFallback")
    public List<News> analyseNewses(AiModel model, Content content) {
        totalRequests.incrementAndGet();
        log.debug("AI 분석 시작 - 모델: {}, 제목: {}", model, content.getTitle());
        NewsAiRequester requester = aiRequesters.get(model);
        if (requester == null) {
            log.error("모델 {}에 사용 가능한 AI 요청기가 없습니다", model);
            failedRequests.incrementAndGet();
            throw new MissingAiRequester(model);
        }
        return completeSelectedCall(model, content, requestSelected(model, content, requester));
    }

    private List<News> analyseNewsesFallback(AiModel model, Content content, Throwable throwable) {
        String title = content == null ? "" : content.getTitle();
        if (throwable instanceof CallNotPermittedException) {
            log.warn("AI 분석 서킷이 열려 호출을 건너뜁니다. 모델: {}, 제목: {}", model, title);
            return List.of();
        }
        if (content == null) {
            log.error("모든 모델이 실패했습니다. 모델: {}, 제목: {}, 원인: {}", model, title, throwable.getMessage());
            return List.of();
        }
        return alternativesOrFailure(model, content, throwable);
    }

    private AiChatResponse requestSelected(
            AiModel model, Content content, NewsAiRequester requester) {
        try {
            return requester.request(createAiChatRequest(content));
        } catch (Exception exception) {
            failedRequests.incrementAndGet();
            aiModelSelectionService.recordModelError(model);
            log.error("AI 분석 실패 - 모델: {}, 제목: {}", model, content.getTitle(), exception);
            throw new IllegalStateException("AI 분석에 실패했습니다. 모델: " + model, exception);
        }
    }

    private List<News> completeSelectedCall(
            AiModel model,
            Content content,
            AiChatResponse response) {
        if (response == null || response.getAnalyses() == null || response.getAnalyses().isEmpty()) {
            failedRequests.incrementAndGet();
            log.warn("모델 {} AI 분석 응답이 비어 있습니다", model);
            throw new EmptyAiResponse(model);
        }
        successfulRequests.incrementAndGet();
        aiModelSelectionService.recordModelUsage(model);
        log.debug("AI 분석 완료 - 모델: {}", model);
        return convertToNews(response, content);
    }

    private List<News> alternativesOrFailure(AiModel model, Content content, Throwable throwable) {
        List<News> alternative = tryAlternativeModels(content, model);
        if (!alternative.isEmpty()) {
            log.info("대체 모델 폴백 성공");
            return alternative;
        }
        log.error("모든 모델이 실패했습니다. 모델: {}, 제목: {}, 원인: {}",
                model, content.getTitle(), throwable.getMessage());
        if (throwable instanceof EmptyAiResponse || throwable instanceof MissingAiRequester) {
            return generateBasicAnalysis(content);
        }
        if (throwable instanceof IllegalStateException illegalState) {
            throw illegalState;
        }
        throw new IllegalStateException("AI 분석에 실패했습니다. 모델: " + model, throwable);
    }

    private List<News> tryAlternativeModels(Content content, AiModel failedModel) {

        List<AiModel> alternativeModels = List.of(
                AiModel.GEMMA,
                AiModel.LLAMA,
                AiModel.CHATGPT,
                AiModel.CLAUDE
        );

        for (AiModel alternativeModel : alternativeModels) {
            if (alternativeModel == failedModel || !isModelAvailable(alternativeModel)) {
                continue;
            }
            try {
                log.debug("대체 모델 시도: {}", alternativeModel);
                NewsAiRequester requester = aiRequesters.get(alternativeModel);
                var aiChatRequest = createAiChatRequest(content);
                var aiChatResponse = requester.request(aiChatRequest);

                if (aiChatResponse != null && aiChatResponse.getAnalyses() != null
                        && !aiChatResponse.getAnalyses().isEmpty()) {
                    log.info("대체 모델 {} 성공", alternativeModel);
                    return convertToNews(aiChatResponse, content);
                }
            } catch (Exception e) {
                log.warn("대체 모델 {} 실패: {}", alternativeModel, e.getMessage());
            }
        }

        return List.of();
    }

    private List<News> generateBasicAnalysis(Content content) {
        log.warn("AI 분석을 만들지 못했습니다. 제목: {}", content.getTitle());
        return List.of();
    }

    @Override
    public boolean isModelAvailable(AiModel model) {
        return aiRequesters.containsKey(model);
    }

    @Override
    public List<AiModel> getAvailableModels() {
        return aiRequesters.keySet().stream().toList();
    }

    private com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest createAiChatRequest(
            Content content) {
        return new com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest(
                generatePrompt(content),
                List.of(new com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest.NewsItemRequest(
                        content.getTitle(),
                        content.getContent())));
    }

    private String generatePrompt(Content content) {
        return String.format("""
                다음 뉴스 기사를 분석해주세요:

                제목: %s
                내용: %s

                다음 형식으로 JSON 응답을 제공해주세요:
                {
                    "overview": "뉴스 요약 (한국어)",
                    "translatedTitle": "번역된 제목 (한국어)",
                    "translatedContent": "번역된 내용 (한국어)",
                    "categories": ["SPY", "QQQ", "BTC", "AAPL", "MSFT", "NVDA", "GOOGL", "META", "TSLA", "NONE"],
                    "sentimentType": "POSITIVE|NEUTRAL|NEGATIVE",
                    "sentimentScore": 0.0-1.0
                }

                분석 시 다음 사항을 고려해주세요:
                1. 금융/투자 관련 키워드 식별
                2. 시장에 미치는 영향 분석
                3. 감정 분석 (긍정/부정/중립)
                4. 관련 주식/암호화폐 카테고리 분류
                """, content.getTitle(), content.getContent());
    }

    private List<News> convertToNews(
            AiChatResponse aiChatResponse,
            Content originalContent) {

        return aiChatResponse.getAnalyses().stream()
                .map(analysis -> {
                    double score = analysis.getSentimentRatio() == null ? 0.0 : analysis.getSentimentRatio();
                    var aiOverview = new com.sleekydz86.finsight.core.news.domain.vo.AiOverview(
                            analysis.getOverView(),
                            analysis.getSentimentType(),
                            score,
                            analysis.getCategories());

                    var translatedContent = new com.sleekydz86.finsight.core.news.domain.vo.Content(
                            analysis.getTranslatedTitle(),
                            analysis.getTranslatedContent());

                    var newsMeta = new com.sleekydz86.finsight.core.news.domain.vo.NewsMeta(
                            com.sleekydz86.finsight.core.global.NewsProvider.ALL,
                            java.time.LocalDateTime.now(),
                            "AI Generated");

                    return News.createWithoutAI(newsMeta, originalContent)
                            .updateAiAnalysis(
                                    analysis.getOverView(),
                                    analysis.getTranslatedTitle(),
                                    analysis.getTranslatedContent(),
                                    analysis.getCategories(),
                                    analysis.getSentimentType(),
                                    analysis.getSentimentRatio() == null ? 0.0 : analysis.getSentimentRatio());
                })
                .collect(Collectors.toList());
    }

    private List<News> retryWithFallbackModel(Content content) {
        try {
            log.info("폴백 모델로 AI 분석 재시도");
            NewsAiRequester fallbackRequester = aiModelSelectionService.selectModelByPriority();
            AiModel fallbackModel = fallbackRequester.supports();
            return analyseNewses(fallbackModel, content);
        } catch (Exception e) {
            log.error("폴백 AI 분석도 실패", e);
            return List.of();
        }
    }

    public AnalysisMetrics getAnalysisMetrics() {
        return new AnalysisMetrics(
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                getSuccessRate());
    }

    private double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0)
            return 0.0;
        return (double) successfulRequests.get() / total * 100;
    }

    public record AnalysisMetrics(
            int totalRequests,
            int successfulRequests,
            int failedRequests,
            double successRate) {
    }

    private static final class EmptyAiResponse extends IllegalStateException {
        private EmptyAiResponse(AiModel model) {
            super("AI 분석 응답이 비어 있습니다. 모델: " + model);
        }
    }

    private static final class MissingAiRequester extends IllegalStateException {
        private MissingAiRequester(AiModel model) {
            super("사용 가능한 AI 요청기가 없습니다. 모델: " + model);
        }
    }
}