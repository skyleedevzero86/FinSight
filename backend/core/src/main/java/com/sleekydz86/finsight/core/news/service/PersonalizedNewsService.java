package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PersonalizedNewsService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizedNewsService.class);

    private final NewsPersistencePort newsPersistencePort;
    private final UserPersistencePort userPersistencePort;

    public PersonalizedNewsService(NewsPersistencePort newsPersistencePort, UserPersistencePort userPersistencePort) {
        this.newsPersistencePort = newsPersistencePort;
        this.userPersistencePort = userPersistencePort;
    }

    public Newses getPersonalizedNews(Long userId, int page, int size) {
        log.debug("개인화 뉴스 조회: userId={}, page={}, size={}", userId, page, size);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();
        if (userWatchlist == null || userWatchlist.isEmpty()) {
            log.debug("사용자 관심사가 비어있어 기본 뉴스 반환: userId={}", userId);
            return getDefaultNews(page, size);
        }

        NewsQueryRequest request = new NewsQueryRequest(
                null, // startDate
                null, // endDate
                null, // sentimentType
                null, // keyword
                userWatchlist, // categories
                null  // providers
        );

        Newses personalizedNews = newsPersistencePort.findAllByFilters(request);

        List<News> newsList = personalizedNews.getNewses();
        int fromIndex = Math.min(page * size, newsList.size());
        int toIndex = Math.min((page + 1) * size, newsList.size());

        List<News> pagedNews = fromIndex < newsList.size() ?
                newsList.subList(fromIndex, toIndex) : new ArrayList<>();

        log.debug("개인화 뉴스 조회 완료: userId={}, 결과 수={}", userId, pagedNews.size());
        return new Newses(pagedNews);
    }

    private Newses getDefaultNews(int page, int size) {
        log.debug("기본 뉴스 조회: page={}, size={}", page, size);

        NewsQueryRequest request = new NewsQueryRequest(
                null, null, null, null, null, null
        );

        Newses allNews = newsPersistencePort.findAllByFilters(request);

        List<News> newsList = allNews.getNewses();
        int fromIndex = Math.min(page * size, newsList.size());
        int toIndex = Math.min((page + 1) * size, newsList.size());

        List<News> pagedNews = fromIndex < newsList.size() ?
                newsList.subList(fromIndex, toIndex) : new ArrayList<>();

        log.debug("기본 뉴스 조회 완료: 결과 수={}", pagedNews.size());
        return new Newses(pagedNews);
    }

    public double calculateRelevanceScore(News news, List<TargetCategory> userWatchlist) {
        if (news.getAiOverView() == null ||
                news.getAiOverView().getTargetCategories() == null ||
                userWatchlist == null || userWatchlist.isEmpty()) {
            return 0.0;
        }

        List<TargetCategory> newsCategories = news.getAiOverView().getTargetCategories();
        long matchCount = newsCategories.stream()
                .mapToLong(category -> userWatchlist.contains(category) ? 1 : 0)
                .sum();

        return (double) matchCount / newsCategories.size();
    }

    public Newses getAdvancedPersonalizedNews(Long userId, int limit) {
        log.debug("고도화 개인화 뉴스 추천: userId={}, limit={}", userId, limit);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();
        if (userWatchlist == null || userWatchlist.isEmpty()) {
            return getDefaultNews(0, limit);
        }

        NewsQueryRequest request = new NewsQueryRequest(
                null, null, null, null, null, null
        );

        Newses allNews = newsPersistencePort.findAllByFilters(request);

        List<News> recommendedNews = allNews.getNewses().stream()
                .filter(news -> calculateRelevanceScore(news, userWatchlist) > 0.0)
                .sorted((n1, n2) -> {
                    double score1 = calculateRelevanceScore(n1, userWatchlist);
                    double score2 = calculateRelevanceScore(n2, userWatchlist);
                    int scoreCompare = Double.compare(score2, score1); // 내림차순

                    if (scoreCompare == 0) {
                        if (n1.getNewsMeta() != null && n2.getNewsMeta() != null &&
                                n1.getNewsMeta().getNewsPublishedTime() != null &&
                                n2.getNewsMeta().getNewsPublishedTime() != null) {
                            return n2.getNewsMeta().getNewsPublishedTime()
                                    .compareTo(n1.getNewsMeta().getNewsPublishedTime());
                        }
                    }
                    return scoreCompare;
                })
                .limit(limit)
                .collect(Collectors.toList());

        log.debug("고도화 개인화 뉴스 추천 완료: userId={}, 결과 수={}", userId, recommendedNews.size());
        return new Newses(recommendedNews);
    }
}