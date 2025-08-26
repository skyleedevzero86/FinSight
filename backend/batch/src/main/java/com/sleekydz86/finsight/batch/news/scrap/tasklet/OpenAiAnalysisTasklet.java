package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OpenAiAnalysisTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAnalysisTasklet.class);

    private final NewsJpaRepository newsJpaRepository;
    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;
    private final AiModelSelectionService aiModelSelectionService;

    private final AtomicInteger processedNewsCount = new AtomicInteger(0);
    private final AtomicInteger successfulAnalysisCount = new AtomicInteger(0);
    private final AtomicInteger failedAnalysisCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final ConcurrentHashMap<AiModel, AtomicInteger> modelUsageCount = new ConcurrentHashMap<>();

    public OpenAiAnalysisTasklet(
            NewsJpaRepository newsJpaRepository,
            NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort,
            AiModelSelectionService aiModelSelectionService) {
        this.newsJpaRepository = newsJpaRepository;
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
        this.aiModelSelectionService = aiModelSelectionService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting AI analysis tasklet");

        int pageSize = 50;
        int pageNumber = 0;
        boolean hasMoreData = true;

        try {
            while (hasMoreData) {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                Page<NewsJpaEntity> newsPage = newsJpaRepository.findByOverviewIsNull(pageable);

                if (newsPage.isEmpty()) {
                    log.info("No more news articles to analyze. Stopping at page: {}", pageNumber);
                    hasMoreData = false;
                    break;
                }

                log.info("Processing page {} with {} news articles", pageNumber, newsPage.getContent().size());

                List<CompletableFuture<Boolean>> analysisFutures = newsPage.getContent().stream()
                        .map(this::processNewsAnalysis)
                        .toList();

                CompletableFuture.allOf(analysisFutures.toArray(new CompletableFuture[0])).join();

                long successfulCount = analysisFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Boolean::booleanValue)
                        .count();

                log.info("Page {} completed. Successfully analyzed: {}/{}",
                        pageNumber, successfulCount, newsPage.getContent().size());

                pageNumber++;

                if (pageNumber > 100) {
                    log.warn("Reached maximum page limit (100). Stopping processing.");
                    break;
                }
            }

            log.info("AI analysis tasklet completed successfully. Total processed: {}", processedNewsCount.get());

        } catch (Exception e) {
            log.error("Error during AI analysis tasklet execution", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    private CompletableFuture<Boolean> processNewsAnalysis(NewsJpaEntity newsEntity) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                log.debug("Starting AI analysis for news: {}", newsEntity.getId());
                processedNewsCount.incrementAndGet();
                AiModel selectedModel = selectOptimalModel(newsEntity);
                log.debug("Selected AI model: {} for news: {}", selectedModel, newsEntity.getId());

                Content aiChatRequest = new Content(
                        newsEntity.getOriginalTitle(),
                        newsEntity.getOriginalContent()
                );

                List<News> analyzedNews = newsAiAnalysisRequesterPort.analyseNewses(selectedModel, aiChatRequest);

                if (analyzedNews != null && !analyzedNews.isEmpty()) {
                    updateNewsEntityWithAnalysis(newsEntity, analyzedNews.get(0));
                    newsJpaRepository.save(newsEntity);

                    successfulAnalysisCount.incrementAndGet();
                    modelUsageCount.computeIfAbsent(selectedModel, k -> new AtomicInteger(0)).incrementAndGet();

                    long processingTime = System.currentTimeMillis() - startTime;
                    totalProcessingTime.addAndGet(processingTime);

                    log.debug("AI analysis completed for news: {} in {}ms using model: {}",
                            newsEntity.getId(), processingTime, selectedModel);
                    return true;

                } else {
                    log.warn("AI analysis returned no results for news: {}", newsEntity.getId());
                    failedAnalysisCount.incrementAndGet();
                    return false;
                }

            } catch (Exception e) {
                log.error("Error during AI analysis for news: {}", newsEntity.getId(), e);
                failedAnalysisCount.incrementAndGet();
                return false;
            }
        });
    }

    private AiModel selectOptimalModel(NewsJpaEntity newsEntity) {
        String content = newsEntity.getOriginalContent();

        if (content.length() > 5000) {
            return AiModel.CHATGPT;
        } else if (content.length() > 2000) {
            return AiModel.CHATGPT;
        } else {
            return AiModel.GEMMA;
        }
    }

    private void updateNewsEntityWithAnalysis(NewsJpaEntity newsEntity, News analyzedNews) {
        try {
            if (analyzedNews.getAiOverView() != null) {
                newsEntity.setOverview(analyzedNews.getAiOverView().getOverview());

                if (analyzedNews.getAiOverView().getSentimentType() != null) {
                    newsEntity.setSentimentType(analyzedNews.getAiOverView().getSentimentType());
                }

                double sentimentScore = analyzedNews.getAiOverView().getSentimentScore();
                if (sentimentScore != 0.0) {
                    newsEntity.setSentimentScore(sentimentScore);
                }
            }

            if (analyzedNews.getTranslatedContent() != null) {
                newsEntity.setTranslatedTitle(analyzedNews.getTranslatedContent().getTitle());
                newsEntity.setTranslatedContent(analyzedNews.getTranslatedContent().getContent());
            }

            log.debug("Updated news entity {} with AI analysis results", newsEntity.getId());

        } catch (Exception e) {
            log.error("Error updating news entity with AI analysis results: {}", newsEntity.getId(), e);
        }
    }

    public AiAnalysisMetrics getAiAnalysisMetrics() {
        return new AiAnalysisMetrics(
                processedNewsCount.get(),
                successfulAnalysisCount.get(),
                failedAnalysisCount.get(),
                totalProcessingTime.get(),
                modelUsageCount.entrySet().stream()
                        .collect(ConcurrentHashMap::new,
                                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                                ConcurrentHashMap::putAll)
        );
    }

    public void resetMetrics() {
        processedNewsCount.set(0);
        successfulAnalysisCount.set(0);
        failedAnalysisCount.set(0);
        totalProcessingTime.set(0);
        modelUsageCount.clear();
        log.info("AI analysis metrics reset");
    }

    public record AiAnalysisMetrics(
            int processedNewsCount,
            int successfulAnalysisCount,
            int failedAnalysisCount,
            long totalProcessingTime,
            ConcurrentHashMap<AiModel, Integer> modelUsageCount
    ) {
        public double getSuccessRate() {
            if (processedNewsCount == 0) return 0.0;
            return (double) successfulAnalysisCount / processedNewsCount * 100;
        }

        public double getAverageProcessingTime() {
            if (successfulAnalysisCount == 0) return 0.0;
            return (double) totalProcessingTime / successfulAnalysisCount;
        }

        public AiModel getMostUsedModel() {
            return modelUsageCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(AiModel.CHATGPT);
        }
    }
}