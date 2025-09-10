package com.sleekydz86.finsight.core.notification.adapter.out;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import com.sleekydz86.finsight.core.notification.service.*;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSenderAdapter implements NotificationSenderPort {

    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final PushNotificationService pushNotificationService;
    private final KakaoTalkNotificationService kakaoTalkNotificationService;
    private final SlackNotificationService slackNotificationService;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    public void sendEmailNotification(User user, News news) {
        log.info("이메일 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        emailNotificationService.sendNewsAlert(user, news);
    }

    @Override
    public void sendEmailNotification(User user, Notification notification) {
        log.info("이메일 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        emailNotificationService.sendNotification(user, notification);
    }

    @Override
    public void sendPushNotification(User user, News news) {
        log.info("푸시 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        pushNotificationService.sendNewsAlert(user, news);
    }

    @Override
    public void sendPushNotification(User user, Notification notification) {
        log.info("푸시 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        pushNotificationService.sendNotification(user, notification);
    }

    @Override
    public void sendSmsNotification(User user, News news) {
        log.info("SMS 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        smsNotificationService.sendNewsAlert(user, news);
    }

    @Override
    public void sendSmsNotification(User user, Notification notification) {
        log.info("SMS 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        smsNotificationService.sendNotification(user, notification);
    }

    @Override
    public void sendKakaoTalkNotification(User user, Notification notification) {
        log.info("카카오톡 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        kakaoTalkNotificationService.sendNotification(user, notification);
    }

    @Override
    public void sendSlackNotification(User user, Notification notification) {
        log.info("슬랙 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        slackNotificationService.sendNotification(user, notification);
    }

    @Override
    public void sendWebhookNotification(User user, Notification notification) {
        log.info("웹훅 알림 전송 - 사용자: {}, 알림 제목: {}",
                user.getEmail(), notification.getTitle());
        webhookNotificationService.sendNotification(user, notification);
    }
}
