package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            String webhookUrl = user.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                log.warn("사용자 웹훅 URL이 없어 웹훅 알림 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            Map<String, Object> payload = createWebhookPayload(notification);
            sendWebhookNotification(webhookUrl, payload);

            log.info("웹훅 알림 발송 성공 - 사용자: {}, 알림: {}",
                    user.getEmail(), notification.getTitle());

        } catch (Exception e) {
            log.error("웹훅 알림 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("웹훅 알림 발송 실패", e);
        }
    }

    private void sendWebhookNotification(String webhookUrl, Map<String, Object> payload) {
        // 실제 웹훅 호출 로직
        log.debug("웹훅 알림 발송 - URL: {}, 페이로드: {}", webhookUrl, payload);
    }

    private Map<String, Object> createWebhookPayload(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("title", notification.getTitle());
        payload.put("content", notification.getContent());
        payload.put("type", notification.getType().name());
        payload.put("channel", notification.getChannel().name());
        payload.put("timestamp", notification.getCreatedAt());
        return payload;
    }
}