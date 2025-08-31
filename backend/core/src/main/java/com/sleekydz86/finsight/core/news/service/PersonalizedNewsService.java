package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedNewsService {

    private final NewsJpaRepository newsJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public Newses getPersonalizedNews(Long userId, int page, int size) {
        log.info("사용자 {}의 맞춤 뉴스 조회 (페이지: {}, 크기: {})", userId, page, size);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            log.warn("사용자 {}의 관심 종목이 설정되지 않음", userId);
            return new Newses(Collections.emptyList());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "newsPublishedTime"));
        List<NewsJpaEntity> newsEntities = findNewsByUserPreferences(userWatchlist, pageable);

        List<News> personalizedNews = newsEntities.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());

        log.info("사용자 {}의 맞춤 뉴스 {}건 조회 완료", userId, personalizedNews.size());

        return new Newses(personalizedNews);
    }

    private List<NewsJpaEntity> findNewsByUserPreferences(List<TargetCategory> categories, Pageable pageable) {
        log.debug("사용자 관심 종목 기반 뉴스 조회: {}", categories);
        return newsJpaRepository.findAll(pageable).getContent();
    }

    public List<News> getImportantNewsForUser(Long userId, int limit) {
        log.info("사용자 {}의 중요 뉴스 알림 대상 조회 (제한: {})", userId, limit);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<NewsJpaEntity> importantNews = newsJpaRepository.findAll()
                .stream()
                .filter(news -> news.getNewsPublishedTime().isAfter(since))
                .limit(limit)
                .collect(Collectors.toList());

        return importantNews.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserNewsSummary(Long userId) {
        log.info("사용자 {}의 뉴스 요약 통계 조회", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(7);

        Map<String, Object> summary = new HashMap<>();

        Map<TargetCategory, Long> categoryCounts = new HashMap<>();
        for (TargetCategory category : userWatchlist) {
            categoryCounts.put(category, 0L);
        }
        summary.put("categoryCounts", categoryCounts);

        Map<SentimentType, Long> sentimentCounts = new HashMap<>();
        for (SentimentType sentiment : SentimentType.values()) {
            sentimentCounts.put(sentiment, 0L);
        }
        summary.put("sentimentCounts", sentimentCounts);

        log.debug("사용자 {}의 뉴스 요약 통계 생성 완료", userId);

        return summary;
    }

    public Map<Long, Double> calculateNewsRecommendationScores(Long userId, List<News> newsList) {
        log.debug("사용자 {}의 뉴스 추천 점수 계산", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> scores = new HashMap<>();

        for (News news : newsList) {
            double score = calculateNewsScore(news, userWatchlist);
            scores.put(news.getId(), score);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private double calculateNewsScore(News news, List<TargetCategory> userWatchlist) {
        double score = 0.0;

        if (news.getAiOverView() != null &&
                news.getAiOverView().getTargetCategories() != null) {

            long matchingCategories = news.getAiOverView().getTargetCategories().stream()
                    .filter(userWatchlist::contains)
                    .count();

            score += matchingCategories * 10.0;
        }

        if (news.getAiOverView() != null) {
            switch (news.getAiOverView().getSentimentType()) {
                case POSITIVE:
                    score += 5.0;
                    break;
                case NEGATIVE:
                    score += 3.0;
                    break;
                case NEUTRAL:
                    score += 1.0;
                    break;
            }
        }

        long hoursSincePublished = java.time.Duration.between(
                news.getNewsMeta().getNewsPublishedTime(),
                LocalDateTime.now()
        ).toHours();

        if (hoursSincePublished <= 24) {
            score += 3.0;
        } else if (hoursSincePublished <= 72) {
            score += 1.0;
        }

        return score;
    }

    private News convertToDomain(NewsJpaEntity entity) {
        Content originalContent = new Content(
                entity.getOriginalTitle() != null ? entity.getOriginalTitle() : "",
                entity.getOriginalContent() != null ? entity.getOriginalContent() : ""
        );

        Content translatedContent = null;
        if (entity.getTranslatedTitle() != null || entity.getTranslatedContent() != null) {
            translatedContent = new Content(
                    entity.getTranslatedTitle() != null ? entity.getTranslatedTitle() : "",
                    entity.getTranslatedContent() != null ? entity.getTranslatedContent() : ""
            );
        }

        NewsMeta newsMeta = new NewsMeta(
                entity.getNewsProvider(),
                entity.getNewsPublishedTime(),
                entity.getSourceUrl()
        );

        AiOverview aiOverview = null;
        if (entity.getOverview() != null ||
                entity.getSentimentType() != null ||
                entity.getSentimentScore() != null ||
                (entity.getTargetCategories() != null && !entity.getTargetCategories().isEmpty())) {

            double sentimentScore = entity.getSentimentScore() != null ? entity.getSentimentScore() : 0.0;

            SentimentType sentimentType = entity.getSentimentType() != null ? entity.getSentimentType() : SentimentType.NEUTRAL;

            List<TargetCategory> targetCategories = entity.getTargetCategories() != null ?
                    new ArrayList<>(entity.getTargetCategories()) : new ArrayList<>();

            aiOverview = new AiOverview(
                    entity.getOverview() != null ? entity.getOverview() : "",
                    sentimentType,
                    sentimentScore,
                    targetCategories
            );
        }

        return new News(
                entity.getId(),
                newsMeta,
                entity.getScrapedTime(),
                originalContent,
                translatedContent,
                aiOverview
        );
    }

    public Newses getFilteredAndSortedPersonalizedNews(Long userId, int page, int size) {
        log.info("사용자 {}의 필터링된 맞춤 뉴스 조회", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return new Newses(Collections.emptyList());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "newsPublishedTime"));
        List<NewsJpaEntity> newsEntities = findNewsByUserPreferences(userWatchlist, pageable);

        List<News> newsList = newsEntities.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());

        Map<Long, Double> scores = calculateNewsRecommendationScores(userId, newsList);

        List<News> sortedNews = newsList.stream()
                .sorted((n1, n2) -> {
                    double score1 = scores.getOrDefault(n1.getId(), 0.0);
                    double score2 = scores.getOrDefault(n2.getId(), 0.0);
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());

        log.info("사용자 {}의 맞춤 뉴스 {}건 필터링 및 정렬 완료", userId, sortedNews.size());

        return new Newses(sortedNews);
    }

    public Map<TargetCategory, List<News>> getLatestNewsByCategory(Long userId, int limitPerCategory) {
        log.info("사용자 {}의 카테고리별 최신 뉴스 조회 (카테고리당 제한: {})", userId, limitPerCategory);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<TargetCategory, List<News>> categoryNewsMap = new HashMap<>();

        for (TargetCategory category : userWatchlist) {
            List<NewsJpaEntity> categoryNews = newsJpaRepository.findAll()
                    .stream()
                    .filter(news -> news.getTargetCategories() != null &&
                            news.getTargetCategories().contains(category))
                    .sorted((n1, n2) -> n2.getNewsPublishedTime().compareTo(n1.getNewsPublishedTime()))
                    .limit(limitPerCategory)
                    .collect(Collectors.toList());

            List<News> domainNews = categoryNews.stream()
                    .map(this::convertToDomain)
                    .collect(Collectors.toList());

            categoryNewsMap.put(category, domainNews);
        }

        log.info("사용자 {}의 카테고리별 최신 뉴스 조회 완료: {}개 카테고리", userId, categoryNewsMap.size());

        return categoryNewsMap;
    }

    public List<News> getPositiveNewsForUser(Long userId, int limit) {
        log.info("사용자 {}의 긍정적 뉴스 조회 (제한: {})", userId, limit);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<NewsJpaEntity> positiveNews = newsJpaRepository.findAll()
                .stream()
                .filter(news -> news.getNewsPublishedTime().isAfter(since))
                .filter(news -> news.getSentimentType() == SentimentType.POSITIVE)
                .filter(news -> news.getTargetCategories() != null &&
                        news.getTargetCategories().stream().anyMatch(userWatchlist::contains))
                .sorted((n1, n2) -> n2.getNewsPublishedTime().compareTo(n1.getNewsPublishedTime()))
                .limit(limit)
                .collect(Collectors.toList());

        return positiveNews.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    public List<News> getNegativeNewsForUser(Long userId, int limit) {
        log.info("사용자 {}의 부정적 뉴스 조회 (제한: {})", userId, limit);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<NewsJpaEntity> negativeNews = newsJpaRepository.findAll()
                .stream()
                .filter(news -> news.getNewsPublishedTime().isAfter(since))
                .filter(news -> news.getSentimentType() == SentimentType.NEGATIVE)
                .filter(news -> news.getTargetCategories() != null &&
                        news.getTargetCategories().stream().anyMatch(userWatchlist::contains))
                .sorted((n1, n2) -> n2.getNewsPublishedTime().compareTo(n1.getNewsPublishedTime()))
                .limit(limit)
                .collect(Collectors.toList());

        return negativeNews.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    public List<News> getNewsRecommendationsByHistory(Long userId, int limit) {
        log.info("사용자 {}의 읽기 히스토리 기반 뉴스 추천 (제한: {})", userId, limit);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();

        if (userWatchlist.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<NewsJpaEntity> recommendedNews = newsJpaRepository.findAll()
                .stream()
                .filter(news -> news.getNewsPublishedTime().isAfter(since))
                .filter(news -> news.getTargetCategories() != null &&
                        news.getTargetCategories().stream().anyMatch(userWatchlist::contains))
                .sorted((n1, n2) -> {
                    double score1 = calculateComplexScore(n1, userWatchlist);
                    double score2 = calculateComplexScore(n2, userWatchlist);
                    return Double.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());

        return recommendedNews.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    private double calculateComplexScore(NewsJpaEntity news, List<TargetCategory> userWatchlist) {
        double score = 0.0;

        long hoursSincePublished = java.time.Duration.between(
                news.getNewsPublishedTime(),
                LocalDateTime.now()
        ).toHours();

        if (hoursSincePublished <= 24) {
            score += 10.0;
        } else if (hoursSincePublished <= 72) {
            score += 7.0;
        } else if (hoursSincePublished <= 168) {
            score += 4.0;
        } else if (hoursSincePublished <= 720) {
            score += 1.0;
        }

        if (news.getSentimentType() != null) {
            switch (news.getSentimentType()) {
                case POSITIVE:
                    score += 5.0;
                    break;
                case NEGATIVE:
                    score += 3.0;
                    break;
                case NEUTRAL:
                    score += 2.0;
                    break;
            }
        }

        if (news.getTargetCategories() != null) {
            long matchingCategories = news.getTargetCategories().stream()
                    .filter(userWatchlist::contains)
                    .count();
            score += matchingCategories * 8.0;
        }

        if (news.getNewsProvider() != null) {
            switch (news.getNewsProvider().name()) {
                case "BLOOMBERG":
                    score += 3.0;
                    break;
                case "MARKETAUX":
                    score += 2.0;
                    break;
                default:
                    score += 1.0;
            }
        }

        return score;
    }
}