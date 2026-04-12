package com.sleekydz86.finsight.core.ulink.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UlinkItemJpaRepository extends JpaRepository<UlinkItemJpaEntity, String> {

    @Query("""
            SELECT u FROM UlinkItemJpaEntity u
            WHERE (:domainId IS NULL OR u.domainId = :domainId)
            AND (:sectionCode IS NULL OR u.sectionCode = :sectionCode)
            ORDER BY u.createdAt DESC
            """)
    Page<UlinkItemJpaEntity> search(
            @Param("domainId") String domainId,
            @Param("sectionCode") String sectionCode,
            Pageable pageable);
}
