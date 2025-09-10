package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NewsQueryService implements NewsQueryUseCase {

        private static final Logger log = LoggerFactory.getLogger(NewsQueryService.class);
        private final PersonalizedNewsService personalizedNewsService;
        private final NewsPersistencePort newsPersistencePort;
        private final NewsStatisticsPersistencePort newsStatisticsPersistencePort;
        private final UserPersistencePort userPersistencePort;

        public NewsQueryService(NewsPersistencePort newsPersistencePort,
                                NewsStatisticsPersistencePort newsStatisticsPersistencePort,
                                PersonalizedNewsService personalizedNewsService,
                                UserPersistencePort userPersistencePort) {
                this.newsPersistencePort = newsPersistencePort;
                this.newsStatisticsPersistencePort = newsStatisticsPersistencePort;
                this.personalizedNewsService = personalizedNewsService;
                this.userPersistencePort = userPersistencePort;
        }

        @Override
        public Newses getPersonalizedNews(String userEmail, int limit) {
                log.debug("개인화 뉴스 조회: userEmail={}, limit={}", userEmail, limit);

                try {
                        // 이제 User 도메인 객체를 직접 반환받음
                        Optional<User> userOpt = userPersistencePort.findByEmail(userEmail);
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
        public Newses findAllByFilters(NewsQueryRequest request) {
                log.info("Finding news by filters: {}", request);
                return newsPersistencePort.findAllByFilters(request);
        }

        @Override
        public PaginationResponse<Newses> searchNews(NewsSearchRequest request) {
                log.info("Searching news with request: {}", request);

                Pageable pageable = Pageable.ofSize(request.getSize())
                        .withPage(request.getPage());

                Newses newses = performSearch(request, pageable);
                long totalElements = getTotalCount(request);

                return new PaginationResponse<>(
                        List.of(newses),
                        request.getPage(),
                        request.getSize(),
                        totalElements);
        }

        @Override
        public NewsDetailResponse getNewsDetail(Long newsId) {
                log.info("Getting news detail for ID: {}", newsId);

                News news = newsPersistencePort.findById(newsId)
                        .orElseThrow(() -> new NewsNotFoundException(newsId));

                newsStatisticsPersistencePort.incrementViewCount(newsId);

                return NewsDetailResponse.builder()
                        .id(news.getId())
                        .newsProvider(news.getNewsProvider())
                        .originalTitle(news.getOriginalContent() != null ? news.getOriginalContent().getTitle()
                                : null)
                        .originalContent(news.getOriginalContent() != null
                                ? news.getOriginalContent().getContent()
                                : null)
                        .translatedTitle(news.getTranslatedContent() != null
                                ? news.getTranslatedContent().getTitle()
                                : null)
                        .translatedContent(news.getTranslatedContent() != null
                                ? news.getTranslatedContent().getContent()
                                : null)
                        .overview(news.getAiOverView() != null ? news.getAiOverView().getOverview() : null)
                        .sentimentType(news.getAiOverView() != null ? news.getAiOverView().getSentimentType()
                                : null)
                        .sentimentScore(news.getAiOverView() != null ? news.getAiOverView().getSentimentScore()
                                : null)
                        .categories(news.getAiOverView() != null ? news.getAiOverView().getTargetCategories()
                                : null)
                        .publishedTime(news.getNewsMeta() != null ? news.getNewsMeta().getNewsPublishedTime()
                                : null)
                        .scrapedTime(news.getScrapedTime())
                        .sourceUrl(news.getNewsMeta() != null ? news.getNewsMeta().getSourceUrl() : null)
                        .statistics(null)
                        .comments(null)
                        .relatedNews(null)
                        .build();
        }

        @Override
        public Newses getRelatedNews(Long newsId, int limit) {
                log.info("Getting related news for ID: {}, limit: {}", newsId, limit);

                News news = newsPersistencePort.findById(newsId)
                        .orElseThrow(() -> new NewsNotFoundException(newsId));

                if (news.getAiOverView() == null || news.getAiOverView().getTargetCategories().isEmpty()) {
                        return new Newses();
                }

                List<TargetCategory> categories = news.getAiOverView().getTargetCategories();
                NewsQueryRequest request = new NewsQueryRequest(
                        null, null, null, null, categories, null);

                Newses allNews = newsPersistencePort.findAllByFilters(request);
                List<News> relatedNews = allNews.getNewses().stream()
                        .filter(n -> !n.getId().equals(newsId))
                        .limit(limit)
                        .toList();

                return new Newses(relatedNews);
        }

        @Override
        public Newses getPopularNews(int limit) {
                log.info("Getting popular news, limit: {}", limit);

                NewsQueryRequest request = new NewsQueryRequest(
                        null, null, null, null, null, null);

                Newses allNews = newsPersistencePort.findAllByFilters(request);
                List<News> popularNews = allNews.getNewses().stream()
                        .sorted((n1, n2) -> {
                                if (n1.getNewsMeta() == null || n1.getNewsMeta().getNewsPublishedTime() == null)
                                        return 1;
                                if (n2.getNewsMeta() == null || n2.getNewsMeta().getNewsPublishedTime() == null)
                                        return -1;
                                return n2.getNewsMeta().getNewsPublishedTime()
                                        .compareTo(n1.getNewsMeta().getNewsPublishedTime());
                        })
                        .limit(limit)
                        .toList();

                return new Newses(popularNews);
        }

        @Override
        public Newses getLatestNews(int limit) {
                log.info("Getting latest news, limit: {}", limit);

                NewsQueryRequest request = new NewsQueryRequest(
                        null, null, null, null, null, null);

                Newses allNews = newsPersistencePort.findAllByFilters(request);
                List<News> latestNews = allNews.getNewses().stream()
                        .sorted((n1, n2) -> {
                                if (n1.getNewsMeta() == null || n1.getNewsMeta().getNewsPublishedTime() == null)
                                        return 1;
                                if (n2.getNewsMeta() == null || n2.getNewsMeta().getNewsPublishedTime() == null)
                                        return -1;
                                return n2.getNewsMeta().getNewsPublishedTime()
                                        .compareTo(n1.getNewsMeta().getNewsPublishedTime());
                        })
                        .limit(limit)
                        .toList();

                return new Newses(latestNews);
        }

        @Override
        public Newses getNewsByCategory(String category, int limit) {
                log.info("Getting news by category: {}, limit: {}", category, limit);

                try {
                        TargetCategory targetCategory = TargetCategory.valueOf(category.toUpperCase());
                        NewsQueryRequest request = new NewsQueryRequest(
                                null, null, null, null, List.of(targetCategory), null);
                        return newsPersistencePort.findAllByFilters(request);
                } catch (IllegalArgumentException e) {
                        log.warn("Invalid category: {}", category);
                        return new Newses();
                }
        }

        private Newses performSearch(NewsSearchRequest request, Pageable pageable) {
                NewsQueryRequest queryRequest = new NewsQueryRequest(
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getSentimentType(),
                        request.getKeyword(),
                        request.getCategories(),
                        request.getProviders());

                return newsPersistencePort.findAllByFilters(queryRequest);
        }

        private long getTotalCount(NewsSearchRequest request) {
                NewsQueryRequest queryRequest = new NewsQueryRequest(
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getSentimentType(),
                        request.getKeyword(),
                        request.getCategories(),
                        request.getProviders());

                return newsPersistencePort.findAllByFilters(queryRequest).getNewses().size();
        }
}