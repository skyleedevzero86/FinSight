package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.news.domain.NewsStatistics;

import java.util.Optional;

public interface NewsStatisticsPersistencePort {
    NewsStatistics save(NewsStatistics statistics);
    Optional<NewsStatistics> findByNewsId(Long newsId);
    NewsStatistics incrementViewCount(Long newsId);
    NewsStatistics incrementLikeCount(Long newsId);
    NewsStatistics incrementDislikeCount(Long newsId);
    NewsStatistics updateCommentCount(Long newsId, int commentCount);
    void deleteByNewsId(Long newsId);
}