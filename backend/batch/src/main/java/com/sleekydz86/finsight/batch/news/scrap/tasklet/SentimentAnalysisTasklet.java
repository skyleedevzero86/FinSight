package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.port.out.SentimentAnalysisPort;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SentimentAnalysisTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisTasklet.class);

    private final NewsJpaRepository newsJpaRepository;
    private final SentimentAnalysisPort sentimentAnalysisPort;

    private final AtomicInteger processedNewsCount = new AtomicInteger(0);
    private final AtomicInteger successfulAnalysisCount = new AtomicInteger(0);
    private final AtomicInteger failedAnalysisCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final ConcurrentHashMap<SentimentType, AtomicInteger> sentimentDistribution = new ConcurrentHashMap<>();

    public SentimentAnalysisTasklet(NewsJpaRepository newsJpaRepository,
                                    SentimentAnalysisPort sentimentAnalysisPort) {
        this.newsJpaRepository = newsJpaRepository;
        this.sentimentAnalysisPort = sentimentAnalysisPort;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("감정 분석 태스크릿 시작");

        if (!sentimentAnalysisPort.isModelAvailable()) {
            log.warn("감정 분석 모델을 사용할 수 없습니다. 태스크릿을 건너뜁니다.");
            return RepeatStatus.FINISHED;
        }

        List<com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity> newsEntities =
                newsJpaRepository.findByOverviewIsNull();

        if (newsEntities.isEmpty()) {
            log.info("분석할 뉴스가 없습니다.");
            return RepeatStatus.FINISHED;
        }

        log.info("{}개의 뉴스에 대해 감정 분석을 시작합니다.", newsEntities.size());

        for (com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity newsEntity : newsEntities) {
            try {
                processNewsEntity(newsEntity);
            } catch (Exception e) {
                log.error("뉴스 ID {} 처리 중 오류 발생: {}", newsEntity.getId(), e.getMessage(), e);
                failedAnalysisCount.incrementAndGet();
            }
        }

        log.info("감정 분석 완료 - 처리됨: {}, 성공: {}, 실패: {}",
                processedNewsCount.get(), successfulAnalysisCount.get(), failedAnalysisCount.get());

        return RepeatStatus.FINISHED;
    }

    private void processNewsEntity(com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity newsEntity) {
        long startTime = System.currentTimeMillis();

        try {
            String content = newsEntity.getOriginalTitle() + ". " + newsEntity.getOriginalContent();
            SentimentAnalysisResult sentimentResult = sentimentAnalysisPort.analyzeSentiment(content);

            if (sentimentResult.isSuccess()) {
                updateNewsEntityWithAnalysis(newsEntity, sentimentResult);
                successfulAnalysisCount.incrementAndGet();

                SentimentType sentimentType = sentimentResult.getSentimentType();
                sentimentDistribution.computeIfAbsent(sentimentType, k -> new AtomicInteger(0))
                        .incrementAndGet();

                long processingTime = System.currentTimeMillis() - startTime;
                totalProcessingTime.addAndGet(processingTime);

                log.debug("뉴스 ID {} 감정 분석 완료: {} (점수: {})",
                        newsEntity.getId(), sentimentType, sentimentResult.getScore());
            } else {
                log.warn("뉴스 ID {} 감정 분석 실패: {}", newsEntity.getId(), sentimentResult.getErrorMessage());
                failedAnalysisCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("뉴스 ID {} 감정 분석 중 예외 발생: {}", newsEntity.getId(), e.getMessage(), e);
            failedAnalysisCount.incrementAndGet();
        } finally {
            processedNewsCount.incrementAndGet();
        }
    }

    private void updateNewsEntityWithAnalysis(com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity newsEntity,
                                              SentimentAnalysisResult sentimentResult) {
        newsEntity.setSentimentType(sentimentResult.getSentimentType());
        newsEntity.setSentimentScore(sentimentResult.getScore());
        newsEntity.setOverview(sentimentResult.getSentimentDescription());

        newsJpaRepository.save(newsEntity);
    }

    public SentimentAnalysisMetrics getAnalysisMetrics() {
        return new SentimentAnalysisMetrics(
                processedNewsCount.get(),
                successfulAnalysisCount.get(),
                failedAnalysisCount.get(),
                totalProcessingTime.get(),
                new ConcurrentHashMap<>(sentimentDistribution.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().get()
                        )))
        );
    }

    public void resetMetrics() {
        processedNewsCount.set(0);
        successfulAnalysisCount.set(0);
        failedAnalysisCount.set(0);
        totalProcessingTime.set(0);
        sentimentDistribution.values().forEach(atomicInteger -> atomicInteger.set(0));
    }

    public record SentimentAnalysisMetrics(
            int processedNewsCount,
            int successfulAnalysisCount,
            int failedAnalysisCount,
            long totalProcessingTime,
            ConcurrentHashMap<SentimentType, Integer> sentimentDistribution
    ) {
        public double getSuccessRate() {
            if (processedNewsCount == 0) return 0.0;
            return (double) successfulAnalysisCount / processedNewsCount * 100;
        }

        public double getAverageProcessingTime() {
            if (successfulAnalysisCount == 0) return 0.0;
            return (double) totalProcessingTime / successfulAnalysisCount;
        }

        public SentimentType getMostCommonSentiment() {
            return sentimentDistribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(SentimentType.NEUTRAL);
        }
    }
}