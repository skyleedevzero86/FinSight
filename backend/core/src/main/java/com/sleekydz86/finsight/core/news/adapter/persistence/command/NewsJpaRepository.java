package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.EmbeddingStatus;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentStatus;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.overview IS NULL")
    Page<NewsJpaEntity> findByOverviewIsNull(Pageable pageable);

    @Query("""
            SELECT n.id FROM NewsJpaEntity n
            WHERE n.sentimentStatus IS NULL OR n.sentimentStatus = :pending
            ORDER BY n.id ASC
            """)
    List<Long> findSentimentPendingIds(@Param("pending") SentimentStatus pending, Pageable pageable);

    @Query("""
            SELECT n.id FROM NewsJpaEntity n
            WHERE n.sentimentStatus IN :statuses AND n.sentimentAttempts < :maxAttempts
            ORDER BY n.id ASC
            """)
    List<Long> findSentimentRetryIds(@Param("statuses") List<SentimentStatus> statuses,
                                     @Param("maxAttempts") int maxAttempts,
                                     Pageable pageable);

    @Query("""
            SELECT n.id FROM NewsJpaEntity n
            WHERE n.embeddingStatus IS NULL OR n.embeddingStatus = :pending
            ORDER BY n.id ASC
            """)
    List<Long> findEmbeddingPendingIds(@Param("pending") EmbeddingStatus pending, Pageable pageable);

    @Query("""
            SELECT n.id FROM NewsJpaEntity n
            WHERE n.embeddingStatus = :status AND n.embeddingAttempts < :maxAttempts
            ORDER BY n.id ASC
            """)
    List<Long> findEmbeddingRetryIds(@Param("status") EmbeddingStatus status,
                                     @Param("maxAttempts") int maxAttempts,
                                     Pageable pageable);

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

    @Query("SELECT n FROM NewsJpaEntity n ORDER BY n.viewCount DESC")
    Page<NewsJpaEntity> findPopularNews(Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.newsProvider = :provider AND n.originalTitle NOT LIKE '[더미]%' AND (n.translatedTitle IS NULL OR n.translatedTitle NOT LIKE '[더미]%') ORDER BY n.newsPublishedTime DESC")
    Page<NewsJpaEntity> findLatestByProvider(@Param("provider") com.sleekydz86.finsight.core.global.NewsProvider provider, Pageable pageable);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.originalTitle NOT LIKE '[더미]%' AND (n.translatedTitle IS NULL OR n.translatedTitle NOT LIKE '[더미]%') ORDER BY n.newsPublishedTime DESC")
    List<NewsJpaEntity> findLatestNews();

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.originalTitle NOT LIKE '[더미]%' AND (n.translatedTitle IS NULL OR n.translatedTitle NOT LIKE '[더미]%') ORDER BY n.newsPublishedTime DESC")
    Page<NewsJpaEntity> findLatestNews(Pageable pageable);

    @Query("""
            SELECT n FROM NewsJpaEntity n
            WHERE n.originalTitle NOT LIKE '[더미]%'
              AND (n.translatedTitle IS NULL OR n.translatedTitle NOT LIKE '[더미]%')
              AND (
                LOWER(COALESCE(n.translatedTitle, n.originalTitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(n.translatedContent, n.originalContent, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY n.newsPublishedTime DESC
            """)
    Page<NewsJpaEntity> searchCatalog(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM NewsJpaEntity n WHERE n.id != :newsId AND n.targetCategories LIKE %:category%")
    List<NewsJpaEntity> findRelatedNews(@Param("newsId") Long newsId, @Param("category") String category);

    @Query(value = """
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM news
            WHERE created_at >= :from AND created_at < :to
            GROUP BY DATE(created_at)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> countCreatedByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}