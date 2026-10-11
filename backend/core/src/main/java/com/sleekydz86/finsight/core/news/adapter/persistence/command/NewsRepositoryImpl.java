package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.NewsSourceUrls;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class NewsRepositoryImpl implements NewsPersistencePort {

    private static final Logger log = LoggerFactory.getLogger(NewsRepositoryImpl.class);

    private final NewsJpaMapper newsJpaMapper;
    private final NewsJpaRepository newsJpaRepository;

    public NewsRepositoryImpl(NewsJpaMapper newsJpaMapper, NewsJpaRepository newsJpaRepository) {
        this.newsJpaMapper = newsJpaMapper;
        this.newsJpaRepository = newsJpaRepository;
    }

    @Override
    public Newses saveAllNews(List<News> newses) {
        List<News> fresh = selectFresh(newses);
        if (fresh.isEmpty()) {
            return new Newses(List.of());
        }
        List<NewsJpaEntity> savedEntities = newsJpaRepository.saveAll(fresh.stream()
                .map(newsJpaMapper::toEntity)
                .toList());
        return new Newses(savedEntities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    private List<News> selectFresh(List<News> newses) {
        if (newses == null || newses.isEmpty()) {
            return List.of();
        }
        Set<String> urls = new HashSet<>();
        for (News news : newses) {
            if (news == null || news.getNewsMeta() == null) {
                continue;
            }
            String url = NewsSourceUrls.normalize(news.getNewsMeta().getSourceUrl());
            if (!url.isBlank()) {
                urls.add(url);
            }
        }
        Set<String> stored = urls.isEmpty()
                ? Set.of()
                : new HashSet<>(newsJpaRepository.findSourceUrlBySourceUrlIn(urls));
        List<News> fresh = NewsSourceUrls.withoutStoredDuplicates(newses, stored);
        int skipped = newses.size() - fresh.size();
        if (skipped > 0) {
            log.info("중복 뉴스 URL {}건을 건너뜁니다", skipped);
        }
        return fresh;
    }

    @Override
    public Newses findByOverviewIsNull() {
        List<NewsJpaEntity> entities = newsJpaRepository.findByOverviewIsNull();
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Newses findAllByFilters(NewsQueryRequest request) {

        List<NewsJpaEntity> entities = newsJpaRepository.findAll();
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Optional<News> findById(Long newsId) {
        return newsJpaRepository.findById(newsId)
                .map(newsJpaMapper::toDomain);
    }

    @Override
    public Newses searchByQuery(NewsSearchRequest request) {
        List<NewsJpaEntity> entities = newsJpaRepository.findAll();
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Newses findByCategory(String category, int limit) {
        List<NewsJpaEntity> entities = newsJpaRepository.findAll();
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .limit(limit)
                .toList());
    }

    @Override
    public Newses findPopularNews(int limit) {
        int safeLimit = Math.max(1, limit);
        return new Newses(newsJpaRepository.findPopularNews(PageRequest.of(0, safeLimit)).stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Newses findLatestNews(int limit) {
        int safeLimit = Math.max(1, limit);
        return new Newses(newsJpaRepository.findLatestNews(PageRequest.of(0, safeLimit)).stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Newses findLatestNewsByProvider(com.sleekydz86.finsight.core.global.NewsProvider provider, int limit) {
        int safeLimit = Math.max(1, limit);
        return new Newses(newsJpaRepository.findLatestByProvider(provider, PageRequest.of(0, safeLimit)).stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    @Override
    public Newses findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new Newses();
        }
        Map<Long, News> byId = new HashMap<>();
        for (NewsJpaEntity entity : newsJpaRepository.findAllById(ids)) {
            byId.put(entity.getId(), newsJpaMapper.toDomain(entity));
        }
        List<News> ordered = new ArrayList<>();
        for (Long id : ids) {
            News news = byId.get(id);
            if (news != null) {
                ordered.add(news);
            }
        }
        return new Newses(ordered);
    }

    @Override
    public Newses findRelatedNews(Long newsId, List<String> categories, int limit) {
        List<NewsJpaEntity> entities = newsJpaRepository.findAll();
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .limit(limit)
                .toList());
    }
}