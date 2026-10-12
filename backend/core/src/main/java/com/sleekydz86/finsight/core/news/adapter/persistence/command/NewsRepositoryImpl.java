package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.NewsSourceUrls;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class NewsRepositoryImpl implements NewsPersistencePort {

    private static final Logger log = LoggerFactory.getLogger(NewsRepositoryImpl.class);

    private final NewsJpaMapper newsJpaMapper;
    private final NewsJpaRepository newsJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final TransactionTemplate freshInsert;

    public NewsRepositoryImpl(NewsJpaMapper newsJpaMapper,
                              NewsJpaRepository newsJpaRepository,
                              JPAQueryFactory queryFactory,
                              PlatformTransactionManager transactionManager) {
        this.newsJpaMapper = newsJpaMapper;
        this.newsJpaRepository = newsJpaRepository;
        this.queryFactory = queryFactory;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.freshInsert = template;
    }

    @Override
    public Newses saveAllNews(List<News> newses) {
        List<News> saved = new ArrayList<>();
        for (News news : selectFresh(newses)) {
            News stored = insertIgnoringDuplicate(news);
            if (stored != null) {
                saved.add(stored);
            }
        }
        return new Newses(saved);
    }

    @Override
    public List<News> omitStoredDuplicates(List<News> newses) {
        return selectFresh(newses);
    }

    private News insertIgnoringDuplicate(News news) {
        try {
            return freshInsert.execute(status -> persist(news));
        } catch (DataIntegrityViolationException exception) {
            log.info("이미 저장된 뉴스 URL이라 건너뜁니다. url={}", sourceUrl(news));
            return null;
        }
    }

    private News persist(News news) {
        NewsJpaEntity entity = newsJpaRepository.saveAndFlush(newsJpaMapper.toEntity(news));
        return newsJpaMapper.toDomain(entity);
    }

    private static String sourceUrl(News news) {
        if (news == null || news.getNewsMeta() == null) {
            return "";
        }
        return news.getNewsMeta().getSourceUrl();
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
                : newsJpaRepository.findSourceUrlBySourceUrlIn(urls).stream()
                        .map(NewsSourceUrls::normalize)
                        .collect(Collectors.toSet());
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
        return fetchAll(NewsListQuery.filters(newsEntity(), request));
    }

    @Override
    public Optional<News> findById(Long newsId) {
        return newsJpaRepository.findById(newsId)
                .map(newsJpaMapper::toDomain);
    }

    @Override
    public Newses searchByQuery(NewsSearchRequest request) {
        return fetch(NewsListQuery.filters(newsEntity(), NewsListQuery.fromSearch(request)), request);
    }

    @Override
    public Newses findByCategory(String category, int limit) {
        TargetCategory parsed = NewsListQuery.category(category);
        if (parsed == null) {
            return new Newses();
        }
        QNewsJpaEntity news = newsEntity();
        return fetch(new BooleanBuilder(news.targetCategories.any().eq(parsed)), limit);
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
        List<TargetCategory> parsed = NewsListQuery.categories(categories);
        if (parsed.isEmpty()) {
            return new Newses();
        }
        QNewsJpaEntity news = newsEntity();
        BooleanBuilder where = new BooleanBuilder(news.targetCategories.any().in(parsed));
        if (newsId != null) {
            where.and(news.id.ne(newsId));
        }
        return fetch(where, limit);
    }

    private Newses fetchAll(BooleanBuilder where) {
        QNewsJpaEntity news = newsEntity();
        List<NewsJpaEntity> entities = queryFactory.selectFrom(news).distinct()
                .where(where)
                .orderBy(news.newsPublishedTime.desc(), news.id.desc())
                .fetch();
        return toNewses(entities);
    }

    private Newses fetch(BooleanBuilder where, NewsSearchRequest page) {
        QNewsJpaEntity news = newsEntity();
        List<NewsJpaEntity> entities = queryFactory.selectFrom(news).distinct()
                .where(where)
                .orderBy(news.newsPublishedTime.desc(), news.id.desc())
                .offset(NewsListQuery.offset(page))
                .limit(NewsListQuery.pageSize(page))
                .fetch();
        return toNewses(entities);
    }

    private Newses fetch(BooleanBuilder where, int limit) {
        QNewsJpaEntity news = newsEntity();
        List<NewsJpaEntity> entities = queryFactory.selectFrom(news).distinct()
                .where(where)
                .orderBy(news.newsPublishedTime.desc(), news.id.desc())
                .limit(Math.max(1, limit))
                .fetch();
        return toNewses(entities);
    }

    private Newses toNewses(List<NewsJpaEntity> entities) {
        return new Newses(entities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
    }

    private static QNewsJpaEntity newsEntity() {
        return QNewsJpaEntity.newsJpaEntity;
    }
}