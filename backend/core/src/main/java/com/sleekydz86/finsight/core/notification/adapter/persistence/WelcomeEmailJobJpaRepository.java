package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WelcomeEmailJobJpaRepository extends JpaRepository<WelcomeEmailJobJpaEntity, Long> {

    Optional<WelcomeEmailJobJpaEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
            SELECT j FROM WelcomeEmailJobJpaEntity j
            WHERE j.status = :status
              AND j.scheduledAt <= :now
              AND j.deadlineAt >= :now
            ORDER BY j.scheduledAt ASC
            """)
    List<WelcomeEmailJobJpaEntity> findDuePending(
            @Param("status") Status status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT j FROM WelcomeEmailJobJpaEntity j
            WHERE j.status = :status
              AND j.deadlineAt < :now
            """)
    List<WelcomeEmailJobJpaEntity> findExpiredPending(
            @Param("status") Status status,
            @Param("now") LocalDateTime now);
}
