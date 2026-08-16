package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.global.exception.AiAnalysisFailedException;
import com.sleekydz86.finsight.core.global.exception.DatabaseConnectionException;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
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
        log.info("AI 분석 태스크릿 시작");

        int pageSize = 50;
        int pageNumber = 0;
        boolean hasMoreData = true;

        try {
            while (hasMoreData) {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                Page<NewsJpaEntity> newsPage = newsJpaRepository.findByOverviewIsNull(pageable);

                if (newsPage.isEmpty()) {
                    log.info("분석할 뉴스가 더 이상 없습니다. 페이지 {}에서 종료합니다", pageNumber);
                    hasMoreData = false;
                    break;
                }

                log.info("페이지 {} 처리 중 - 뉴스 {}건", pageNumber, newsPage.getContent().size());

                List<CompletableFuture<Boolean>> analysisFutures = newsPage.getContent().stream()
                        .map(this::processNewsAnalysis)
                        .toList();

                CompletableFuture.allOf(analysisFutures.toArray(new CompletableFuture[0])).join();

                long successfulCount = analysisFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Boolean::booleanValue)
                        .count();

                log.info("페이지 {} 처리 완료 - 분석 성공: {}/{}",
                        pageNumber, successfulCount, newsPage.getContent().size());

                pageNumber++;

                if (pageNumber > 100) {
                    log.warn("최대 페이지 제한(100)에 도달하여 처리를 중단합니다.");
                    break;
                }
            }

            log.info("AI 분석 태스크릿 완료 - 총 처리 건수: {}", processedNewsCount.get());

        } catch (Exception e) {
            log.error("AI 분석 태스크릿 실행 중 오류 발생", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    private CompletableFuture<Boolean> processNewsAnalysis(NewsJpaEntity newsEntity) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                log.debug("뉴스 AI 분석 시작: {}", newsEntity.getId());
                processedNewsCount.incrementAndGet();

                AiModel selectedModel = selectOptimalModel(newsEntity);
                log.debug("뉴스 {}에 선택된 AI 모델: {}", newsEntity.getId(), selectedModel);

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

                    log.debug("뉴스 {} AI 분석 완료 - 처리시간: {}ms, 모델: {}",
                            newsEntity.getId(), processingTime, selectedModel);
                    return true;

                } else {
                    log.warn("뉴스 {} AI 분석 결과가 비어 있습니다", newsEntity.getId());
                    failedAnalysisCount.incrementAndGet();

                    try {
                        log.info("뉴스 {}에 대해 폴백 분석을 시도합니다", newsEntity.getId());
                        List<News> fallbackNews = newsAiAnalysisRequesterPort.analyseNewses(AiModel.GEMMA, aiChatRequest);

                        if (fallbackNews != null && !fallbackNews.isEmpty()) {
                            updateNewsEntityWithAnalysis(newsEntity, fallbackNews.get(0));
                            newsJpaRepository.save(newsEntity);
                            successfulAnalysisCount.incrementAndGet();
                            log.info("뉴스 {} 폴백 분석 성공", newsEntity.getId());
                            return true;
                        }
                    } catch (Exception fallbackEx) {
                        log.warn("뉴스 {} 폴백 분석도 실패했습니다", newsEntity.getId(), fallbackEx);
                        throw new AiAnalysisFailedException(AiModel.GEMMA.name(), "폴백 분석 실패: " + fallbackEx.getMessage());
                    }

                    throw new AiAnalysisFailedException(selectedModel.name(), "분석 결과가 반환되지 않았습니다");
                }

            } catch (Exception e) {
                log.error("뉴스 {} AI 분석 중 오류 발생", newsEntity.getId(), e);
                failedAnalysisCount.incrementAndGet();

                if (e instanceof AiAnalysisFailedException) {
                    throw e;
                }

                try {
                    newsEntity.setOverview("AI 분석 실패로 인한 기본 요약");
                    newsEntity.setSentimentType(SentimentType.NEUTRAL);
                    newsEntity.setSentimentScore(0.5);
                    newsJpaRepository.save(newsEntity);
                    log.info("뉴스 {} 기본 폴백 데이터 저장 완료", newsEntity.getId());
                } catch (Exception saveEx) {
                    log.error("뉴스 {} 폴백 데이터 저장 실패", newsEntity.getId(), saveEx);
                    throw new DatabaseConnectionException("H2", "폴백 데이터 저장 실패: " + saveEx.getMessage());
                }

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

            log.debug("뉴스 엔티티 {}에 AI 분석 결과 반영 완료", newsEntity.getId());

        } catch (Exception e) {
            log.error("뉴스 엔티티 {} AI 분석 결과 반영 중 오류 발생", newsEntity.getId(), e);
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
                                ConcurrentHashMap::putAll));
    }

    public void resetMetrics() {
        processedNewsCount.set(0);
        successfulAnalysisCount.set(0);
        failedAnalysisCount.set(0);
        totalProcessingTime.set(0);
        modelUsageCount.clear();
        log.info("AI 분석 지표 초기화 완료");
    }

    public record AiAnalysisMetrics(
            int processedNewsCount,
            int successfulAnalysisCount,
            int failedAnalysisCount,
            long totalProcessingTime,
            ConcurrentHashMap<AiModel, Integer> modelUsageCount) {
        public double getSuccessRate() {
            if (processedNewsCount == 0)
                return 0.0;
            return (double) successfulAnalysisCount / processedNewsCount * 100;
        }

        public double getAverageProcessingTime() {
            if (successfulAnalysisCount == 0)
                return 0.0;
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