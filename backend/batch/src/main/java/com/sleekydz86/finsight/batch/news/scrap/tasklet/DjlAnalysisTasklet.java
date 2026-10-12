package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.port.out.DjlSentimentAnalysisPort;
import com.sleekydz86.finsight.core.news.domain.port.out.SentimentAnalysisPort;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentStatus;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DjlAnalysisTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(DjlAnalysisTasklet.class);
    private static final int PAGE = 32;
    private static final int PENDING_CAP = 500;

    private final NewsJpaRepository newsJpaRepository;
    private final DjlSentimentAnalysisPort djlSentimentAnalysisPort;
    private final SentimentAnalysisPort sentimentAnalysisPort;

    private final AtomicInteger processedNewsCount = new AtomicInteger(0);
    private final AtomicInteger successfulAnalysisCount = new AtomicInteger(0);
    private final AtomicInteger failedAnalysisCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final ConcurrentHashMap<SentimentType, AtomicInteger> sentimentDistribution = new ConcurrentHashMap<>();

    public DjlAnalysisTasklet(NewsJpaRepository newsJpaRepository,
                              DjlSentimentAnalysisPort djlSentimentAnalysisPort,
                              SentimentAnalysisPort sentimentAnalysisPort) {
        this.newsJpaRepository = newsJpaRepository;
        this.djlSentimentAnalysisPort = djlSentimentAnalysisPort;
        this.sentimentAnalysisPort = sentimentAnalysisPort;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        if (!djlSentimentAnalysisPort.isModelAvailable()) {
            log.warn("DJL 모델을 사용할 수 없습니다. 키워드 감성으로 대체하고 재시도 대상으로 남깁니다.");
        }
        int pending = drainPending();
        int retried = analyzeIds(retryIds());
        log.info("DJL 감정분석 배치 완료 - 신규: {}, 재시도: {}, 성공: {}, 실패: {}",
                pending, retried, successfulAnalysisCount.get(), failedAnalysisCount.get());
        return RepeatStatus.FINISHED;
    }

    private int drainPending() {
        int seen = 0;
        while (seen < PENDING_CAP) {
            List<Long> ids = newsJpaRepository.findSentimentPendingIds(
                    SentimentStatus.PENDING, PageRequest.of(0, PAGE));
            if (ids.isEmpty()) {
                return seen;
            }
            analyzeIds(ids);
            seen += ids.size();
        }
        return seen;
    }

    private List<Long> retryIds() {
        return newsJpaRepository.findSentimentRetryIds(
                List.of(SentimentStatus.FAILED, SentimentStatus.KEYWORD),
                NewsSentimentRecorder.MAX_ATTEMPTS,
                PageRequest.of(0, PAGE));
    }

    private int analyzeIds(List<Long> ids) {
        List<NewsJpaEntity> chunk = new ArrayList<>();
        for (Long id : ids) {
            newsJpaRepository.findById(id).ifPresent(chunk::add);
            if (chunk.size() == PAGE) {
                analyzeChunk(chunk);
                chunk = new ArrayList<>();
            }
        }
        if (!chunk.isEmpty()) {
            analyzeChunk(chunk);
        }
        return ids.size();
    }

    private void analyzeChunk(List<NewsJpaEntity> news) {
        if (!djlSentimentAnalysisPort.isModelAvailable()) {
            news.forEach(item -> {
                processedNewsCount.incrementAndGet();
                applyKeywordOrFail(item);
            });
            newsJpaRepository.saveAll(news);
            return;
        }
        List<DjlSentimentResult> results = djlSentimentAnalysisPort.analyzeSentimentBatch(textsOf(news));
        for (int index = 0; index < news.size(); index++) {
            applyOne(news.get(index), resultAt(results, index));
        }
        newsJpaRepository.saveAll(news);
    }

    private void applyOne(NewsJpaEntity news, DjlSentimentResult result) {
        long started = System.currentTimeMillis();
        processedNewsCount.incrementAndGet();
        if (result != null && result.isSuccess()) {
            NewsSentimentRecorder.applyDjl(news, result);
            successfulAnalysisCount.incrementAndGet();
            totalProcessingTime.addAndGet(System.currentTimeMillis() - started);
            sentimentDistribution.computeIfAbsent(result.toSentimentType(), key -> new AtomicInteger()).incrementAndGet();
            return;
        }
        String reason = result == null ? "결과 없음" : result.getErrorMessage();
        log.warn("뉴스 감성분석 실패 newsId={} 사유={}", news.getId(), reason);
        applyKeywordOrFail(news);
    }

    private void applyKeywordOrFail(NewsJpaEntity news) {
        SentimentAnalysisResult keyword = sentimentAnalysisPort.analyzeSentiment(textOf(news));
        if (keyword.isSuccess()) {
            NewsSentimentRecorder.applyKeyword(news, keyword);
            log.info("뉴스 감성을 키워드로 대체했습니다. newsId={} 시도={}", news.getId(), news.getSentimentAttempts());
            return;
        }
        NewsSentimentRecorder.markFailed(news);
        failedAnalysisCount.incrementAndGet();
        log.warn("뉴스 감성 키워드 대체도 실패했습니다. newsId={} 사유={}", news.getId(), keyword.getErrorMessage());
    }

    private DjlSentimentResult resultAt(List<DjlSentimentResult> results, int index) {
        if (results == null || index >= results.size()) {
            return null;
        }
        return results.get(index);
    }

    private List<String> textsOf(List<NewsJpaEntity> news) {
        return news.stream().map(this::textOf).toList();
    }

    private String textOf(NewsJpaEntity news) {
        String title = news.getOriginalTitle() == null ? "" : news.getOriginalTitle();
        String body = news.getOriginalContent() == null ? "" : news.getOriginalContent();
        return title + ". " + body;
    }

    public DjlAnalysisMetrics getAnalysisMetrics() {
        ConcurrentHashMap<SentimentType, Integer> converted = new ConcurrentHashMap<>();
        sentimentDistribution.forEach((key, value) -> converted.put(key, value.get()));
        return new DjlAnalysisMetrics(
                processedNewsCount.get(),
                successfulAnalysisCount.get(),
                failedAnalysisCount.get(),
                totalProcessingTime.get(),
                converted);
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
            if (processedNewsCount == 0) {
                return 0.0;
            }
            return (double) successfulAnalysisCount / processedNewsCount * 100;
        }

        public double getAverageProcessingTime() {
            if (successfulAnalysisCount == 0) {
                return 0.0;
            }
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
