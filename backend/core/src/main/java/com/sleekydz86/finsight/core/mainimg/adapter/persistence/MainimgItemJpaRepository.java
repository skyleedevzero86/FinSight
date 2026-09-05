package com.sleekydz86.finsight.core.mainimg.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MainimgItemJpaRepository extends JpaRepository<MainimgItemJpaEntity, String> {

    @Query("""
            SELECT m FROM MainimgItemJpaEntity m
            WHERE (:domainId IS NULL OR m.domainId = :domainId)
            AND m.reflectYn = 'Y'
            AND (m.noticeBegin IS NULL OR m.noticeBegin = '' OR m.noticeBegin <= :today)
            AND (m.noticeEnd IS NULL OR m.noticeEnd = '' OR m.noticeEnd >= :today)
            ORDER BY m.sortOrder ASC, m.id ASC
            """)
    Page<MainimgItemJpaEntity> searchReflectOnly(
            @Param("domainId") String domainId,
            @Param("today") String today,
            Pageable pageable);

    @Query("""
            SELECT m FROM MainimgItemJpaEntity m
            WHERE (:domainId IS NULL OR m.domainId = :domainId)
            ORDER BY m.sortOrder ASC, m.id ASC
            """)
    Page<MainimgItemJpaEntity> searchAll(@Param("domainId") String domainId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(m.sortOrder), 0) FROM MainimgItemJpaEntity m")
    int findMaxSortOrder();
}
