package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailActorType;
import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EmailLogJpaRepository extends JpaRepository<EmailLog, Long> {

    @Query("""
            SELECT e FROM EmailLog e
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(e.recipient) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(e.requestIp, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(e.requestLocation, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(e.relatedRef, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR e.status = :status)
              AND (:purpose IS NULL OR e.purpose = :purpose)
              AND (:actorType IS NULL OR e.actorType = :actorType)
              AND (:requestIp IS NULL OR :requestIp = '' OR e.requestIp = :requestIp)
              AND (:from IS NULL OR e.createdAt >= :from)
              AND (:to IS NULL OR e.createdAt <= :to)
            """)
    Page<EmailLog> search(
            @Param("keyword") String keyword,
            @Param("status") EmailStatus status,
            @Param("purpose") EmailMailPurpose purpose,
            @Param("actorType") EmailActorType actorType,
            @Param("requestIp") String requestIp,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
