package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageSendResult;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationService {

    private final SolapiMessageService solapiMessageService;

    @Value("${app.notification.sms.max-length:80}")
    private int maxSmsLength;

    public void sendNewsAlert(User user, News news) {
        try {
            String message = createNewsAlertMessage(news);
            String phoneNumber = user.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("사용자 전화번호가 없어 SMS 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            MessageSendResult result = solapiMessageService.sendSms(phoneNumber, message);

            if (result.isSuccess()) {
                log.info("뉴스 알림 SMS 발송 성공 - 사용자: {}, 뉴스: {}, 메시지ID: {}",
                        user.getEmail(), news.getOriginalContent().getTitle(), result.getMessageId());
            } else {
                log.error("뉴스 알림 SMS 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("뉴스 알림 SMS 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    public void sendNotification(User user, Notification notification) {
        try {
            String message = createNotificationMessage(notification);
            String phoneNumber = user.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("사용자 전화번호가 없어 SMS 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            MessageSendResult result = solapiMessageService.sendSms(phoneNumber, message);

            if (result.isSuccess()) {
                log.info("알림 SMS 발송 성공 - 사용자: {}, 알림: {}, 메시지ID: {}",
                        user.getEmail(), notification.getTitle(), result.getMessageId());
            } else {
                log.error("알림 SMS 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("알림 SMS 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    public void sendLongMessage(User user, String title, String content) {
        try {
            String phoneNumber = user.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("사용자 전화번호가 없어 LMS 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            MessageSendResult result = solapiMessageService.sendLms(phoneNumber, content, title);

            if (result.isSuccess()) {
                log.info("LMS 발송 성공 - 사용자: {}, 제목: {}, 메시지ID: {}",
                        user.getEmail(), title, result.getMessageId());
            } else {
                log.error("LMS 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("LMS 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("LMS 발송 실패", e);
        }
    }

    public void sendKakaoAlimtalk(User user, String message, String templateId) {
        try {
            String phoneNumber = user.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("사용자 전화번호가 없어 카카오 알림톡 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            MessageSendResult result = solapiMessageService.sendKakaoAlimtalk(phoneNumber, message, templateId, "pfId");

            if (result.isSuccess()) {
                log.info("카카오 알림톡 발송 성공 - 사용자: {}, 메시지ID: {}",
                        user.getEmail(), result.getMessageId());
            } else {
                log.error("카카오 알림톡 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("카카오 알림톡 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("카카오 알림톡 발송 실패", e);
        }
    }

    public void sendScheduledMessage(User user, String message, java.time.LocalDateTime scheduledDate) {
        try {
            String phoneNumber = user.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("사용자 전화번호가 없어 예약 메시지 발송 불가 - 사용자: {}", user.getEmail());
                return;
            }

            MessageSendResult result = solapiMessageService.sendScheduledMessage(phoneNumber, message, scheduledDate);

            if (result.isSuccess()) {
                log.info("예약 메시지 발송 성공 - 사용자: {}, 예약시간: {}, 메시지ID: {}",
                        user.getEmail(), scheduledDate, result.getMessageId());
            } else {
                log.error("예약 메시지 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("예약 메시지 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("예약 메시지 발송 실패", e);
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

    public void sendAdaptiveMessage(User user, String title, String content) {
        String phoneNumber = user.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("사용자 전화번호가 없어 메시지 발송 불가 - 사용자: {}", user.getEmail());
            return;
        }

        String message = String.format("[FinSight] %s\n%s", title, content);

        try {
            MessageSendResult result;

            if (message.length() <= maxSmsLength) {
                result = solapiMessageService.sendSms(phoneNumber, message);
            } else {
                result = solapiMessageService.sendLms(phoneNumber, content, title);
            }

            if (result.isSuccess()) {
                log.info("적응형 메시지 발송 성공 - 사용자: {}, 메시지ID: {}",
                        user.getEmail(), result.getMessageId());
            } else {
                log.error("적응형 메시지 발송 실패 - 사용자: {}, 오류: {}",
                        user.getEmail(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("적응형 메시지 발송 중 예외 발생 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("메시지 발송 실패", e);
        }
    }
}