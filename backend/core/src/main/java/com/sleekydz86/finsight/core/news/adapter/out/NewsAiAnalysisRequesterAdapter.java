package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.vo.*;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    public List<News> analyseNewses(AiModel model, Content content) {
        totalRequests.incrementAndGet();

        try {
            log.debug("Starting AI analysis with model: {} for content: {}", model, content.getTitle());

            NewsAiRequester requester = selectRequester(model);
            if (requester == null) {
                log.error("No available AI requester for model: {}", model);
                failedRequests.incrementAndGet();
                return executeFallbackStrategy(content, "No requester available");
            }

            var aiChatRequest = createAiChatRequest(content);
            var aiChatResponse = requester.request(aiChatRequest);

            if (aiChatResponse == null || aiChatResponse.getAnalyses() == null || aiChatResponse.getAnalyses().isEmpty()) {
                log.warn("AI analysis returned empty response for model: {}", model);
                return executeFallbackStrategy(content, "Empty AI response");
            }

            List<News> analyzedNews = convertToNews(aiChatResponse, content);

            successfulRequests.incrementAndGet();
            aiModelSelectionService.recordModelUsage(model);

            log.debug("AI analysis completed successfully with model: {}", model);
            return analyzedNews;

        } catch (Exception e) {
            log.error("AI analysis failed with model: {} for content: {}", model, content.getTitle(), e);
            failedRequests.incrementAndGet();
            aiModelSelectionService.recordModelError(model);

            return executeFallbackStrategy(content, "Exception occurred: " + e.getMessage());
        }
    }

    private List<News> executeFallbackStrategy(Content content, String fallbackReason) {
        log.info("Executing fallback strategy for content: {}. Reason: {}", content.getTitle(), fallbackReason);

        try {
            List<News> fallbackResult = tryAlternativeModels(content);
            if (!fallbackResult.isEmpty()) {
                log.info("Fallback to alternative models successful");
                return fallbackResult;
            }

            log.info("Generating basic analysis as final fallback");
            return generateBasicAnalysis(content);

        } catch (Exception e) {
            log.error("Fallback strategy also failed", e);
            return generateBasicAnalysis(content);
        }
    }

    private List<News> tryAlternativeModels(Content content) {

        List<AiModel> alternativeModels = List.of(
                AiModel.GEMMA,
                AiModel.LLAMA,
                AiModel.CHATGPT,
                AiModel.CLAUDE
        );

        for (AiModel alternativeModel : alternativeModels) {
            if (isModelAvailable(alternativeModel)) {
                try {
                    log.debug("Trying alternative model: {}", alternativeModel);
                    NewsAiRequester requester = aiRequesters.get(alternativeModel);
                    var aiChatRequest = createAiChatRequest(content);
                    var aiChatResponse = requester.request(aiChatRequest);

                    if (aiChatResponse != null && !aiChatResponse.getAnalyses().isEmpty()) {
                        log.info("Alternative model {} succeeded", alternativeModel);
                        return convertToNews(aiChatResponse, content);
                    }
                } catch (Exception e) {
                    log.warn("Alternative model {} failed: {}", alternativeModel, e.getMessage());
                    continue;
                }
            }
        }

        return List.of();
    }

    private List<News> generateBasicAnalysis(Content content) {
        log.info("Generating basic analysis for content: {}", content.getTitle());

        try {
            String basicOverview = extractBasicOverview(content);
            SentimentType sentimentType = analyzeBasicSentiment(content);
            List<TargetCategory> categories = extractBasicCategories(content);

            var aiOverview = new AiOverview(
                    basicOverview,
                    sentimentType,
                    0.5,
                    categories
            );

            var news = News.createWithoutAI(
                    new NewsMeta(NewsProvider.ALL, LocalDateTime.now(), "fallback"),
                    content
            ).updateAiAnalysis(
                    basicOverview,
                    content.getTitle(),
                    content.getContent(),
                    categories,
                    sentimentType,
                    0.5
            );

            return List.of(news);

        } catch (Exception e) {
            log.error("Basic analysis generation failed", e);
            // 최후의 수단: 원본 내용만 반환
            var news = News.createWithoutAI(
                    new NewsMeta(NewsProvider.ALL, LocalDateTime.now(), "fallback"),
                    content
            );
            return List.of(news);
        }
    }

    private String extractBasicOverview(Content content) {
        String text = content.getTitle() + " " + content.getContent();

        if (text.toLowerCase().contains("stock") || text.toLowerCase().contains("market")) {
            return "주식 시장 관련 뉴스입니다.";
        } else if (text.toLowerCase().contains("crypto") || text.toLowerCase().contains("bitcoin")) {
            return "암호화폐 관련 뉴스입니다.";
        } else if (text.toLowerCase().contains("earnings") || text.toLowerCase().contains("revenue")) {
            return "기업 실적 관련 뉴스입니다.";
        }

        return "금융 관련 뉴스입니다.";
    }

    private SentimentType analyzeBasicSentiment(Content content) {
        String text = (content.getTitle() + " " + content.getContent()).toLowerCase();

        List<String> positiveWords = List.of("surge", "jump", "rise", "gain", "profit", "growth", "positive", "bullish");
        List<String> negativeWords = List.of("fall", "drop", "decline", "loss", "crash", "negative", "bearish", "concern");

        int positiveCount = countWords(text, positiveWords);
        int negativeCount = countWords(text, negativeWords);

        if (positiveCount > negativeCount) {
            return SentimentType.POSITIVE;
        } else if (negativeCount > positiveCount) {
            return SentimentType.NEGATIVE;
        } else {
            return SentimentType.NEUTRAL;
        }
    }

    private int countWords(String text, List<String> words) {
        return (int) words.stream()
                .filter(text::contains)
                .count();
    }

    private List<TargetCategory> extractBasicCategories(Content content) {
        String text = (content.getTitle() + " " + content.getContent()).toLowerCase();

        List<TargetCategory> categories = new ArrayList<>();

        if (text.contains("apple") || text.contains("iphone")) {
            categories.add(TargetCategory.AAPL);
        }
        if (text.contains("microsoft") || text.contains("azure")) {
            categories.add(TargetCategory.MSFT);
        }
        if (text.contains("nvidia") || text.contains("ai")) {
            categories.add(TargetCategory.NVDA);
        }
        if (text.contains("google") || text.contains("alphabet")) {
            categories.add(TargetCategory.GOOGL);
        }
        if (text.contains("meta") || text.contains("facebook")) {
            categories.add(TargetCategory.META);
        }
        if (text.contains("tesla") || text.contains("electric")) {
            categories.add(TargetCategory.TSLA);
        }
        if (text.contains("bitcoin") || text.contains("crypto")) {
            categories.add(TargetCategory.BTC);
        }
        if (text.contains("spy") || text.contains("s&p")) {
            categories.add(TargetCategory.SPY);
        }
        if (text.contains("qqq") || text.contains("nasdaq")) {
            categories.add(TargetCategory.QQQ);
        }

        if (categories.isEmpty()) {
            categories.add(TargetCategory.NONE);
        }

        return categories;
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
            com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse aiChatResponse,
            Content originalContent) {

        return aiChatResponse.getAnalyses().stream()
                .map(analysis -> {
                    var aiOverview = new com.sleekydz86.finsight.core.news.domain.vo.AiOverview(
                            analysis.getOverView(),
                            analysis.getSentimentType(),
                            analysis.getSentimentRatio(),
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
                                    analysis.getSentimentRatio());
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
}