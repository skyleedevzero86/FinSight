package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlFailureSink;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlPermit;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRunGate;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.service.NewsAiProcessingService;
import com.sleekydz86.finsight.core.news.service.NewsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class NewsCrawlingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlingTasklet.class);

    private final Map<NewsProvider, NewsScrapRequester> newsScrapRequesters;
    private final NewsPersistencePort newsPersistencePort;
    private final NewsAiProcessingService newsAiProcessingService;
    private final NewsNotificationService newsNotificationService;
    private final NewsCrawlRunGate crawlRunGate;
    private final NewsCrawlFailureSink failureSink;

    private final ConcurrentHashMap<NewsProvider, AtomicInteger> scrapedNewsCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NewsProvider, AtomicInteger> errorCount = new ConcurrentHashMap<>();
    private final AtomicInteger totalScrapedCount = new AtomicInteger(0);
    private final AtomicInteger lockSkips = new AtomicInteger(0);
    private final AtomicInteger ranProviders = new AtomicInteger(0);

    public NewsCrawlingTasklet(
            List<NewsScrapRequester> newsScrapRequesters,
            NewsPersistencePort newsPersistencePort,
            NewsAiProcessingService newsAiProcessingService,
            NewsNotificationService newsNotificationService,
            NewsCrawlRunGate crawlRunGate,
            NewsCrawlFailureSink failureSink) {
        this.newsScrapRequesters = newsScrapRequesters.stream()
                .collect(Collectors.toMap(
                        NewsScrapRequester::supports,
                        requester -> requester
                ));
        this.newsPersistencePort = newsPersistencePort;
        this.newsAiProcessingService = newsAiProcessingService;
        this.newsNotificationService = newsNotificationService;
        this.crawlRunGate = crawlRunGate;
        this.failureSink = failureSink;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("뉴스 크롤링 태스크릿 시작");
        resetMetrics();
        String owner = "batch-" + UUID.randomUUID();
        List<NewsCrawlPermit> held = new CopyOnWriteArrayList<>();
        LocalDateTime publishTimeAfter = LocalDateTime.now().minusHours(24);
        int limit = 100;

        try {
            List<CompletableFuture<List<News>>> scrapingFutures = newsScrapRequesters.entrySet().stream()
                    .map(entry -> {
                        NewsProvider provider = entry.getKey();
                        NewsScrapRequester requester = entry.getValue();

                        return executeScrapingForProvider(provider, requester, publishTimeAfter, limit, owner, held)
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        return;
                                    }
                                    log.info("제공자 {}에서 뉴스 {}건 스크래핑 완료", provider, result.size());
                                    scrapedNewsCount.computeIfAbsent(provider, k -> new AtomicInteger(0))
                                            .addAndGet(result.size());
                                    totalScrapedCount.addAndGet(result.size());
                                })
                                .exceptionally(throwable -> {
                                    log.error("뉴스 제공자 스크래핑 중 오류 발생: {}", provider, throwable);
                                    errorCount.computeIfAbsent(provider, k -> new AtomicInteger(0)).incrementAndGet();
                                    recordFailure(provider, throwable);
                                    return List.of();
                                });
                    })
                    .toList();

            CompletableFuture.allOf(scrapingFutures.toArray(new CompletableFuture[0])).join();

            List<News> allScrapedNews = scrapingFutures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            if (everyProviderFailed(allScrapedNews)) {
                log.error("모든 뉴스 제공처 수집에 실패했습니다");
                throw new IllegalStateException("모든 뉴스 제공처 수집에 실패했습니다");
            }

            List<News> fresh = newsPersistencePort.omitStoredDuplicates(allScrapedNews);
            if (!fresh.isEmpty()) {
                log.info("스크래핑한 뉴스 {}건 중 신규 {}건 저장 시작", allScrapedNews.size(), fresh.size());
                List<News> analyzed = newsAiProcessingService.processNewsWithAI(fresh);
                Newses saved = newsPersistencePort.saveAllNews(analyzed);
                newsNotificationService.sendNotificationsForImportantNews(saved.getNewses());
                log.info("스크래핑한 뉴스 저장 완료");
            } else if (allScrapedNews.isEmpty()) {
                log.warn("스크래핑된 뉴스가 없습니다");
            } else {
                log.info("이미 저장된 뉴스라 신규 저장을 건너뜁니다");
            }

            contribution.incrementWriteCount(allScrapedNews.size());
            log.info("뉴스 크롤링 태스크릿 완료 - 총 스크래핑 건수: {}", totalScrapedCount.get());

        } catch (Exception e) {
            log.error("뉴스 크롤링 태스크릿 실행 중 오류 발생", e);
            throw e;
        } finally {
            held.forEach(crawlRunGate::release);
            writeRunCounts(chunkContext);
        }

        return RepeatStatus.FINISHED;
    }

    private boolean everyProviderFailed(List<News> scraped) {
        if (!scraped.isEmpty() || newsScrapRequesters.isEmpty()) {
            return false;
        }
        long failedProviders = errorCount.values().stream().filter(count -> count.get() > 0).count();
        return failedProviders >= newsScrapRequesters.size();
    }

    private CompletableFuture<List<News>> executeScrapingForProvider(
            NewsProvider provider,
            NewsScrapRequester requester,
            LocalDateTime publishTimeAfter,
            int limit,
            String owner,
            List<NewsCrawlPermit> held) {

        return CompletableFuture.supplyAsync(() -> {
            Optional<NewsCrawlPermit> permit = crawlRunGate.tryAcquire(provider, owner, Duration.ofMinutes(2));
            if (permit.isEmpty()) {
                lockSkips.incrementAndGet();
                log.warn("제공처 {} 수집 잠금이 있어 이번 수집을 건너뜁니다", provider);
                return List.of();
            }
            ranProviders.incrementAndGet();
            try {
                log.debug("제공자 {} 스크래핑 시작 - 제한 건수: {}", provider, limit);
                List<News> scrapedNews = requester.scrap(publishTimeAfter, limit).get();
                log.debug("제공자 {} 스크래핑 완료 - 조회 건수: {}", provider, scrapedNews.size());
                held.add(permit.get());
                return scrapedNews;
            } catch (Exception e) {
                crawlRunGate.release(permit.get());
                log.error("제공자 뉴스 스크래핑 실패: {}", provider, e);
                throw new RuntimeException("제공자 스크래핑 실패: " + provider, e);
            }
        });
    }

    private void recordFailure(NewsProvider provider, Throwable error) {
        try {
            failureSink.record(provider, error);
        } catch (RuntimeException failure) {
            log.error("뉴스 수집 장애 기록에 실패했습니다 provider={}", provider, failure);
        }
    }

    private void writeRunCounts(ChunkContext chunkContext) {
        var context = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        context.putInt("newsCrawlRan", ranProviders.get());
        context.putInt("newsCrawlLockSkips", lockSkips.get());
    }

    public CrawlingMetrics getCrawlingMetrics() {
        return new CrawlingMetrics(
                scrapedNewsCount.entrySet().stream()
                        .collect(ConcurrentHashMap::new,
                                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                                ConcurrentHashMap::putAll),
                errorCount.entrySet().stream()
                        .collect(ConcurrentHashMap::new,
                                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                                ConcurrentHashMap::putAll),
                totalScrapedCount.get()
        );
    }

    public void resetMetrics() {
        scrapedNewsCount.clear();
        errorCount.clear();
        totalScrapedCount.set(0);
        lockSkips.set(0);
        ranProviders.set(0);
        log.info("크롤링 지표 초기화 완료");
    }

    public record CrawlingMetrics(
            ConcurrentHashMap<NewsProvider, Integer> scrapedNewsCount,
            ConcurrentHashMap<NewsProvider, Integer> errorCount,
            int totalScrapedCount
    ) {
        public int getTotalErrors() {
            return errorCount.values().stream().mapToInt(Integer::intValue).sum();
        }

        public double getSuccessRate() {
            if (totalScrapedCount == 0) return 0.0;
            return (double) (totalScrapedCount - getTotalErrors()) / totalScrapedCount * 100;
        }
    }
}