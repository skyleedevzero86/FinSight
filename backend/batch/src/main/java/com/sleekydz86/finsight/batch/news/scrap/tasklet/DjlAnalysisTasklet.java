package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.port.out.DjlSentimentAnalysisPort;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DjlAnalysisTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(DjlAnalysisTasklet.class);

    private final NewsJpaRepository newsJpaRepository;
    private final DjlSentimentAnalysisPort djlSentimentAnalysisPort;

    private final AtomicInteger processedNewsCount = new AtomicInteger(0);
    private final AtomicInteger successfulAnalysisCount = new AtomicInteger(0);
    private final AtomicInteger failedAnalysisCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final ConcurrentHashMap<SentimentType, AtomicInteger> sentimentDistribution = new ConcurrentHashMap<>();

    public DjlAnalysisTasklet(NewsJpaRepository newsJpaRepository,
                              DjlSentimentAnalysisPort djlSentimentAnalysisPort) {
        this.newsJpaRepository = newsJpaRepository;
        this.djlSentimentAnalysisPort = djlSentimentAnalysisPort;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!djlSentimentAnalysisPort.isModelAvailable()) {
            log.warn("DJL 모델을 사용할 수 없습니다. 작업을 건너뜁니다.");
            return RepeatStatus.FINISHED;
        }

        log.info("DJL 감정분석 배치 작업 시작");

        int pageSize = 100;
        int pageNumber = 0;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        while (true) {
            Page<NewsJpaEntity> newsPage = newsJpaRepository.findByOverviewIsNull(pageable);

            if (!newsPage.hasContent()) {
                break;
            }

            for (NewsJpaEntity newsEntity : newsPage.getContent()) {
                try {
                    processNewsEntity(newsEntity);
                } catch (Exception e) {
                    log.error("뉴스 엔티티 처리 실패 (ID: {}): {}", newsEntity.getId(), e.getMessage());
                    failedAnalysisCount.incrementAndGet();
                }
            }

            if (!newsPage.hasNext()) {
                break;
            }

            pageable = pageable.next();
        }

        log.info("DJL 감정분석 배치 작업 완료 - 처리: {}, 성공: {}, 실패: {}",
                processedNewsCount.get(), successfulAnalysisCount.get(), failedAnalysisCount.get());

        return RepeatStatus.FINISHED;
    }

    private void processNewsEntity(NewsJpaEntity newsEntity) {
        long startTime = System.currentTimeMillis();
        processedNewsCount.incrementAndGet();

        try {
            String content = newsEntity.getOriginalTitle() + ". " + newsEntity.getOriginalContent();
            DjlSentimentResult sentimentResult = djlSentimentAnalysisPort.analyzeSentiment(content);

            if (sentimentResult.isSuccess()) {
                updateNewsEntityWithAnalysis(newsEntity, sentimentResult);

                sentimentDistribution.computeIfAbsent(sentimentResult.toSentimentType(),
                        k -> new AtomicInteger(0)).incrementAndGet();

                successfulAnalysisCount.incrementAndGet();
                totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);

                log.debug("뉴스 ID {} 감정분석 완료: {} (점수: {})",
                        newsEntity.getId(), sentimentResult.getLabel(), sentimentResult.getScore());
            } else {
                failedAnalysisCount.incrementAndGet();
                log.warn("뉴스 ID {} 감정분석 실패: {}",
                        newsEntity.getId(), sentimentResult.getErrorMessage());
            }

        } catch (Exception e) {
            failedAnalysisCount.incrementAndGet();
            log.error("뉴스 ID {} 처리 중 오류: {}", newsEntity.getId(), e.getMessage());
        }
    }

    private void updateNewsEntityWithAnalysis(NewsJpaEntity newsEntity, DjlSentimentResult sentimentResult) {
        newsEntity.setSentimentType(sentimentResult.toSentimentType());
        newsEntity.setSentimentScore(sentimentResult.getScore());
        String overview = String.format("감정분석 결과: %s (신뢰도: %.2f%%)",
                sentimentResult.getLabel(), sentimentResult.getConfidence() * 100);
        newsEntity.setOverview(overview);
        newsJpaRepository.save(newsEntity);
    }

    public DjlAnalysisMetrics getAnalysisMetrics() {
        ConcurrentHashMap<SentimentType, Integer> convertedDistribution = new ConcurrentHashMap<>();
        sentimentDistribution.forEach((key, value) ->
                convertedDistribution.put(key, value.get()));

        return new DjlAnalysisMetrics(
                processedNewsCount.get(),
                successfulAnalysisCount.get(),
                failedAnalysisCount.get(),
                totalProcessingTime.get(),
                convertedDistribution
        );
    }

    public void resetMetrics() {
        processedNewsCount.set(0);
        successfulAnalysisCount.set(0);
        failedAnalysisCount.set(0);
        totalProcessingTime.set(0);
        sentimentDistribution.clear();
    }

    public record DjlAnalysisMetrics(
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