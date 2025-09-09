package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.global.annotation.Cacheable;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;



@Service
@Qualifier("advancedNewsService")
public class AdvancedNewsService implements NewsCommandUseCase, NewsQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdvancedNewsService.class);

    private final NewsPersistencePort newsPersistencePort;
    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;
    private final NewsStatisticsPersistencePort newsStatisticsPersistencePort;
    private final PersonalizedNewsService personalizedNewsService;
    private final UserJpaRepository userJpaRepository;

    public AdvancedNewsService(NewsPersistencePort newsPersistencePort,
                               NewsScrapRequesterPort newsScrapRequesterPort,
                               NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort,
                               NewsStatisticsPersistencePort newsStatisticsPersistencePort,
                               PersonalizedNewsService personalizedNewsService,
                               UserJpaRepository userJpaRepository) {
        this.newsPersistencePort = newsPersistencePort;
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
        this.newsStatisticsPersistencePort = newsStatisticsPersistencePort;
        this.personalizedNewsService = personalizedNewsService;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Newses getPersonalizedNews(String userEmail, int limit) {
        log.debug("개인화 뉴스 조회: userEmail={}, limit={}", userEmail, limit);

        try {
            Optional<UserJpaEntity> userOpt = userJpaRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                log.warn("사용자를 찾을 수 없습니다: {}", userEmail);
                return new Newses();
            }

            Long userId = userOpt.get().getId();
            return personalizedNewsService.getPersonalizedNews(userId, 0, limit);
        } catch (Exception e) {
            log.error("개인화 뉴스 조회 실패: userEmail={}", userEmail, e);
            throw new SystemException("개인화 뉴스 조회 중 오류가 발생했습니다", "PERSONALIZED_NEWS_ERROR", e);
        }
    }


    @Override
    @Transactional
    @LogExecution
    public CompletableFuture<Newses> scrapNewses() {
        log.info("뉴스 스크래핑 시작");

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<News> allScrapedNews = new ArrayList<>();

                for (NewsProvider provider : NewsProvider.values()) {
                    if (provider != NewsProvider.ALL) {
                        try {
                            List<News> providerNews = newsScrapRequesterPort.scrap(provider);
                            allScrapedNews.addAll(providerNews);
                            log.info("{}에서 {}건의 뉴스 스크래핑 완료", provider, providerNews.size());
                        } catch (Exception e) {
                            log.error("{}에서 뉴스 스크래핑 실패: {}", provider, e.getMessage());
                        }
                    }
                }

                if (!allScrapedNews.isEmpty()) {
                    Newses savedNewses = newsPersistencePort.saveAllNews(allScrapedNews);
                    log.info("총 {}건의 뉴스 저장 완료", savedNewses.getNewses().size());
                    return savedNewses;
                }

                return new Newses();
            } catch (Exception e) {
                log.error("뉴스 스크래핑 중 오류 발생", e);
                throw new RuntimeException("뉴스 스크래핑 실패", e);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:all:filters", expireSeconds = 300)
    public Newses findAllByFilters(NewsQueryRequest request) {
        log.info("필터 조건으로 뉴스 조회: {}", request);
        return newsPersistencePort.findAllByFilters(request);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:search", expireSeconds = 300)
    public PaginationResponse<Newses> searchNews(NewsSearchRequest request) {
        log.info("뉴스 검색: {}", request);

        NewsQueryRequest queryRequest = new NewsQueryRequest(
                request.getStartDate(),
                request.getEndDate(),
                request.getSentimentType(),
                request.getKeyword(),
                request.getCategories(),
                request.getProviders());

        Newses newses = newsPersistencePort.findAllByFilters(queryRequest);

        List<News> allNews = newses.getNewses();
        int totalElements = allNews.size();
        int startIndex = request.getPage() * request.getSize();
        int endIndex = Math.min(startIndex + request.getSize(), totalElements);

        List<News> pagedNews = allNews.subList(startIndex, endIndex);
        Newses pagedNewses = new Newses(pagedNews);

        return PaginationResponse.<Newses>builder()
                .content(List.of(pagedNewses))
                .page(request.getPage())
                .size(request.getSize())
                .totalElements(totalElements)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:detail", expireSeconds = 600)
    public NewsDetailResponse getNewsDetail(Long newsId) {
        log.info("뉴스 상세 조회: {}", newsId);

        Optional<News> newsOpt = newsPersistencePort.findById(newsId);
        if (newsOpt.isEmpty()) {
            throw new RuntimeException("뉴스를 찾을 수 없습니다: " + newsId);
        }

        News news = newsOpt.get();
        return buildNewsDetailResponse(news);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:related", expireSeconds = 300)
    public Newses getRelatedNews(Long newsId, int limit) {
        log.info("관련 뉴스 조회: {}, limit: {}", newsId, limit);

        Optional<News> newsOpt = newsPersistencePort.findById(newsId);
        if (newsOpt.isEmpty()) {
            return new Newses();
        }

        News news = newsOpt.get();

        List<TargetCategory> categories = news.getAiOverView() != null ? news.getAiOverView().getTargetCategories()
                : new ArrayList<>();

        NewsQueryRequest queryRequest = new NewsQueryRequest(
                null, null, null, null, categories, List.of(news.getNewsProvider()));

        Newses relatedNewses = newsPersistencePort.findAllByFilters(queryRequest);
        List<News> relatedNewsList = relatedNewses.getNewses().stream()
                .filter(n -> !n.getId().equals(newsId))
                .limit(limit)
                .collect(Collectors.toList());

        return new Newses(relatedNewsList);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:popular", expireSeconds = 300)
    public Newses getPopularNews(int limit) {
        log.info("인기 뉴스 조회: limit: {}", limit);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        NewsQueryRequest queryRequest = new NewsQueryRequest(
                weekAgo, null, null, null, null, null);

        Newses recentNewses = newsPersistencePort.findAllByFilters(queryRequest);
        List<News> popularNews = recentNewses.getNewses().stream()
                .limit(limit)
                .collect(Collectors.toList());

        return new Newses(popularNews);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:latest", expireSeconds = 300)
    public Newses getLatestNews(int limit) {
        log.info("최신 뉴스 조회: limit: {}", limit);

        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        NewsQueryRequest queryRequest = new NewsQueryRequest(
                dayAgo, null, null, null, null, null);

        Newses recentNewses = newsPersistencePort.findAllByFilters(queryRequest);
        List<News> latestNews = recentNewses.getNewses().stream()
                .sorted((n1, n2) -> n2.getScrapedTime().compareTo(n1.getScrapedTime()))
                .limit(limit)
                .collect(Collectors.toList());

        return new Newses(latestNews);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "news:category", expireSeconds = 300)
    public Newses getNewsByCategory(String category, int limit) {
        log.info("카테고리별 뉴스 조회: {}, limit: {}", category, limit);

        try {
            TargetCategory targetCategory = TargetCategory.valueOf(category.toUpperCase());
            NewsQueryRequest queryRequest = new NewsQueryRequest(
                    null, null, null, null, List.of(targetCategory), null);

            Newses categoryNewses = newsPersistencePort.findAllByFilters(queryRequest);
            List<News> limitedNews = categoryNewses.getNewses().stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            return new Newses(limitedNews);
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 카테고리: {}", category);
            return new Newses();
        }
    }

    @Transactional
    @LogExecution
    public Newses processNewsWithAI(Newses newses) {
        log.info("AI 분석을 통한 뉴스 처리 시작: {}건", newses.getNewses().size());

        List<News> processedNews = new ArrayList<>();

        for (News news : newses.getNewses()) {
            try {
                if (news.getAiOverView() == null && news.getOriginalContent() != null) {
                    List<News> analyzedNews = newsAiAnalysisRequesterPort.analyseNewses(
                            AiModel.OPENAI,
                            news.getOriginalContent());

                    if (!analyzedNews.isEmpty()) {
                        processedNews.add(analyzedNews.get(0));
                    } else {
                        processedNews.add(news);
                    }
                } else {
                    processedNews.add(news);
                }
            } catch (Exception e) {
                log.error("뉴스 AI 분석 실패: {}", news.getId(), e);
                processedNews.add(news);
            }
        }

        return new Newses(processedNews);
    }

    @Transactional
    @LogExecution
    public Newses translateNews(Newses newses) {
        log.info("뉴스 번역 처리 시작: {}건", newses.getNewses().size());

        List<News> translatedNews = new ArrayList<>();

        for (News news : newses.getNewses()) {
            try {
                if (news.getTranslatedContent() == null && news.getOriginalContent() != null) {
                    Content translatedContent = new Content(
                            "번역된 제목: " + news.getOriginalContent().getTitle(),
                            "번역된 내용: " + news.getOriginalContent().getContent());

                    News translatedNewsItem = new News(
                            news.getId(),
                            news.getNewsProvider(),
                            news.getScrapedTime(),
                            news.getOriginalContent(),
                            translatedContent,
                            news.getAiOverView(),
                            news.getNewsMeta());

                    translatedNews.add(translatedNewsItem);
                } else {
                    translatedNews.add(news);
                }
            } catch (Exception e) {
                log.error("뉴스 번역 실패: {}", news.getId(), e);
                translatedNews.add(news);
            }
        }

        return new Newses(translatedNews);
    }

    @Transactional
    @LogExecution
    public Newses analyzeNewsSentiment(Newses newses) {
        log.info("뉴스 감정 분석 처리 시작: {}건", newses.getNewses().size());

        List<News> analyzedNews = new ArrayList<>();

        for (News news : newses.getNewses()) {
            try {
                if (news.getAiOverView() == null && news.getOriginalContent() != null) {
                    AiOverview aiOverview = new AiOverview(
                            "자동 생성된 요약",
                            SentimentType.NEUTRAL,
                            0.5,
                            List.of(TargetCategory.GENERAL));

                    News analyzedNewsItem = new News(
                            news.getId(),
                            news.getNewsProvider(),
                            news.getScrapedTime(),
                            news.getOriginalContent(),
                            news.getTranslatedContent(),
                            aiOverview,
                            news.getNewsMeta());

                    analyzedNews.add(analyzedNewsItem);
                } else {
                    analyzedNews.add(news);
                }
            } catch (Exception e) {
                log.error("뉴스 감정 분석 실패: {}", news.getId(), e);
                analyzedNews.add(news);
            }
        }

        return new Newses(analyzedNews);
    }

    private NewsDetailResponse buildNewsDetailResponse(News news) {
        NewsDetailResponse.Builder builder = NewsDetailResponse.builder()
                .id(news.getId())
                .newsProvider(news.getNewsProvider())
                .scrapedTime(news.getScrapedTime())
                .comments(new Comments());

        if (news.getOriginalContent() != null) {
            builder.originalTitle(news.getOriginalContent().getTitle())
                    .originalContent(news.getOriginalContent().getContent());
        }

        if (news.getTranslatedContent() != null) {
            builder.translatedTitle(news.getTranslatedContent().getTitle())
                    .translatedContent(news.getTranslatedContent().getContent());
        }

        if (news.getAiOverView() != null) {
            builder.overview(news.getAiOverView().getOverview())
                    .sentimentType(news.getAiOverView().getSentimentType())
                    .sentimentScore(news.getAiOverView().getSentimentScore())
                    .categories(news.getAiOverView().getTargetCategories());
        }

        if (news.getNewsMeta() != null) {
            builder.publishedTime(news.getNewsMeta().getPublishedTime())
                    .sourceUrl(news.getNewsMeta().getSourceUrl());
        }

        try {
            Optional<NewsStatistics> statistics = newsStatisticsPersistencePort.findByNewsId(news.getId());
            if (statistics.isPresent()) {
                builder.statistics(statistics.get());
            }
        } catch (Exception e) {
            log.warn("뉴스 통계 조회 실패: {}", news.getId(), e);
        }

        return builder.build();
    }
}