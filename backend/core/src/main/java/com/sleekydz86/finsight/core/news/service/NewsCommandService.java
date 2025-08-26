package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class NewsCommandService implements NewsCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(NewsCommandService.class);

    private final NewsScrapService newsScrapService;
    private final NewsAiProcessingService newsAiProcessingService;
    private final NewsPersistenceService newsPersistenceService;
    private final NewsNotificationService newsNotificationService;
    private final Executor newsProcessingExecutor;

    public NewsCommandService(NewsScrapService newsScrapService,
                              NewsAiProcessingService newsAiProcessingService,
                              NewsPersistenceService newsPersistenceService,
                              NewsNotificationService newsNotificationService,
                              @Qualifier("newsProcessingExecutor") Executor newsProcessingExecutor) {
        this.newsScrapService = newsScrapService;
        this.newsAiProcessingService = newsAiProcessingService;
        this.newsPersistenceService = newsPersistenceService;
        this.newsNotificationService = newsNotificationService;
        this.newsProcessingExecutor = newsProcessingExecutor;
    }

    @Override
    @Async("newsProcessingExecutor")
    @Timed("news.scrap.duration")
    @Counted("news.scrap.count")
    @CircuitBreaker(name = "newsScrapCircuitBreaker")
    @Retry(name = "newsScrapRetry")
    @TimeLimiter(name = "newsScrapTimeLimiter")
    @CacheEvict(value = "newsCache", allEntries = true)
    public CompletableFuture<Newses> scrapNewses() {
        try {
            log.info("뉴스 스크래핑 작업 시작");

            List<News> scrapedNews = newsScrapService.scrapNewsFromProviders();

            if (scrapedNews.isEmpty()) {
                log.warn("스크래핑된 뉴스가 없습니다");
                return CompletableFuture.completedFuture(new Newses(List.of()));
            }

            List<News> analyzedNews = newsAiProcessingService.processNewsWithAI(scrapedNews);

            Newses savedNewses = newsPersistenceService.saveNewsToDatabase(analyzedNews);

            newsNotificationService.sendNotificationsForImportantNews(savedNewses.getNewses());

            log.info("뉴스 스크래핑 작업 완료: {} 건", savedNewses.getNewses().size());
            return CompletableFuture.completedFuture(savedNewses);

        } catch (Exception e) {
            log.error("뉴스 스크래핑 작업 중 오류 발생", e);
            throw new RuntimeException("뉴스 스크래핑 실패", e);
        }
    }

    public Newses scrapNewsesFallback(Exception e) {
        log.error("Circuit Breaker 폴백 실행: {}", e.getMessage());
        return new Newses(List.of());
    }
}