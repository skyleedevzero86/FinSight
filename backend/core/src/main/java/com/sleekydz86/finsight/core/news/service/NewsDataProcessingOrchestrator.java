package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsDataProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NewsDataProcessingOrchestrator.class);

    private final NewsDataNormalizationService normalizationService;
    private final NewsDeduplicationService deduplicationService;
    private final NewsDataQualityService qualityService;
    private final NewsPersistenceService persistenceService;
    private final Executor newsProcessingExecutor;

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong successfulProcessed = new AtomicLong(0);
    private final AtomicLong failedProcessed = new AtomicLong(0);

    public NewsDataProcessingOrchestrator(NewsDataNormalizationService normalizationService,
                                          NewsDeduplicationService deduplicationService,
                                          NewsDataQualityService qualityService,
                                          NewsPersistenceService persistenceService,
                                          Executor newsProcessingExecutor) {
        this.normalizationService = normalizationService;
        this.deduplicationService = deduplicationService;
        this.qualityService = qualityService;
        this.persistenceService = persistenceService;
        this.newsProcessingExecutor = newsProcessingExecutor;
    }

    public CompletableFuture<Newses> processNewsData(Newses rawNewses) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("뉴스 데이터 처리 시작: {} 개", rawNewses.getNewses().size());

                List<News> processedNews = new ArrayList<>();

                for (News news : rawNewses.getNewses()) {
                    try {
                        List<News> normalizedNewsList = normalizationService.normalizeAndDeduplicate(List.of(news));
                        if (!normalizedNewsList.isEmpty()) {
                            News normalizedNews = normalizedNewsList.get(0);
                            News deduplicatedNews = deduplicationService.deduplicateNews(normalizedNews);
                            if (deduplicatedNews != null) {
                                NewsDataQualityService.DataQualityResult qualityResult = qualityService.validateNewsQuality(deduplicatedNews);
                                if (qualityResult.isValid()) {
                                    processedNews.add(deduplicatedNews);
                                    successfulProcessed.incrementAndGet();
                                } else {
                                    log.warn("뉴스 품질 검증 실패: {}", qualityResult.getErrors());
                                    failedProcessed.incrementAndGet();
                                }
                            } else {
                                log.debug("중복 뉴스로 제거됨");
                                failedProcessed.incrementAndGet();
                            }
                        } else {
                            log.warn("정규화된 뉴스가 없음");
                            failedProcessed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("뉴스 처리 중 오류 발생: {}", e.getMessage());
                        failedProcessed.incrementAndGet();
                    }
                    totalProcessed.incrementAndGet();
                }

                Newses processedNewses = new Newses(processedNews);
                if (!processedNews.isEmpty()) {
                    persistenceService.saveNewsToDatabase(processedNews);
                }

                log.info("뉴스 데이터 처리 완료: 성공 {} 개, 실패 {} 개",
                        successfulProcessed.get(), failedProcessed.get());

                return processedNewses;
            } catch (Exception e) {
                log.error("뉴스 데이터 처리 중 전체 오류 발생", e);
                throw new RuntimeException("뉴스 데이터 처리 실패", e);
            }
        }, newsProcessingExecutor);
    }

    public Map<String, Object> getProcessingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("successfulProcessed", successfulProcessed.get());
        stats.put("failedProcessed", failedProcessed.get());
        stats.put("successRate", totalProcessed.get() > 0 ?
                (double) successfulProcessed.get() / totalProcessed.get() * 100 : 0);
        return stats;
    }

    public void resetStatistics() {
        totalProcessed.set(0);
        successfulProcessed.set(0);
        failedProcessed.set(0);
    }
}