package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.annotation.Cacheable;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.SentimentAnalysisPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdvancedNewsService implements NewsCommandUseCase, NewsQueryUseCase {

    private final NewsPersistencePort newsPersistencePort;
    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;
    private final SentimentAnalysisPort sentimentAnalysisPort;

    @Autowired
    public AdvancedNewsService(NewsPersistencePort newsPersistencePort,
                               NewsScrapRequesterPort newsScrapRequesterPort,
                               NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort,
                               SentimentAnalysisPort sentimentAnalysisPort) {
        this.newsPersistencePort = newsPersistencePort;
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
        this.sentimentAnalysisPort = sentimentAnalysisPort;
    }

    @Override
    @LogExecution("뉴스 스크래핑")
    @PerformanceMonitor(threshold = 5000, metricName = "news.scrap")
    @Retryable(maxAttempts = 3, delay = 1000, multiplier = 2.0)
    @SecurityAudit(action = "NEWS_SCRAP", resource = "NEWS", level = SecurityAudit.SecurityLevel.MEDIUM)
    @Transactional
    public News scrapNews(String url) {
        try {
            News news = newsScrapRequesterPort.scrapNews(url);
            if (news == null) {
                throw new SystemException("뉴스 스크래핑에 실패했습니다.");
            }

            News savedNews = newsPersistencePort.save(news);
            return savedNews;
        } catch (Exception e) {
            throw new SystemException("뉴스 스크래핑 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @LogExecution("뉴스 AI 분석")
    @PerformanceMonitor(threshold = 10000, metricName = "news.ai_analysis")
    @Retryable(maxAttempts = 2, delay = 2000, multiplier = 1.5)
    @SecurityAudit(action = "NEWS_AI_ANALYSIS", resource = "NEWS", level = SecurityAudit.SecurityLevel.HIGH)
    @Transactional
    public News analyzeNews(Long newsId) {
        try {
            Optional<News> newsOpt = newsPersistencePort.findById(newsId);
            if (newsOpt.isEmpty()) {
                throw new SystemException("뉴스를 찾을 수 없습니다.");
            }

            News news = newsOpt.get();
            News analyzedNews = newsAiAnalysisRequesterPort.analyzeNews(news);

            News savedNews = newsPersistencePort.save(analyzedNews);
            return savedNews;
        } catch (Exception e) {
            throw new SystemException("뉴스 AI 분석 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @LogExecution("뉴스 감정 분석")
    @PerformanceMonitor(threshold = 3000, metricName = "news.sentiment_analysis")
    @Retryable(maxAttempts = 3, delay = 1000, multiplier = 2.0)
    @SecurityAudit(action = "NEWS_SENTIMENT_ANALYSIS", resource = "NEWS", level = SecurityAudit.SecurityLevel.MEDIUM)
    @Transactional
    public News analyzeSentiment(Long newsId) {
        try {
            Optional<News> newsOpt = newsPersistencePort.findById(newsId);
            if (newsOpt.isEmpty()) {
                throw new SystemException("뉴스를 찾을 수 없습니다.");
            }

            News news = newsOpt.get();
            News analyzedNews = sentimentAnalysisPort.analyzeSentiment(news);

            News savedNews = newsPersistencePort.save(analyzedNews);
            return savedNews;
        } catch (Exception e) {
            throw new SystemException("뉴스 감정 분석 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @LogExecution("뉴스 조회")
    @Cacheable(value = "news", key = "#newsId", expireSeconds = 3600)
    @PerformanceMonitor(threshold = 1000, metricName = "news.query")
    @SecurityAudit(action = "NEWS_QUERY", resource = "NEWS", level = SecurityAudit.SecurityLevel.LOW)
    public NewsDetailResponse getNewsDetail(Long newsId) {
        try {
            Optional<News> newsOpt = newsPersistencePort.findById(newsId);
            if (newsOpt.isEmpty()) {
                throw new SystemException("뉴스를 찾을 수 없습니다.");
            }

            News news = newsOpt.get();
            return NewsDetailResponse.builder()
                    .id(news.getId())
                    .title(news.getTitle())
                    .content(news.getContent())
                    .url(news.getUrl())
                    .publishedAt(news.getPublishedAt())
                    .sentiment(news.getSentiment())
                    .aiOverview(news.getAiOverview())
                    .build();
        } catch (Exception e) {
            throw new SystemException("뉴스 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @LogExecution("뉴스 목록 조회")
    @Cacheable(value = "news_list", key = "#request.toString()", expireSeconds = 1800)
    @PerformanceMonitor(threshold = 2000, metricName = "news.list_query")
    @SecurityAudit(action = "NEWS_LIST_QUERY", resource = "NEWS", level = SecurityAudit.SecurityLevel.LOW)
    public Newses getNewsList(NewsQueryRequest request) {
        try {
            List<News> newsList = newsPersistencePort.findByQuery(request);
            return new Newses(newsList);
        } catch (Exception e) {
            throw new SystemException("뉴스 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @LogExecution("뉴스 검색")
    @Cacheable(value = "news_search", key = "#request.toString()", expireSeconds = 1800)
    @PerformanceMonitor(threshold = 3000, metricName = "news.search")
    @SecurityAudit(action = "NEWS_SEARCH", resource = "NEWS", level = SecurityAudit.SecurityLevel.LOW)
    public Newses searchNews(NewsSearchRequest request) {
        try {
            List<News> newsList = newsPersistencePort.searchByQuery(request);
            return new Newses(newsList);
        } catch (Exception e) {
            throw new SystemException("뉴스 검색 중 오류가 발생했습니다.", e);
        }
    }
}