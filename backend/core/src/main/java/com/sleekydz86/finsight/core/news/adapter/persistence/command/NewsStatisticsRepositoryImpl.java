package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NewsStatisticsRepositoryImpl implements NewsStatisticsPersistencePort {

    private final NewsStatisticsJpaRepository newsStatisticsJpaRepository;
    private final NewsStatisticsJpaMapper newsStatisticsJpaMapper;

    public NewsStatisticsRepositoryImpl(NewsStatisticsJpaRepository newsStatisticsJpaRepository,
                                        NewsStatisticsJpaMapper newsStatisticsJpaMapper) {
        this.newsStatisticsJpaRepository = newsStatisticsJpaRepository;
        this.newsStatisticsJpaMapper = newsStatisticsJpaMapper;
    }

    @Override
    public NewsStatistics save(NewsStatistics statistics) {
        NewsStatisticsJpaEntity entity = newsStatisticsJpaMapper.toEntity(statistics);
        NewsStatisticsJpaEntity savedEntity = newsStatisticsJpaRepository.save(entity);
        return newsStatisticsJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<NewsStatistics> findByNewsId(Long newsId) {
        return newsStatisticsJpaRepository.findByNewsId(newsId)
                .map(newsStatisticsJpaMapper::toDomain);
    }

    @Override
    public NewsStatistics incrementViewCount(Long newsId) {
        newsStatisticsJpaRepository.incrementViewCount(newsId);
        return findByNewsId(newsId).orElse(null);
    }

    @Override
    public NewsStatistics incrementLikeCount(Long newsId) {
        newsStatisticsJpaRepository.incrementLikeCount(newsId);
        return findByNewsId(newsId).orElse(null);
    }

    @Override
    public NewsStatistics incrementDislikeCount(Long newsId) {
        newsStatisticsJpaRepository.incrementDislikeCount(newsId);
        return findByNewsId(newsId).orElse(null);
    }

    @Override
    public NewsStatistics updateCommentCount(Long newsId, int commentCount) {
        newsStatisticsJpaRepository.updateCommentCount(newsId, commentCount);
        return findByNewsId(newsId).orElse(null);
    }

    @Override
    public void deleteByNewsId(Long newsId) {
        newsStatisticsJpaRepository.deleteByNewsId(newsId);
    }
}