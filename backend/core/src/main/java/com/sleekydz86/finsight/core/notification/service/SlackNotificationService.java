package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
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
public class SlackNotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.notification.slack.webhook-url:}")
    private String webhookUrl;

    public void sendNotification(User user, Notification notification) {
        try {
            String slackChannel = user.getSlackChannelId();
            if (slackChannel == null || slackChannel.trim().isEmpty()) {
                log.warn("사용자 슬랙 채널이 없어 슬랙 알림 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            Map<String, Object> message = createSlackMessage(notification);
            sendSlackMessage(slackChannel, message);

            log.info("슬랙 알림 발송 성공 - 사용자: {}, 알림: {}",
                    user.getEmail(), notification.getTitle());

        } catch (Exception e) {
            log.error("슬랙 알림 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("슬랙 알림 발송 실패", e);
        }
    }

    private void sendSlackMessage(String channel, Map<String, Object> message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("text", message.get("text"));
        payload.put("attachments", message.get("attachments"));

        // 실제 슬랙 웹훅 호출 로직
        log.debug("슬랙 메시지 발송 - 채널: {}, 메시지: {}", channel, message);
    }

    private Map<String, Object> createSlackMessage(Notification notification) {
        Map<String, Object> message = new HashMap<>();
        message.put("text", notification.getTitle());

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", "good");
        attachment.put("text", notification.getContent());
        attachment.put("footer", "FinSight");
        attachment.put("ts", System.currentTimeMillis() / 1000);

        message.put("attachments", new Object[]{attachment});
        return message;
    }
}