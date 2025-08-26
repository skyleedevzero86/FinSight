package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

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
    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;

    public NewsCommandService(NewsScrapRequesterPort newsScrapRequesterPort,
                              NewsPersistencePort newsPersistencePort,
                              NotificationService notificationService,
                              @Qualifier("newsProcessingExecutor") Executor newsProcessingExecutor,
                              NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsPersistencePort = newsPersistencePort;
        this.notificationService = notificationService;
        this.newsProcessingExecutor = newsProcessingExecutor;
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
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

            List<News> scrapedNews = scrapNewsFromProviders();

            if (scrapedNews.isEmpty()) {
                log.warn("스크래핑된 뉴스가 없습니다");
                return CompletableFuture.completedFuture(new Newses(List.of()));
            }

            List<News> analyzedNews = processNewsWithAI(scrapedNews);

            Newses savedNewses = saveNewsToDatabase(analyzedNews);

            sendNotificationsForImportantNews(savedNewses.getNewses());

            log.info("뉴스 스크래핑 작업 완료: {} 건", savedNewses.getNewses().size());
            return CompletableFuture.completedFuture(savedNewses);

        } catch (Exception e) {
            log.error("뉴스 스크래핑 작업 중 오류 발생", e);
            throw new RuntimeException("뉴스 스크래핑 실패", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private List<News> scrapNewsFromProviders() {
        List<NewsProvider> providers = List.of(NewsProvider.MARKETAUX);

        return providers.stream()
                .map(provider -> {
                    try {
                        return newsScrapRequesterPort.scrap(provider);
                    } catch (Exception e) {
                        log.error("뉴스 제공자 {}에서 스크래핑 실패", provider, e);
                        return List.<News>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private List<News> processNewsWithAI(List<News> newses) {
        return newses.stream()
                .map(news -> {
                    try {
                        return analyzeNewsWithAI(news);
                    } catch (Exception e) {
                        log.error("뉴스 AI 분석 실패: {}", news.getId(), e);
                        return news; // 원본 뉴스 반환
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private News analyzeNewsWithAI(News news) {
        try {
            Content originalContent = news.getOriginalContent();
            List<News> analyzedNewsList = newsAiAnalysisRequesterPort.analyseNewses(
                    AiModel.CHATGPT, originalContent);

            if (!analyzedNewsList.isEmpty()) {
                return analyzedNewsList.get(0);
            }
        } catch (Exception e) {
            log.warn("AI 분석 실패, 원본 뉴스 사용: {}", news.getId(), e);
        }
        return news;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    private Newses saveNewsToDatabase(List<News> newses) {
        try {
            log.info("데이터베이스에 {} 건의 뉴스 저장 시작", newses.size());
            Newses savedNewses = newsPersistencePort.saveAllNews(newses);
            log.info("데이터베이스 저장 완료: {} 건", savedNewses.getNewses().size());
            return savedNewses;
        } catch (Exception e) {
            log.error("데이터베이스 저장 실패", e);
            throw new RuntimeException("뉴스 저장 실패", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private void sendNotificationsForImportantNews(List<News> newses) {
        try {
            List<News> importantNews = newses.stream()
                    .filter(news -> news.getAiOverView() != null &&
                            news.getAiOverView().getSentimentScore() > 0.7)
                    .collect(Collectors.toList());

            if (!importantNews.isEmpty()) {
                log.info("중요 뉴스 {} 건에 대한 알림 발송 시작", importantNews.size());
                importantNews.forEach(news -> {
                    try {
                        notificationService.notifyUsersAboutNews(news);
                    } catch (Exception e) {
                        log.error("알림 발송 실패: {}", news.getId(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("알림 발송 처리 중 오류", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Newses scrapNewsesFallback(Exception e) {
        log.error("Circuit Breaker 폴백 실행: {}", e.getMessage());
        return new Newses(List.of());
    }
}