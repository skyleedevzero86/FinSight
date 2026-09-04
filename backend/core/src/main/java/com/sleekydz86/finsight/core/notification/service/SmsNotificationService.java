package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageSendResult;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationService {

    private final SolapiMessageService solapiMessageService;
    private final SmsAdminService smsAdminService;

    @Value("${app.notification.sms.max-length:80}")
    private int maxSmsLength;

    public void sendNewsAlert(User user, News news) {
        sendForUser(user, SmsPurpose.NEWS_ALERT, createNewsAlertMessage(news), MessageType.SMS, null);
    }

    public void sendNotification(User user, Notification notification) {
        sendForUser(user, SmsPurpose.NOTIFICATION, createNotificationMessage(notification), MessageType.SMS, null);
    }

    public void sendLongMessage(User user, String title, String content) {
        sendForUser(user, SmsPurpose.NOTIFICATION, content, MessageType.LMS, title);
    }

    public void sendKakaoAlimtalk(User user, String message, String templateId) {
        if (!smsAdminService.isPurposeEnabled(SmsPurpose.NOTIFICATION)) {
            smsAdminService.recordSkipped(SmsPurpose.NOTIFICATION, user.getPhoneNumber(),
                    "알림톡 설정 비활성", user.getId());
            return;
        }
        String phoneNumber = user.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("사용자 전화번호가 없어 카카오 알림톡 발송 불가 - 사용자: {}", user.getEmail());
            return;
        }
        try {
            MessageSendResult result = solapiMessageService.sendKakaoAlimtalk(phoneNumber, message, templateId, "pfId");
            smsAdminService.recordSend(SmsPurpose.NOTIFICATION, MessageType.KAKAO_ALIMTALK,
                    phoneNumber, smsAdminService.resolveFromNumber(), message, result, user.getId(), null);
        } catch (Exception e) {
            log.error("카카오 알림톡 발송 중 예외 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("카카오 알림톡 발송 실패", e);
        }
    }

    public void sendScheduledMessage(User user, String message, LocalDateTime scheduledDate) {
        sendForUser(user, SmsPurpose.SYSTEM, message, MessageType.SMS, null);
    }

    public void sendAdaptiveMessage(User user, String title, String content) {
        String message = String.format("[FinSight] %s\n%s", title, content);
        MessageType type = message.length() <= maxSmsLength ? MessageType.SMS : MessageType.LMS;
        sendForUser(user, SmsPurpose.NOTIFICATION, type == MessageType.SMS ? message : content, type, title);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendRecoveryOtpSms(String phoneNumber, String otpCode) {
        String message = String.format("[FinSight] 계정 복구 OTP: %s (5분간 유효)", otpCode);
        sendRaw(phoneNumber, message, SmsPurpose.OTP, null);
        return CompletableFuture.completedFuture(null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendPasswordResetConfirmationSms(String phoneNumber) {
        String message = "[FinSight] 비밀번호가 성공적으로 재설정되었습니다. 보안을 위해 로그인 후 비밀번호를 변경해주세요.";
        sendRaw(phoneNumber, message, SmsPurpose.ACCOUNT_RECOVERY, null);
        return CompletableFuture.completedFuture(null);
    }

    public void sendSms(String phoneNumber, String message) {
        sendRaw(phoneNumber, message, SmsPurpose.SYSTEM, null);
    }

    private void sendForUser(User user, SmsPurpose purpose, String message, MessageType type, String subject) {
        if (!smsAdminService.isPurposeEnabled(purpose)) {
            smsAdminService.recordSkipped(purpose, user.getPhoneNumber(),
                    "관리자 SMS 설정으로 비활성", user.getId());
            log.debug("SMS 발송 스킵 - purpose={}, user={}", purpose, user.getEmail());
            return;
        }
        String phoneNumber = user.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("사용자 전화번호가 없어 SMS 발송 불가 - 사용자: {}", user.getEmail());
            return;
        }
        String from = smsAdminService.resolveFromNumber();
        try {
            MessageSendResult result = switch (type) {
                case LMS -> solapiMessageService.sendLms(phoneNumber, message,
                        subject != null ? subject : "FinSight", from);
                case MMS -> solapiMessageService.sendMms(phoneNumber, message,
                        subject != null ? subject : "FinSight", null, from);
                default -> solapiMessageService.sendSms(phoneNumber, message, from);
            };
            smsAdminService.recordSend(purpose, type, phoneNumber, from, message, result, user.getId(), null);
            if (result.isSuccess()) {
                log.info("SMS 발송 성공 - purpose={}, user={}, messageId={}",
                        purpose, user.getEmail(), result.getMessageId());
            } else {
                log.error("SMS 발송 실패 - purpose={}, user={}, error={}",
                        purpose, user.getEmail(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("SMS 발송 예외 - purpose={}, user={}, error={}",
                    purpose, user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    private void sendRaw(String phoneNumber, String message, SmsPurpose purpose, Long userId) {
        if (!smsAdminService.isPurposeEnabled(purpose)) {
            smsAdminService.recordSkipped(purpose, phoneNumber, "관리자 SMS 설정으로 비활성", userId);
            log.debug("SMS 발송 스킵 - purpose={}", purpose);
            return;
        }
        String from = smsAdminService.resolveFromNumber();
        try {
            MessageSendResult result = solapiMessageService.sendSms(phoneNumber, message, from);
            smsAdminService.recordSend(purpose, MessageType.SMS, phoneNumber, from, message, result, userId, null);
            if (result.isSuccess()) {
                log.info("SMS 발송 성공 - purpose={}, phone={}, messageId={}",
                        purpose, maskPhoneNumber(phoneNumber), result.getMessageId());
            } else {
                log.error("SMS 발송 실패 - purpose={}, phone={}, error={}",
                        purpose, maskPhoneNumber(phoneNumber), result.getErrorMessage());
                throw new RuntimeException("SMS 발송 실패: " + result.getErrorMessage());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("SMS 발송 예외 - purpose={}, phone={}, error={}",
                    purpose, maskPhoneNumber(phoneNumber), e.getMessage(), e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    private String createNewsAlertMessage(News news) {
        String title = news.getOriginalContent().getTitle();
        String summary = news.getAiOverView() != null ? news.getAiOverView().getSummary() : "요약 정보 없음";
        String message = String.format("[FinSight] %s\n%s", title, summary);
        if (message.length() > maxSmsLength) {
            message = message.substring(0, maxSmsLength - 3) + "...";
        }
        return message;
    }

    private String createNotificationMessage(Notification notification) {
        String message = String.format("[FinSight] %s\n%s",
                notification.getTitle(), notification.getContent());
        if (message.length() > maxSmsLength) {
            message = message.substring(0, maxSmsLength - 3) + "...";
        }
        return message;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        int length = phoneNumber.length();
        return phoneNumber.substring(0, length - 8) + "****" + phoneNumber.substring(length - 4);
    }
}
