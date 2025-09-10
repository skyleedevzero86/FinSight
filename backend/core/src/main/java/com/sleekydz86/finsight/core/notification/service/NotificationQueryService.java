package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationQueryUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationPersistencePort;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements NotificationQueryUseCase {

    private final NotificationPersistencePort notificationPersistencePort;

    @Override
    @LogExecution("사용자 알림 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_notifications_by_user")
    public Page<Notification> getNotificationsByUser(User user, Pageable pageable) {
        log.debug("사용자 알림 조회 - 사용자: {}, 페이지: {}", user.getEmail(), pageable.getPageNumber());
        return notificationPersistencePort.findByUser(user, pageable);
    }

    @Override
    @LogExecution("상태별 알림 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_notifications_by_status")
    public List<Notification> getNotificationsByStatus(NotificationStatus status) {
        log.debug("상태별 알림 조회 - 상태: {}", status);
        return notificationPersistencePort.findByStatus(status);
    }

    @Override
    @LogExecution("예약 알림 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_scheduled_notifications")
    public List<Notification> getScheduledNotifications(LocalDateTime before) {
        log.debug("예약 알림 조회 - 기준시간: {}", before);
        return notificationPersistencePort.findByScheduledAtBefore(before);
    }

    @Override
    @LogExecution("실패 알림 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_failed_notifications")
    public List<Notification> getFailedNotifications() {
        log.debug("실패 알림 조회");
        return notificationPersistencePort.findByFailedStatus();
    }

    @Override
    @LogExecution("알림 상세 조회")
    @PerformanceMonitor(threshold = 500, operation = "get_notification_by_id")
    public Notification getNotificationById(Long id) {
        log.debug("알림 상세 조회 - ID: {}", id);
        return notificationPersistencePort.findById(id);
    }

    @Override
    @LogExecution("사용자 알림 수 조회")
    @PerformanceMonitor(threshold = 500, operation = "get_notification_count_by_user")
    public long getNotificationCountByUser(User user) {
        log.debug("사용자 알림 수 조회 - 사용자: {}", user.getEmail());
        return notificationPersistencePort.countByUser(user);
    }

    @Override
    @LogExecution("기간별 알림 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_notifications_by_date_range")
    public List<Notification> getNotificationsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("기간별 알림 조회 - 시작: {}, 종료: {}", start, end);
        return notificationPersistencePort.findByDateRange(start, end);
    }
}