package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.global.NewsProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdvancedNewsCommandService implements NewsCommandUseCase {

    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsPersistencePort newsPersistencePort;

    public AdvancedNewsCommandService(NewsScrapRequesterPort newsScrapRequesterPort,
                                      NewsPersistencePort newsPersistencePort) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    @Async("newsProcessingExecutor")
    @Timed("news.scrap.duration")
    @Counted("news.scrap.count")
    @CircuitBreaker(name = "newsScrapCircuitBreaker", fallbackMethod = "scrapNewsesFallback")
    @Retry(name = "newsScrapRetry")
    @TimeLimiter(name = "newsScrapTimeLimiter")
    @Bulkhead(name = "newsScrapBulkhead")
    @RateLimiter(name = "newsScrapRateLimiter")
    @CacheEvict(value = "newsCache", allEntries = true)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Newses> scrapNewses() {
        List<NewsProvider> providers = List.of(NewsProvider.MARKETAUX);

        List<CompletableFuture<List<News>>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() ->
                                newsScrapRequesterPort.scrap(provider))
                        .exceptionally(throwable -> {
                            // 개별 실패 처리
                            return List.<News>of();
                        }))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            List<News> allNewses = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            return newsPersistencePort.saveAllNews(allNewses);
        });
    }

    public Newses scrapNewsesFallback(Exception e) {
        // Circuit Breaker가 열렸을 때의 폴백 로직
        return new Newses(List.of());
    }
}
