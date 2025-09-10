package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.notification.push.fcm.server-key:}")
    private String fcmServerKey;

    @Value("${app.notification.push.fcm.url:https://fcm.googleapis.com/fcm/send}")
    private String fcmUrl;

    public void sendNewsAlert(User user, News news) {
        try {
            String deviceToken = user.getDeviceToken();
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("사용자 디바이스 토큰이 없어 푸시 알림 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            Map<String, Object> payload = createNewsAlertPayload(news);
            sendPushNotification(deviceToken, payload);

            log.info("뉴스 알림 푸시 발송 성공 - 사용자: {}, 뉴스: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

        } catch (Exception e) {
            log.error("뉴스 알림 푸시 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("푸시 알림 발송 실패", e);
        }
    }

    public void sendNotification(User user, Notification notification) {
        try {
            String deviceToken = user.getDeviceToken();
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("사용자 디바이스 토큰이 없어 푸시 알림 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            Map<String, Object> payload = createNotificationPayload(notification);
            sendPushNotification(deviceToken, payload);

            log.info("알림 푸시 발송 성공 - 사용자: {}, 알림: {}",
                    user.getEmail(), notification.getTitle());

        } catch (Exception e) {
            log.error("알림 푸시 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("푸시 알림 발송 실패", e);
        }
    }

    private void sendPushNotification(String deviceToken, Map<String, Object> payload) {
        Map<String, Object> fcmMessage = new HashMap<>();
        fcmMessage.put("to", deviceToken);
        fcmMessage.put("data", payload);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "key=" + fcmServerKey);
        headers.put("Content-Type", "application/json");

        log.debug("FCM 푸시 알림 발송 - 토큰: {}, 페이로드: {}", deviceToken, payload);
    }

    private Map<String, Object> createNewsAlertPayload(News news) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "[FinSight] 중요 뉴스 알림");
        payload.put("body", news.getOriginalContent().getTitle());
        payload.put("newsId", news.getId());
        payload.put("url", news.getNewsMeta().getSourceUrl());
        payload.put("type", "news_alert");
        return payload;
    }

    private Map<String, Object> createNotificationPayload(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", notification.getTitle());
        payload.put("body", notification.getContent());
        payload.put("notificationId", notification.getId());
        payload.put("type", notification.getType().name().toLowerCase());
        return payload;
    }
}