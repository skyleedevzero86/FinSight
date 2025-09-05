package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsStatisticsJpaRepository extends JpaRepository<NewsStatisticsJpaEntity, Long> {

    Optional<NewsStatisticsJpaEntity> findByNewsId(Long newsId);

    @Modifying
    @Query("UPDATE NewsStatisticsJpaEntity ns SET ns.viewCount = ns.viewCount + 1 WHERE ns.newsId = :newsId")
    void incrementViewCount(@Param("newsId") Long newsId);

    @Modifying
    @Query("UPDATE NewsStatisticsJpaEntity ns SET ns.likeCount = ns.likeCount + 1 WHERE ns.newsId = :newsId")
    void incrementLikeCount(@Param("newsId") Long newsId);

    @Modifying
    @Query("UPDATE NewsStatisticsJpaEntity ns SET ns.dislikeCount = ns.dislikeCount + 1 WHERE ns.newsId = :newsId")
    void incrementDislikeCount(@Param("newsId") Long newsId);

    @Modifying
    @Query("UPDATE NewsStatisticsJpaEntity ns SET ns.commentCount = :commentCount WHERE ns.newsId = :newsId")
    void updateCommentCount(@Param("newsId") Long newsId, @Param("commentCount") int commentCount);

    void deleteByNewsId(Long newsId);
}