package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NewsRepositoryImpl implements NewsPersistencePort {

    private final NewsJpaMapper newsJpaMapper;
    private final NewsJpaRepository newsJpaRepository;

    public NewsRepositoryImpl(NewsJpaMapper newsJpaMapper, NewsJpaRepository newsJpaRepository) {
        this.newsJpaMapper = newsJpaMapper;
        this.newsJpaRepository = newsJpaRepository;
    }

    @Override
    public Newses saveAllNews(List<News> newses) {
        List<NewsJpaEntity> entities = newses.stream()
                .map(newsJpaMapper::toEntity)
                .toList();

        List<NewsJpaEntity> savedEntities = newsJpaRepository.saveAll(entities);

        return new Newses(savedEntities.stream()
                .map(newsJpaMapper::toDomain)
                .toList());
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
}