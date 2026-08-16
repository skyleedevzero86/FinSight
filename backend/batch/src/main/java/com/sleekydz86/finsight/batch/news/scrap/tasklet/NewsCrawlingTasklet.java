package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class NewsCrawlingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlingTasklet.class);

    private final Map<NewsProvider, NewsScrapRequester> newsScrapRequesters;
    private final NewsPersistencePort newsPersistencePort;

    private final ConcurrentHashMap<NewsProvider, AtomicInteger> scrapedNewsCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NewsProvider, AtomicInteger> errorCount = new ConcurrentHashMap<>();
    private final AtomicInteger totalScrapedCount = new AtomicInteger(0);

    public NewsCrawlingTasklet(
            List<NewsScrapRequester> newsScrapRequesters,
            NewsPersistencePort newsPersistencePort) {
        this.newsScrapRequesters = newsScrapRequesters.stream()
                .collect(Collectors.toMap(
                        NewsScrapRequester::supports,
                        requester -> requester
                ));
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("뉴스 크롤링 태스크릿 시작");

        LocalDateTime publishTimeAfter = LocalDateTime.now().minusHours(24);
        int limit = 100;

        try {
            List<CompletableFuture<List<News>>> scrapingFutures = newsScrapRequesters.entrySet().stream()
                    .map(entry -> {
                        NewsProvider provider = entry.getKey();
                        NewsScrapRequester requester = entry.getValue();

                        return executeScrapingForProvider(provider, requester, publishTimeAfter, limit)
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        log.error("뉴스 제공자 스크래핑 중 오류 발생: {}", provider, throwable);
                                        errorCount.computeIfAbsent(provider, k -> new AtomicInteger(0)).incrementAndGet();
                                    } else {
                                        log.info("제공자 {}에서 뉴스 {}건 스크래핑 완료",
                                                result.size(), provider);
                                        scrapedNewsCount.computeIfAbsent(provider, k -> new AtomicInteger(0))
                                                .addAndGet(result.size());
                                        totalScrapedCount.addAndGet(result.size());
                                    }
                                });
                    })
                    .toList();

            CompletableFuture.allOf(scrapingFutures.toArray(new CompletableFuture[0])).join();

            List<News> allScrapedNews = scrapingFutures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            if (!allScrapedNews.isEmpty()) {
                log.info("스크래핑한 뉴스 {}건 저장 시작", allScrapedNews.size());
                newsPersistencePort.saveAllNews(allScrapedNews);
                log.info("스크래핑한 뉴스 저장 완료");
            } else {
                log.warn("스크래핑된 뉴스가 없습니다");
            }

            contribution.incrementWriteCount(allScrapedNews.size());
            log.info("뉴스 크롤링 태스크릿 완료 - 총 스크래핑 건수: {}", totalScrapedCount.get());

        } catch (Exception e) {
            log.error("뉴스 크롤링 태스크릿 실행 중 오류 발생", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    private CompletableFuture<List<News>> executeScrapingForProvider(
            NewsProvider provider,
            NewsScrapRequester requester,
            LocalDateTime publishTimeAfter,
            int limit) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("제공자 {} 스크래핑 시작 - 제한 건수: {}", provider, limit);

                List<News> scrapedNews = requester.scrap(publishTimeAfter, limit).get();

                log.debug("제공자 {} 스크래핑 완료 - 조회 건수: {}",
                        provider, scrapedNews.size());

                return scrapedNews;

            } catch (Exception e) {
                log.error("제공자 뉴스 스크래핑 실패: {}", provider, e);
                throw new RuntimeException("제공자 스크래핑 실패: " + provider, e);
            }
        });
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