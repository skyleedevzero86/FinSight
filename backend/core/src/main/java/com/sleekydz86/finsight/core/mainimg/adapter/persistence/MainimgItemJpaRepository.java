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
            ORDER BY m.id DESC
            """)
    Page<MainimgItemJpaEntity> searchReflectOnly(@Param("domainId") String domainId, Pageable pageable);

    @Query("""
            SELECT m FROM MainimgItemJpaEntity m
            WHERE (:domainId IS NULL OR m.domainId = :domainId)
            ORDER BY m.id DESC
            """)
    Page<MainimgItemJpaEntity> searchAll(@Param("domainId") String domainId, Pageable pageable);
}
