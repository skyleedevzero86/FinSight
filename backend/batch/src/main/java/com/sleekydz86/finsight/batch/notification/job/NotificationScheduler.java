package com.sleekydz86.finsight.batch.notification.job;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationCommandUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationQueryUseCase notificationQueryUseCase;
    private final NotificationCommandUseCase notificationCommandUseCase;

    @Scheduled(fixedDelay = 30000)
    public void processScheduledNotifications() {
        log.debug("예약된 알림 처리 시작");

        try {
            List<Notification> scheduledNotifications = notificationQueryUseCase
                    .getScheduledNotifications(LocalDateTime.now());

            if (!scheduledNotifications.isEmpty()) {
                log.info("예약된 알림 {}건 처리 시작", scheduledNotifications.size());
                notificationCommandUseCase.sendBulkNotifications(scheduledNotifications);
                log.info("예약된 알림 {}건 처리 완료", scheduledNotifications.size());
            }

        } catch (Exception e) {
            log.error("예약된 알림 처리 중 오류 발생", e);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void retryFailedNotifications() {
        log.debug("실패한 알림 재시도 시작");

        try {
            List<Notification> failedNotifications = notificationQueryUseCase.getFailedNotifications();

            if (!failedNotifications.isEmpty()) {
                log.info("실패한 알림 {}건 재시도 시작", failedNotifications.size());

                for (Notification notification : failedNotifications) {
                    try {
                        notificationCommandUseCase.retryFailedNotification(notification.getId());
                    } catch (Exception e) {
                        log.error("알림 재시도 실패 - ID: {}, 오류: {}",
                                notification.getId(), e.getMessage());
                    }
                }

                log.info("실패한 알림 {}건 재시도 완료", failedNotifications.size());
            }

        } catch (Exception e) {
            log.error("실패한 알림 재시도 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupOldNotifications() {
        log.debug("오래된 알림 정리 시작");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            List<Notification> oldNotifications = notificationQueryUseCase
                    .getNotificationsByDateRange(LocalDateTime.now().minusDays(90), cutoffDate);

            if (!oldNotifications.isEmpty()) {
                log.info("오래된 알림 {}건 정리 시작", oldNotifications.size());
                // 실제 구현에서는 삭제 로직 추가할예정
                log.info("오래된 알림 {}건 정리 완료", oldNotifications.size());
            }

        } catch (Exception e) {
            log.error("오래된 알림 정리 중 오류 발생", e);
        }
    }
}


