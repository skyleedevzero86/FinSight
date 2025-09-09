package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsJpaRepository extends JpaRepository<NewsJpaEntity, Long> {

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.overview IS NULL")
    List<NewsJpaEntity> findByOverviewIsNull();

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.newsPublishedTime >= :startDate AND n.newsPublishedTime <= :endDate")
    List<NewsJpaEntity> findByPublishedTimeBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.sentimentType = :sentimentType")
    List<NewsJpaEntity> findBySentimentType(@Param("sentimentType") String sentimentType);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.originalTitle LIKE %:keyword% OR n.originalContent LIKE %:keyword%")
    List<NewsJpaEntity> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.targetCategories LIKE %:category%")
    List<NewsJpaEntity> findByCategory(@Param("category") String category);

    @Query("SELECT n FROM NewsJpaEntity n ORDER BY n.viewCount DESC")
    List<NewsJpaEntity> findPopularNews();

    @Query("SELECT n FROM NewsJpaEntity n ORDER BY n.newsPublishedTime DESC")
    List<NewsJpaEntity> findLatestNews();

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.id != :newsId AND n.targetCategories LIKE %:category%")
    List<NewsJpaEntity> findRelatedNews(@Param("newsId") Long newsId, @Param("category") String category);
}