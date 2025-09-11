package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookNotificationService {

    private final RestTemplate restTemplate;

    public void sendNotification(User user, Notification notification) {
        try {
            if (!user.canReceiveWebhookNotification()) {
                log.warn("사용자가 웹훅 알림을 받을 수 없는 상태 - 사용자: {}", user.getEmail());
                return;
            }

            String webhookUrl = user.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                log.warn("사용자 웹훅 URL이 없어 웹훅 알림 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            Map<String, Object> payload = createWebhookPayload(user, notification);
            boolean success = sendWebhookNotification(webhookUrl, payload);

            if (success) {
                log.info("웹훅 알림 발송 성공 - 사용자: {}, 알림: {}, URL: {}",
                        user.getEmail(), notification.getTitle(), webhookUrl);
            } else {
                log.error("웹훅 알림 발송 실패 - 사용자: {}, 알림: {}, URL: {}",
                        user.getEmail(), notification.getTitle(), webhookUrl);
            }

        } catch (Exception e) {
            log.error("웹훅 알림 발송 중 오류 발생 - 사용자: {}, 알림: {}, 오류: {}",
                    user.getEmail(), notification.getTitle(), e.getMessage(), e);
            throw new RuntimeException("웹훅 알림 발송 실패", e);
        }
    }

    private boolean sendWebhookNotification(String webhookUrl, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.debug("웹훅 알림 발송 완료 - URL: {}, 페이로드: {}", webhookUrl, payload);
            return true;

        } catch (Exception e) {
            log.error("웹훅 호출 실패 - URL: {}, 오류: {}", webhookUrl, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> createWebhookPayload(User user, Notification notification) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("notificationId", notification.getId());
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getTitle());
        notificationData.put("content", notification.getContent());
        notificationData.put("type", notification.getType() != null ? notification.getType().name() : null);
        notificationData.put("channel", notification.getChannel() != null ? notification.getChannel().name() : null);
        notificationData.put("priority", notification.getPriority() != null ? notification.getPriority().name() : null);
        notificationData.put("createdAt", notification.getCreatedAt());
        notificationData.put("sentAt", notification.getSentAt());

        payload.put("notification", notificationData);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "FinSight");
        metadata.put("version", "1.0");
        metadata.put("timestamp", System.currentTimeMillis());

        payload.put("metadata", metadata);

        return payload;
    }
}