package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.dto.KakaoMessageRequest;
import com.sleekydz86.finsight.core.notification.domain.dto.KakaoMessageResponse;
import com.sleekydz86.finsight.core.notification.domain.dto.KakaoTokenResponse;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoTalkNotificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserPersistencePort userPersistencePort;

    @Value("${app.notification.kakao.api-key:}")
    private String kakaoApiKey;

    @Value("${app.notification.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${app.notification.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${app.notification.kakao.api-url:https://kapi.kakao.com}")
    private String kakaoApiUrl;

    @Value("${app.notification.kakao.auth-url:https://kauth.kakao.com}")
    private String kakaoAuthUrl;

    @Value("${app.notification.kakao.enabled:true}")
    private boolean kakaoNotificationEnabled;

    @Value("${app.name:FinSight}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.notification.kakao.retry-attempts:3}")
    private int retryAttempts;

    @Value("${app.notification.kakao.retry-delay:1000}")
    private long retryDelay;

    @Async("notificationExecutor")
    public CompletableFuture<KakaoMessageResponse> sendNewsAlert(User user, News news) {
        if (!kakaoNotificationEnabled) {
            log.debug("카카오톡 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림이 비활성화되어 있습니다.")
            );
        }

        if (!user.canReceiveKakaoNotification()) {
            log.warn("사용자 카카오톡 알림 조건 미충족 - 사용자: {}, 카카오ID존재: {}, 설정활성화: {}, 토큰유효: {}",
                    user.getEmail(),
                    user.getKakaoUserId() != null,
                    user.getKakaoNotificationEnabled(),
                    user.isKakaoTokenValid());
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림을 받을 수 없는 상태입니다.")
            );
        }

        try {
            if (!ensureValidToken(user)) {
                return CompletableFuture.completedFuture(
                        KakaoMessageResponse.failed("카카오톡 토큰을 갱신할 수 없습니다.")
                );
            }

            Map<String, Object> templateObject = createNewsAlertTemplate(news);
            KakaoMessageRequest request = KakaoMessageRequest.builder()
                    .receiverUuids(Arrays.asList(user.getKakaoUserId()))
                    .templateObject(templateObject)
                    .build();

            KakaoMessageResponse response = sendKakaoMessage(user, request);

            if (response.isSuccess()) {
                log.info("카카오톡 뉴스 알림 발송 성공 - 사용자: {}, 뉴스: {}, 메시지ID: {}",
                        user.getEmail(), news.getOriginalContent().getTitle(), response.getSuccessfulReceiver());
            } else {
                log.error("카카오톡 뉴스 알림 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), response.getErrorMessage());
            }

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("카카오톡 뉴스 알림 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림 발송 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    @Async("notificationExecutor")
    public CompletableFuture<KakaoMessageResponse> sendSystemNotification(User user, Notification notification) {
        if (!kakaoNotificationEnabled) {
            log.debug("카카오톡 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림이 비활성화되어 있습니다.")
            );
        }

        if (!user.canReceiveKakaoNotification()) {
            log.warn("사용자 카카오톡 알림 조건 미충족 - 사용자: {}", user.getEmail());
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림을 받을 수 없는 상태입니다.")
            );
        }

        try {
            if (!ensureValidToken(user)) {
                return CompletableFuture.completedFuture(
                        KakaoMessageResponse.failed("카카오톡 토큰을 갱신할 수 없습니다.")
                );
            }

            Map<String, Object> templateObject = createSystemNotificationTemplate(notification);
            KakaoMessageRequest request = KakaoMessageRequest.builder()
                    .receiverUuids(Arrays.asList(user.getKakaoUserId()))
                    .templateObject(templateObject)
                    .build();

            KakaoMessageResponse response = sendKakaoMessage(user, request);

            if (response.isSuccess()) {
                log.info("카카오톡 시스템 알림 발송 성공 - 사용자: {}, 알림: {}, 메시지ID: {}",
                        user.getEmail(), notification.getTitle(), response.getSuccessfulReceiver());
            } else {
                log.error("카카오톡 시스템 알림 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), response.getErrorMessage());
            }

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("카카오톡 시스템 알림 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림 발송 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    @Async("notificationExecutor")
    public CompletableFuture<List<KakaoMessageResponse>> sendBulkNotifications(
            List<User> users, String title, String content) {

        if (!kakaoNotificationEnabled) {
            log.debug("카카오톡 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        List<KakaoMessageResponse> responses = new ArrayList<>();
        List<User> eligibleUsers = users.stream()
                .filter(User::canReceiveKakaoNotification)
                .toList();

        if (eligibleUsers.isEmpty()) {
            log.warn("카카오톡 알림을 받을 수 있는 사용자가 없습니다. 총 사용자 수: {}", users.size());
            return CompletableFuture.completedFuture(responses);
        }

        for (User user : eligibleUsers) {
            try {
                if (!ensureValidToken(user)) {
                    responses.add(KakaoMessageResponse.failed("토큰 갱신 실패: " + user.getEmail()));
                    continue;
                }

                Map<String, Object> templateObject = createTextTemplate(title, content);
                KakaoMessageRequest request = KakaoMessageRequest.builder()
                        .receiverUuids(Arrays.asList(user.getKakaoUserId()))
                        .templateObject(templateObject)
                        .build();

                KakaoMessageResponse response = sendKakaoMessage(user, request);
                responses.add(response);

                if (response.isSuccess()) {
                    log.info("카카오톡 대량 알림 발송 성공 - 사용자: {}", user.getEmail());
                } else {
                    log.error("카카오톡 대량 알림 발송 실패 - 사용자: {}, 오류: {}",
                            user.getEmail(), response.getErrorMessage());
                }

            } catch (Exception e) {
                log.error("카카오톡 대량 알림 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                        user.getEmail(), e.getMessage(), e);
                responses.add(KakaoMessageResponse.failed("발송 중 오류: " + e.getMessage()));
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("카카오톡 대량 알림 발송 완료 - 전체 사용자: {}, 처리된 사용자: {}, 성공: {}",
                users.size(), eligibleUsers.size(),
                responses.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum());

        return CompletableFuture.completedFuture(responses);
    }

    public CompletableFuture<KakaoMessageResponse> sendCustomMessage(
            User user, String title, String content, String linkUrl) {

        if (!user.canReceiveKakaoNotification()) {
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 알림을 받을 수 없는 상태입니다.")
            );
        }

        try {
            if (!ensureValidToken(user)) {
                return CompletableFuture.completedFuture(
                        KakaoMessageResponse.failed("카카오톡 토큰을 갱신할 수 없습니다.")
                );
            }

            Map<String, Object> templateObject = createCustomTemplate(title, content, linkUrl);
            KakaoMessageRequest request = KakaoMessageRequest.builder()
                    .receiverUuids(Arrays.asList(user.getKakaoUserId()))
                    .templateObject(templateObject)
                    .build();

            KakaoMessageResponse response = sendKakaoMessage(user, request);
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("카카오톡 커스텀 메시지 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    KakaoMessageResponse.failed("카카오톡 메시지 발송 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    private KakaoMessageResponse sendKakaoMessage(User user, KakaoMessageRequest request) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                return sendKakaoMessageInternal(user, request);
            } catch (Exception e) {
                lastException = e;
                log.warn("카카오톡 메시지 발송 실패 - 시도 {}/{}, 오류: {}", attempt, retryAttempts, e.getMessage());

                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(retryDelay * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return KakaoMessageResponse.failed("최대 재시도 횟수 초과: " +
                (lastException != null ? lastException.getMessage() : "알 수 없는 오류"));
    }

    private KakaoMessageResponse sendKakaoMessageInternal(User user, KakaoMessageRequest request) {
        if (user.getKakaoAccessToken() == null || user.getKakaoAccessToken().trim().isEmpty()) {
            throw new IllegalStateException("카카오 액세스 토큰이 없습니다.");
        }

        try {
            String url = kakaoApiUrl + "/v1/api/talk/friends/message/default/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Bearer " + user.getKakaoAccessToken());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("receiver_uuids", objectMapper.writeValueAsString(request.getReceiverUuids()));
            body.add("template_object", objectMapper.writeValueAsString(request.getTemplateObject()));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<String> successfulReceiver = (List<String>) responseBody.get("successful_receiver_uuids");

                if (successfulReceiver != null && !successfulReceiver.isEmpty()) {
                    return KakaoMessageResponse.success(successfulReceiver);
                } else {
                    List<Map<String, Object>> failureInfo = (List<Map<String, Object>>) responseBody.get("failure_info");
                    String errorMessage = failureInfo != null && !failureInfo.isEmpty()
                            ? failureInfo.get(0).get("msg").toString()
                            : "알 수 없는 오류";
                    return KakaoMessageResponse.failed("카카오 API 오류: " + errorMessage);
                }
            } else {
                return KakaoMessageResponse.failed("HTTP 오류: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                if (refreshToken(user)) {
                    return sendKakaoMessageInternal(user, request);
                }
            }
            log.error("카카오 API 클라이언트 오류: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return KakaoMessageResponse.failed("클라이언트 오류: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("카카오 API 서버 오류: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("카카오 API 서버 오류", e);
        } catch (ResourceAccessException e) {
            log.error("카카오 API 연결 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 API 연결 오류", e);
        } catch (Exception e) {
            log.error("카카오톡 메시지 발송 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new RuntimeException("카카오톡 메시지 발송 오류", e);
        }
    }

    private boolean ensureValidToken(User user) {
        if (user.isKakaoTokenValid()) {
            return true;
        }

        log.info("카카오 토큰이 만료되었거나 없습니다. 토큰 갱신 시도 - 사용자: {}", user.getEmail());
        return refreshToken(user);
    }

    private boolean refreshToken(User user) {
        if (user.getKakaoRefreshToken() == null || user.getKakaoRefreshToken().trim().isEmpty()) {
            log.error("카카오 리프레시 토큰이 없어 토큰 갱신 불가 - 사용자: {}", user.getEmail());
            return false;
        }

        try {
            String url = kakaoAuthUrl + "/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", kakaoClientId);
            body.add("client_secret", kakaoClientSecret);
            body.add("refresh_token", user.getKakaoRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, KakaoTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoTokenResponse tokenResponse = response.getBody();

                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
                String newRefreshToken = tokenResponse.getRefreshToken() != null
                        ? tokenResponse.getRefreshToken()
                        : user.getKakaoRefreshToken();

                user.updateKakaoInfo(
                        user.getKakaoUserId(),
                        tokenResponse.getAccessToken(),
                        expiresAt,
                        newRefreshToken
                );

                userPersistencePort.save(user);
                log.info("카카오 토큰 갱신 성공 - 사용자: {}", user.getEmail());
                return true;
            } else {
                log.error("카카오 토큰 갱신 실패 - HTTP 오류: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("카카오 토큰 갱신 중 오류 발생 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    private Map<String, Object> createNewsAlertTemplate(News news) {
        Map<String, Object> template = new HashMap<>();
        template.put("object_type", "feed");

        Map<String, Object> content = new HashMap<>();
        content.put("title", String.format("[%s] 중요 뉴스 알림", appName));
        content.put("description", news.getOriginalContent().getTitle());
        content.put("image_url", "https://via.placeholder.com/200x200/4A90E2/FFFFFF?text=NEWS");

        Map<String, String> link = new HashMap<>();
        link.put("web_url", frontendUrl + "/news/" + news.getId());
        link.put("mobile_web_url", frontendUrl + "/news/" + news.getId());
        content.put("link", link);

        template.put("content", content);

        List<Map<String, Object>> buttons = new ArrayList<>();
        Map<String, Object> button = new HashMap<>();
        button.put("title", "자세히 보기");
        button.put("link", link);
        buttons.add(button);
        template.put("buttons", buttons);

        return template;
    }

    private Map<String, Object> createSystemNotificationTemplate(Notification notification) {
        Map<String, Object> template = new HashMap<>();
        template.put("object_type", "text");

        String text = String.format("[%s] %s\n\n%s\n\n발송시간: %s",
                appName,
                notification.getTitle(),
                notification.getContent(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        template.put("text", text);

        Map<String, String> link = new HashMap<>();
        link.put("web_url", frontendUrl + "/notifications");
        link.put("mobile_web_url", frontendUrl + "/notifications");
        template.put("link", link);

        return template;
    }

    private Map<String, Object> createTextTemplate(String title, String content) {
        Map<String, Object> template = new HashMap<>();
        template.put("object_type", "text");

        String text = String.format("[%s] %s\n\n%s",
                appName, title, content);
        template.put("text", text);

        Map<String, String> link = new HashMap<>();
        link.put("web_url", frontendUrl);
        link.put("mobile_web_url", frontendUrl);
        template.put("link", link);

        return template;
    }

    private Map<String, Object> createCustomTemplate(String title, String content, String linkUrl) {
        Map<String, Object> template = new HashMap<>();
        template.put("object_type", "feed");

        Map<String, Object> contentObj = new HashMap<>();
        contentObj.put("title", String.format("[%s] %s", appName, title));
        contentObj.put("description", content);
        contentObj.put("image_url", "https://via.placeholder.com/200x200/4A90E2/FFFFFF?text=" + appName);

        Map<String, String> link = new HashMap<>();
        link.put("web_url", linkUrl != null ? linkUrl : frontendUrl);
        link.put("mobile_web_url", linkUrl != null ? linkUrl : frontendUrl);
        contentObj.put("link", link);

        template.put("content", contentObj);

        return template;
    }

    public boolean isServiceHealthy() {
        return kakaoNotificationEnabled &&
                kakaoApiKey != null && !kakaoApiKey.trim().isEmpty() &&
                kakaoClientId != null && !kakaoClientId.trim().isEmpty() &&
                kakaoClientSecret != null && !kakaoClientSecret.trim().isEmpty();
    }

    public void logServiceStats() {
        log.info("카카오톡 알림 서비스 상태 - 활성화: {}, API URL: {}, 재시도 횟수: {}",
                kakaoNotificationEnabled, kakaoApiUrl, retryAttempts);
    }

    public int getActiveUsersCount() {
        return userPersistencePort.findAll().stream()
                .mapToInt(user -> user.canReceiveKakaoNotification() ? 1 : 0)
                .sum();
    }
}