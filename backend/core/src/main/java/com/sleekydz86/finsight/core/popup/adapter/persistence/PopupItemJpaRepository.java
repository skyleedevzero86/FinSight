package com.sleekydz86.finsight.core.popup.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopupItemJpaRepository extends JpaRepository<PopupItemJpaEntity, String> {

    @Query("""
            SELECT p FROM PopupItemJpaEntity p
            WHERE (:domainId IS NULL OR p.domainId = :domainId)
            AND p.noticeActive = 'Y'
            ORDER BY p.createdAt DESC
            """)
    Page<PopupItemJpaEntity> searchActiveOnly(@Param("domainId") String domainId, Pageable pageable);

    @Query("""
            SELECT p FROM PopupItemJpaEntity p
            WHERE (:domainId IS NULL OR p.domainId = :domainId)
            ORDER BY p.createdAt DESC
            """)
    Page<PopupItemJpaEntity> searchAll(@Param("domainId") String domainId, Pageable pageable);
}
