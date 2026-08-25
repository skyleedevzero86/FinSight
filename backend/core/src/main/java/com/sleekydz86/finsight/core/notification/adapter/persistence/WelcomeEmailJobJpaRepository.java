package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob;
import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WelcomeEmailJobJpaRepository extends JpaRepository<WelcomeEmailJob, Long> {

    Optional<WelcomeEmailJob> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
            SELECT j FROM WelcomeEmailJob j
            WHERE j.status = :status
              AND j.scheduledAt <= :now
              AND j.deadlineAt >= :now
            ORDER BY j.scheduledAt ASC
            """)
    List<WelcomeEmailJob> findDuePending(
            @Param("status") Status status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT j FROM WelcomeEmailJob j
            WHERE j.status = :status
              AND j.deadlineAt < :now
            """)
    List<WelcomeEmailJob> findExpiredPending(
            @Param("status") Status status,
            @Param("now") LocalDateTime now);
}
