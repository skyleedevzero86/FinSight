package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
                        requester -> requester
                ));
        this.aiModelSelectionService = aiModelSelectionService;
    }

    @Override
    public List<News> analyseNewses(AiModel model, Content content) {
        totalRequests.incrementAndGet();

        try {
            log.debug("Starting AI analysis with model: {} for content: {}", model, content.getTitle());

            NewsAiRequester requester = selectRequester(model);
            if (requester == null) {
                log.error("No available AI requester for model: {}", model);
                failedRequests.incrementAndGet();
                return List.of();
            }

            var aiChatRequest = createAiChatRequest(content);
            var aiChatResponse = requester.request(aiChatRequest);

            List<News> analyzedNews = convertToNews(aiChatResponse, content);

            successfulRequests.incrementAndGet();
            aiModelSelectionService.recordModelUsage(model);

            log.debug("AI analysis completed successfully with model: {}", model);
            return analyzedNews;

        } catch (Exception e) {
            log.error("AI analysis failed with model: {} for content: {}", model, content.getTitle(), e);
            failedRequests.incrementAndGet();
            aiModelSelectionService.recordModelError(model);

            return retryWithFallbackModel(content);
        }
    }

    @Override
    public CompletableFuture<List<News>> analyseNewsesAsync(AiModel model, Content content) {
        return CompletableFuture.supplyAsync(() -> analyseNewses(model, content));
    }

    @Override
    public List<News> analyseNewsesBatch(AiModel model, List<Content> contents) {
        log.info("Starting batch AI analysis with model: {} for {} contents", model, contents.size());

        return contents.parallelStream()
                .map(content -> analyseNewses(model, content))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isModelAvailable(AiModel model) {
        return aiRequesters.containsKey(model);
    }

    @Override
    public List<AiModel> getAvailableModels() {
        return aiRequesters.keySet().stream().toList();
    }

    private NewsAiRequester selectRequester(AiModel model) {
        if (isModelAvailable(model)) {
            return aiRequesters.get(model);
        }

        log.warn("Requested model {} is not available, selecting alternative", model);
        return aiModelSelectionService.selectModelByPriority();
    }

    private com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest createAiChatRequest(Content content) {
        return new com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest(
                generatePrompt(content),
                List.of(new com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest.NewsItemRequest(
                        content.getTitle(),
                        content.getContent()
                ))
        );
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
            com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse aiChatResponse,
            Content originalContent) {

        return aiChatResponse.getAnalyses().stream()
                .map(analysis -> {
                    var aiOverview = new com.sleekydz86.finsight.core.news.domain.vo.AiOverview(
                            analysis.getOverView(),
                            analysis.getSentimentType(),
                            analysis.getSentimentRatio(),
                            analysis.getCategories()
                    );

                    var translatedContent = new com.sleekydz86.finsight.core.news.domain.vo.Content(
                            analysis.getTranslatedTitle(),
                            analysis.getTranslatedContent()
                    );

                    var newsMeta = new com.sleekydz86.finsight.core.news.domain.vo.NewsMeta(
                            com.sleekydz86.finsight.core.global.NewsProvider.ALL,
                            java.time.LocalDateTime.now(),
                            "AI Generated"
                    );

                    return News.createWithoutAI(newsMeta, originalContent)
                            .updateAiAnalysis(
                                    analysis.getOverView(),
                                    analysis.getTranslatedTitle(),
                                    analysis.getTranslatedContent(),
                                    analysis.getCategories(),
                                    analysis.getSentimentType(),
                                    analysis.getSentimentRatio()
                            );
                })
                .collect(Collectors.toList());
    }

    private List<News> retryWithFallbackModel(Content content) {
        try {
            log.info("Retrying AI analysis with fallback model");
            NewsAiRequester fallbackRequester = aiModelSelectionService.selectModelByPriority();
            AiModel fallbackModel = fallbackRequester.supports();
            return analyseNewses(fallbackModel, content);
        } catch (Exception e) {
            log.error("Fallback AI analysis also failed", e);
            return List.of();
        }
    }

    public AnalysisMetrics getAnalysisMetrics() {
        return new AnalysisMetrics(
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                getSuccessRate()
        );
    }

    private double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0) return 0.0;
        return (double) successfulRequests.get() / total * 100;
    }

    public record AnalysisMetrics(
            int totalRequests,
            int successfulRequests,
            int failedRequests,
            double successRate
    ) {}
}