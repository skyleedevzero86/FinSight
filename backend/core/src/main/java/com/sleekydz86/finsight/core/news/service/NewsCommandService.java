package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsCommandService implements NewsCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(NewsCommandService.class);

    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsPersistencePort newsPersistencePort;
    private final NotificationService notificationService;
    private final Executor newsProcessingExecutor;

    public NewsCommandService(NewsScrapRequesterPort newsScrapRequesterPort,
            NewsPersistencePort newsPersistencePort,
            NotificationService notificationService,
            @Qualifier("newsProcessingExecutor") Executor newsProcessingExecutor) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsPersistencePort = newsPersistencePort;
        this.notificationService = notificationService;
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("뉴스 스크래핑 시작");
                long startTime = System.currentTimeMillis();

                List<CompletableFuture<List<News>>> scrapFutures = NewsProvider.getAllProviders().stream()
                        .filter(provider -> provider != NewsProvider.ALL)
                        .map(provider -> CompletableFuture.supplyAsync(() -> {
                            try {
                                log.info("{}에서 뉴스 스크래핑 시작", provider);
                                List<News> newses = newsScrapRequesterPort.scrap(provider);
                                log.info("{}에서 {}개 뉴스 스크래핑 완료", provider, newses.size());
                                return newses;
                            } catch (Exception e) {
                                log.error("{}에서 뉴스 스크래핑 오류: {}", provider, e.getMessage());
                                return new ArrayList<News>();
                            }
                        }, newsProcessingExecutor))
                        .collect(Collectors.toList());

                CompletableFuture<Void> allScraped = CompletableFuture.allOf(
                        scrapFutures.toArray(new CompletableFuture[0]));

                List<News> allNewses = allScraped.thenApply(v -> scrapFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList())).join();

                log.info("총 {}개 뉴스 스크래핑 완료", allNewses.size());

                CompletableFuture<Newses> aiAnalysisFuture = processNewsWithAI(allNewses);

                Newses savedNewses = aiAnalysisFuture.thenApply(analyzedNewses -> {
                    try {
                        Newses saved = newsPersistencePort.saveAllNews(analyzedNewses.getNewses());
                        log.info("{}개 뉴스 저장 완료", saved.getNewses().size());

                        sendNotificationsForImportantNews(saved.getNewses());

                        return saved;
                    } catch (Exception e) {
                        log.error("뉴스 저장 중 오류: {}", e.getMessage());
                        throw new RuntimeException("뉴스 저장 실패", e);
                    }
                }).join();

                long duration = System.currentTimeMillis() - startTime;
                log.info("뉴스 스크래핑 전체 완료: {}ms", duration);

                return savedNewses;

            } catch (Exception e) {
                log.error("뉴스 스크래핑 중 치명적 오류: {}", e.getMessage(), e);
                throw new RuntimeException("뉴스 스크래핑 실패", e);
            }
        }, newsProcessingExecutor);
    }

    private CompletableFuture<Newses> processNewsWithAI(List<News> newses) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("AI 분석 시작: {}개 뉴스", newses.size());

                int chunkSize = 10;
                List<CompletableFuture<List<News>>> analysisFutures = new ArrayList<>();

                for (int i = 0; i < newses.size(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, newses.size());
                    List<News> chunk = newses.subList(i, end);

                    CompletableFuture<List<News>> chunkFuture = CompletableFuture.supplyAsync(() -> {
                        return chunk.stream()
                                .map(this::analyzeNewsWithAI)
                                .collect(Collectors.toList());
                    }, newsProcessingExecutor);

                    analysisFutures.add(chunkFuture);
                }

                CompletableFuture<Void> allAnalyzed = CompletableFuture.allOf(
                        analysisFutures.toArray(new CompletableFuture[0]));

                List<News> analyzedNewses = allAnalyzed.thenApply(v -> analysisFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList())).join();

                log.info("AI 분석 완료: {}개 뉴스", analyzedNewses.size());
                return new Newses(analyzedNewses);

            } catch (Exception e) {
                log.error("AI 분석 중 오류: {}", e.getMessage());
                throw new RuntimeException("AI 분석 실패", e);
            }
        }, newsProcessingExecutor);
    }

    private News analyzeNewsWithAI(News news) {
        try {
            log.debug("뉴스 AI 분석 중: ID={}", news.getId());
            // 실제 AI 분석 로직 구현
            return news;
        } catch (Exception e) {
            log.error("뉴스 AI 분석 오류: ID={}, 오류={}", news.getId(), e.getMessage());
            return news;
        }
    }

    private void sendNotificationsForImportantNews(List<News> newses) {
        try {
            List<News> importantNewses = newses.stream()
                    .filter(news -> news.getAiOverView() != null &&
                            news.getAiOverView().getSentimentScore() > 0.7)
                    .collect(Collectors.toList());

            if (!importantNewses.isEmpty()) {
                log.info("중요 뉴스 {}개에 대한 알림 발송 시작", importantNewses.size());

                CompletableFuture.runAsync(() -> {
                    importantNewses.forEach(news -> {
                        try {
                            notificationService.notifyUsersAboutNews(news);
                        } catch (Exception e) {
                            log.error("뉴스 알림 발송 오류: 뉴스 ID={}, 오류={}", news.getId(), e.getMessage());
                        }
                    });
                }, newsProcessingExecutor);
            }
        } catch (Exception e) {
            log.error("알림 발송 중 오류: {}", e.getMessage());
        }
    }

    public Newses scrapNewsesFallback(Exception e) {
        log.error("뉴스 스크래핑 폴백 실행: {}", e.getMessage());
        return new Newses(new ArrayList<>());
    }
}