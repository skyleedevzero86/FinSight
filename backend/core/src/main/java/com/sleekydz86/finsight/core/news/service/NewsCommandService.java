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
import io.micrometer.core.annotation.Timed;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsCommandService implements NewsCommandUseCase {

    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsPersistencePort newsPersistencePort;

    public NewsCommandService(NewsScrapRequesterPort newsScrapRequesterPort,
                              NewsPersistencePort newsPersistencePort) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    @Async("newsProcessingExecutor")
    @Timed("news.scrap.duration")
    @CircuitBreaker(name = "newsScrapCircuitBreaker")
    @Retry(name = "newsScrapRetry")
    @TimeLimiter(name = "newsScrapTimeLimiter")
    @CacheEvict(value = "newsCache", allEntries = true)
    public CompletableFuture<Newses> scrapNewses() {
        List<NewsProvider> providers = List.of(NewsProvider.MARKETAUX);

        List<CompletableFuture<List<News>>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() ->
                        newsScrapRequesterPort.scrap(provider)))
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
}