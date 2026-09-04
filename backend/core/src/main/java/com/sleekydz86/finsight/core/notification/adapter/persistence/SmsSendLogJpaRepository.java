package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SmsSendLogJpaRepository extends JpaRepository<SmsSendLogJpaEntity, Long> {

    Page<SmsSendLogJpaEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<SmsSendLogJpaEntity> findByStatusOrderByCreatedAtDescIdDesc(SmsSendStatus status, Pageable pageable);

    Page<SmsSendLogJpaEntity> findByPurposeOrderByCreatedAtDescIdDesc(SmsPurpose purpose, Pageable pageable);

    Page<SmsSendLogJpaEntity> findByStatusAndPurposeOrderByCreatedAtDescIdDesc(
            SmsSendStatus status, SmsPurpose purpose, Pageable pageable);

    long countByStatus(SmsSendStatus status);

    long countByPurpose(SmsPurpose purpose);

    long countByStatusAndCreatedAtAfter(SmsSendStatus status, LocalDateTime after);

    @Query("""
            SELECT FUNCTION('DATE', l.createdAt), l.status, COUNT(l)
            FROM SmsSendLogJpaEntity l
            WHERE l.createdAt >= :from
            GROUP BY FUNCTION('DATE', l.createdAt), l.status
            ORDER BY FUNCTION('DATE', l.createdAt)
            """)
    List<Object[]> countDailyStatusSince(@Param("from") LocalDateTime from);
}
