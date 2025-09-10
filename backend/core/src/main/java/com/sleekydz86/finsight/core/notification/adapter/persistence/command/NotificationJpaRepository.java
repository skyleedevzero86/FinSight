package com.sleekydz86.finsight.core.notification.adapter.persistence.command;

import com.sleekydz86.finsight.core.notification.domain.NotificationChannel;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<NotificationJpaEntity> findByStatus(NotificationStatus status);

    List<NotificationJpaEntity> findByScheduledAtBeforeAndStatus(LocalDateTime dateTime, NotificationStatus status);

    List<NotificationJpaEntity> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime dateTime);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.userId = :userId AND n.createdAt BETWEEN :start AND :end ORDER BY n.createdAt DESC")
    List<NotificationJpaEntity> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.status = 'FAILED' AND n.createdAt > :since")
    List<NotificationJpaEntity> findFailedNotificationsSince(@Param("since") LocalDateTime since);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.channel = :channel AND n.status = 'PENDING'")
    List<NotificationJpaEntity> findByChannelAndPendingStatus(@Param("channel") NotificationChannel channel);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.type = :type AND n.status = 'SENT' AND n.sentAt BETWEEN :start AND :end")
    List<NotificationJpaEntity> findByTypeAndSentDateRange(@Param("type") NotificationType type,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);
}