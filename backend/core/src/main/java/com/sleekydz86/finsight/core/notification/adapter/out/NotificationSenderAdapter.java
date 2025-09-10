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
        try {
            log.info("이메일 알림 전송 시작 - 사용자: {}, 뉴스 제목: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

            if (!user.canReceiveEmailNotification()) {
                log.warn("사용자가 이메일 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            emailNotificationService.sendNewsAlert(user, news);
            log.info("이메일 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("이메일 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailNotification(User user, Notification notification) {
        try {
            log.info("이메일 시스템 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceiveEmailNotification()) {
                log.warn("사용자가 이메일 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            emailNotificationService.sendSystemNotification(user, notification);
            log.info("이메일 시스템 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("이메일 시스템 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendPushNotification(User user, News news) {
        try {
            log.info("푸시 알림 전송 시작 - 사용자: {}, 뉴스 제목: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

            if (!user.canReceivePushNotification()) {
                log.warn("사용자가 푸시 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            pushNotificationService.sendNewsAlert(user, news);
            log.info("푸시 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("푸시 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendPushNotification(User user, Notification notification) {
        try {
            log.info("푸시 시스템 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceivePushNotification()) {
                log.warn("사용자가 푸시 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            pushNotificationService.sendNotification(user, notification);
            log.info("푸시 시스템 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("푸시 시스템 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsNotification(User user, News news) {
        try {
            log.info("SMS 알림 전송 시작 - 사용자: {}, 뉴스 제목: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

            if (!user.canReceiveSmsNotification()) {
                log.warn("사용자가 SMS 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            smsNotificationService.sendNewsAlert(user, news);
            log.info("SMS 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("SMS 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSmsNotification(User user, Notification notification) {
        try {
            log.info("SMS 시스템 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceiveSmsNotification()) {
                log.warn("사용자가 SMS 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            smsNotificationService.sendNotification(user, notification);
            log.info("SMS 시스템 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("SMS 시스템 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendKakaoTalkNotification(User user, News news) {
        try {
            log.info("카카오톡 알림 전송 시작 - 사용자: {}, 뉴스 제목: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

            if (!user.canReceiveKakaoNotification()) {
                log.warn("사용자가 카카오톡 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            kakaoTalkNotificationService.sendNewsAlert(user, news);
            log.info("카카오톡 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("카카오톡 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendKakaoTalkNotification(User user, Notification notification) {
        try {
            log.info("카카오톡 시스템 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceiveKakaoNotification()) {
                log.warn("사용자가 카카오톡 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            kakaoTalkNotificationService.sendSystemNotification(user, notification);
            log.info("카카오톡 시스템 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("카카오톡 시스템 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSlackNotification(User user, Notification notification) {
        try {
            log.info("슬랙 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceiveSlackNotification()) {
                log.warn("사용자가 슬랙 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            slackNotificationService.sendNotification(user, notification);
            log.info("슬랙 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("슬랙 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendWebhookNotification(User user, Notification notification) {
        try {
            log.info("웹훅 알림 전송 시작 - 사용자: {}, 알림 제목: {}",
                    user.getEmail(), notification.getTitle());

            if (!user.canReceiveWebhookNotification()) {
                log.warn("사용자가 웹훅 알림을 받을 수 없는 상태입니다 - 사용자: {}", user.getEmail());
                return;
            }

            webhookNotificationService.sendNotification(user, notification);
            log.info("웹훅 알림 전송 완료 - 사용자: {}", user.getEmail());

        } catch (Exception e) {
            log.error("웹훅 알림 전송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
        }
    }
}