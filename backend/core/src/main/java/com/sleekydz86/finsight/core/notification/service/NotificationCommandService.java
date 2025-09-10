package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationCommandUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationPersistencePort;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationPersistencePort notificationPersistencePort;
    private final NotificationSenderPort notificationSenderPort;

    @Override
    @LogExecution("알림 생성")
    @PerformanceMonitor(threshold = 1000, operation = "create_notification")
    public Notification createNotification(NotificationTemplate template, User user) {
        log.info("알림 생성 시작 - 사용자: {}, 타입: {}, 채널: {}",
                user.getEmail(), template.getType(), template.getChannel());

        Notification notification = Notification.builder()
                .user(user)
                .type(template.getType())
                .title(template.getTitle())
                .content(template.getContent())
                .status(com.sleekydz86.finsight.core.notification.domain.NotificationStatus.PENDING)
                .channel(template.getChannel())
                .metadata(template.getVariables())
                .build();

        return notificationPersistencePort.save(notification);
    }

    @Override
    @LogExecution("알림 발송")
    @PerformanceMonitor(threshold = 2000, operation = "send_notification")
    public void sendNotification(Notification notification) {
        log.info("알림 발송 시작 - ID: {}, 사용자: {}, 채널: {}",
                notification.getId(), notification.getUser().getEmail(), notification.getChannel());

        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    notificationSenderPort.sendEmailNotification(notification.getUser(), notification);
                    break;
                case SMS:
                    notificationSenderPort.sendSmsNotification(notification.getUser(), notification);
                    break;
                case PUSH:
                    notificationSenderPort.sendPushNotification(notification.getUser(), notification);
                    break;
                case KAKAO:
                    notificationSenderPort.sendKakaoTalkNotification(notification.getUser(), notification);
                    break;
                case SLACK:
                    notificationSenderPort.sendSlackNotification(notification.getUser(), notification);
                    break;
                case WEBHOOK:
                    notificationSenderPort.sendWebhookNotification(notification.getUser(), notification);
                    break;
                default:
                    throw new IllegalArgumentException("지원하지 않는 알림 채널: " + notification.getChannel());
            }

            notification.markAsSent();
            notificationPersistencePort.save(notification);
            log.info("알림 발송 성공 - ID: {}", notification.getId());

        } catch (Exception e) {
            log.error("알림 발송 실패 - ID: {}, 오류: {}", notification.getId(), e.getMessage(), e);
            notification.markAsFailed(e.getMessage());
            notificationPersistencePort.save(notification);
            throw e;
        }
    }

    @Override
    @LogExecution("대량 알림 발송")
    @PerformanceMonitor(threshold = 5000, operation = "send_bulk_notifications")
    public void sendBulkNotifications(List<Notification> notifications) {
        log.info("대량 알림 발송 시작 - 건수: {}", notifications.size());

        List<CompletableFuture<Void>> futures = notifications.stream()
                .map(notification -> CompletableFuture.runAsync(() -> {
                    try {
                        sendNotification(notification);
                    } catch (Exception e) {
                        log.error("대량 알림 발송 중 오류 - ID: {}, 오류: {}",
                                notification.getId(), e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("대량 알림 발송 완료 - 건수: {}", notifications.size());
    }

    @Override
    @LogExecution("예약 알림 등록")
    @PerformanceMonitor(threshold = 1000, operation = "schedule_notification")
    public void scheduleNotification(Notification notification) {
        log.info("예약 알림 등록 - ID: {}, 예약시간: {}",
                notification.getId(), notification.getScheduledAt());

        notification.markAsPending();
        notificationPersistencePort.save(notification);
    }

    @Override
    @LogExecution("알림 취소")
    @PerformanceMonitor(threshold = 500, operation = "cancel_notification")
    public void cancelNotification(Long notificationId) {
        log.info("알림 취소 - ID: {}", notificationId);

        Notification notification = notificationPersistencePort.findById(notificationId);
        if (notification != null) {
            notification.setStatus(com.sleekydz86.finsight.core.notification.domain.NotificationStatus.CANCELLED);
            notificationPersistencePort.save(notification);
        }
    }

    @Override
    @LogExecution("실패 알림 재시도")
    @PerformanceMonitor(threshold = 2000, operation = "retry_failed_notification")
    public void retryFailedNotification(Long notificationId) {
        log.info("실패 알림 재시도 - ID: {}", notificationId);

        Notification notification = notificationPersistencePort.findById(notificationId);
        if (notification != null && notification.getStatus() ==
                com.sleekydz86.finsight.core.notification.domain.NotificationStatus.FAILED) {
            notification.markAsPending();
            notificationPersistencePort.save(notification);
            sendNotification(notification);
        }
    }

    @Override
    @LogExecution("뉴스 알림 발송")
    @PerformanceMonitor(threshold = 3000, operation = "send_news_alert")
    public void sendNewsAlert(News news, List<User> users) {
        log.info("뉴스 알림 발송 시작 - 뉴스: {}, 사용자 수: {}",
                news.getOriginalContent().getTitle(), users.size());

        List<Notification> notifications = users.stream()
                .filter(user -> user.getNotificationPreferences().contains(
                        com.sleekydz86.finsight.core.user.domain.NotificationType.NEWS_ALERT))
                .map(user -> {
                    NotificationTemplate template = NotificationTemplate.createNewsAlertTemplate(
                            news, user, user.getPreferredNotificationChannel());
                    return createNotification(template, user);
                })
                .collect(Collectors.toList());

        sendBulkNotifications(notifications);
    }

    @Override
    @LogExecution("시스템 알림 발송")
    @PerformanceMonitor(threshold = 2000, operation = "send_system_alert")
    public void sendSystemAlert(String title, String content, List<User> users) {
        log.info("시스템 알림 발송 시작 - 제목: {}, 사용자 수: {}", title, users.size());

        List<Notification> notifications = users.stream()
                .filter(user -> user.getNotificationPreferences().contains(
                        com.sleekydz86.finsight.core.user.domain.NotificationType.SYSTEM_ALERT))
                .map(user -> {
                    NotificationTemplate template = NotificationTemplate.createSystemAlertTemplate(
                            title, content, user.getPreferredNotificationChannel());
                    return createNotification(template, user);
                })
                .collect(Collectors.toList());

        sendBulkNotifications(notifications);
    }
}