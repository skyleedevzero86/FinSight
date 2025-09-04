package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(NewsDeduplicationService.class);

    @Value("${news.deduplication.url-based:true}")
    private boolean urlBasedDeduplication;

    @Value("${news.deduplication.title-similarity-threshold:0.8}")
    private double titleSimilarityThreshold;

    @Value("${news.deduplication.content-similarity-threshold:0.7}")
    private double contentSimilarityThreshold;

    @Value("${news.deduplication.time-window-hours:24}")
    private int timeWindowHours;

    private final Map<String, News> urlCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> titleHashCache = new ConcurrentHashMap<>();
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong duplicatesFound = new AtomicLong(0);

    public News deduplicateNews(News news) {
        try {
            totalProcessed.incrementAndGet();

            if (urlBasedDeduplication && isDuplicateByUrl(news)) {
                duplicatesFound.incrementAndGet();
                log.debug("URL 기반 중복 뉴스 발견: {}", news.getNewsMeta().getSourceUrl());
                return null;
            }

            if (isDuplicateByTitle(news)) {
                duplicatesFound.incrementAndGet();
                log.debug("제목 기반 중복 뉴스 발견: {}", news.getOriginalContent().getTitle());
                return null;
            }

            if (isDuplicateByContent(news)) {
                duplicatesFound.incrementAndGet();
                log.debug("내용 기반 중복 뉴스 발견");
                return null;
            }

            cacheNews(news);
            return news;

        } catch (Exception e) {
            log.error("뉴스 중복 제거 중 오류 발생", e);
            return news;
        }
    }

    private boolean isDuplicateByUrl(News news) {
        String url = news.getNewsMeta().getSourceUrl();
        return urlCache.containsKey(url);
    }

    private boolean isDuplicateByTitle(News news) {
        String title = news.getOriginalContent().getTitle();
        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        String titleHash = generateTitleHash(title);
        String timeKey = getTimeWindowKey(news.getScrapedTime());

        Set<String> titleHashes = titleHashCache.computeIfAbsent(timeKey, k -> ConcurrentHashMap.newKeySet());

        if (titleHashes.contains(titleHash)) {
            return true;
        }

        titleHashes.add(titleHash);
        return false;
    }

    private boolean isDuplicateByContent(News news) {
        Content content = news.getOriginalContent();
        if (content == null || content.getContent() == null) {
            return false;
        }

        String contentHash = generateContentHash(content.getContent());
        String timeKey = getTimeWindowKey(news.getScrapedTime());

        Set<String> contentHashes = titleHashCache.computeIfAbsent(timeKey + "_content", k -> ConcurrentHashMap.newKeySet());

        return contentHashes.contains(contentHash);
    }

    private void cacheNews(News news) {
        String url = news.getNewsMeta().getSourceUrl();
        urlCache.put(url, news);

        String title = news.getOriginalContent().getTitle();
        if (title != null && !title.trim().isEmpty()) {
            String titleHash = generateTitleHash(title);
            String timeKey = getTimeWindowKey(news.getScrapedTime());
            titleHashCache.computeIfAbsent(timeKey, k -> ConcurrentHashMap.newKeySet()).add(titleHash);
        }
    }

    private String generateTitleHash(String title) {
        return String.valueOf(title.toLowerCase().trim().hashCode());
    }

    private String generateContentHash(String content) {
        return String.valueOf(content.toLowerCase().trim().hashCode());
    }

    private String getTimeWindowKey(LocalDateTime dateTime) {
        LocalDateTime windowStart = dateTime.truncatedTo(ChronoUnit.HOURS)
                .minusHours(dateTime.getHour() % (timeWindowHours / 24));
        return windowStart.toString();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("duplicatesFound", duplicatesFound.get());
        stats.put("duplicateRate", totalProcessed.get() > 0 ?
                (double) duplicatesFound.get() / totalProcessed.get() * 100 : 0);
        stats.put("cacheSize", urlCache.size());
        return stats;
    }

    public void clearCache() {
        urlCache.clear();
        titleHashCache.clear();
        log.info("중복 제거 캐시가 초기화되었습니다");
    }
}