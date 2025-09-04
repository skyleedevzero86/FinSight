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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PersonalizedNewsService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizedNewsService.class);

    private final NewsJpaRepository newsJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public PersonalizedNewsService(NewsJpaRepository newsJpaRepository, UserJpaRepository userJpaRepository) {
        this.newsJpaRepository = newsJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    public Newses getPersonalizedNews(Long userId, int page, int size) {
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> userWatchlist = user.getWatchlist();
        if (userWatchlist == null || userWatchlist.isEmpty()) {
            return getDefaultNews(page, size);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("scrapedTime").descending());
        Page<NewsJpaEntity> newsPage = newsJpaRepository.findAll(pageable);

        List<News> personalizedNews = newsPage.getContent().stream()
                .filter(news -> isRelevantToUser(news, userWatchlist))
                .map(this::convertToDomain)
                .collect(Collectors.toList());

        return new Newses(personalizedNews);
    }

    private boolean isRelevantToUser(NewsJpaEntity news, List<TargetCategory> userWatchlist) {
        if (news.getTargetCategories() == null || news.getTargetCategories().isEmpty()) {
            return false;
        }

        return news.getTargetCategories().stream()
                .anyMatch(userWatchlist::contains);
    }

    private Newses getDefaultNews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scrapedTime").descending());
        Page<NewsJpaEntity> newsPage = newsJpaRepository.findAll(pageable);

        List<News> defaultNews = newsPage.getContent().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());

        return new Newses(defaultNews);
    }

    private News convertToDomain(NewsJpaEntity entity) {
        Content originalContent = new Content(entity.getOriginalTitle(), entity.getOriginalContent());
        Content translatedContent = new Content(entity.getTranslatedTitle(), entity.getTranslatedContent());

        NewsMeta newsMeta = NewsMeta.of(
                entity.getNewsProvider(),
                entity.getNewsPublishedTime(),
                entity.getSourceUrl()
        );

        AiOverview aiOverview = new AiOverview(
                entity.getOverview(),
                entity.getSentimentType(),
                entity.getSentimentScore(),
                entity.getTargetCategories()
        );

        return News.builder()
                .id(entity.getId())
                .newsMeta(newsMeta)
                .scrapedTime(entity.getScrapedTime())
                .originalContent(originalContent)
                .translatedContent(translatedContent)
                .aiOverView(aiOverview)
                .build();
    }
}