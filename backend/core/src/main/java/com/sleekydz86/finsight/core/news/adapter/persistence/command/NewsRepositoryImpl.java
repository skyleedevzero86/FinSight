package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());

        List<News> savedNewses = newsJpaRepository.saveAll(entities).stream()
                .map(newsJpaMapper::toDomain)
                .collect(Collectors.toList());

        return new Newses(savedNewses);
    }

    @Override
    public Newses findByOverviewIsNull() {
        List<NewsJpaEntity> entities = newsJpaRepository.findByOverviewIsNull();
        List<News> foundNewses = entities.stream()
                .map(newsJpaMapper::toDomain)
                .collect(Collectors.toList());
        return new Newses(foundNewses);
    }

    @Override
    public Newses findAllByFilters(NewsQueryRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}