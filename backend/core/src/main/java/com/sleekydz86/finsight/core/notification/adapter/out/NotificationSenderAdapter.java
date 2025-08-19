package com.sleekydz86.finsight.core.notification.adapter.out;

import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderAdapter.class);

    @Override
    public void sendEmailNotification(User user, News news) {
        log.info("이메일 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        // TODO: 실제 이메일 전송 로직 구현
    }

    @Override
    public void sendPushNotification(User user, News news) {
        log.info("푸시 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        // TODO: 실제 푸시 알림 전송 로직 구현
    }

    @Override
    public void sendSmsNotification(User user, News news) {
        log.info("SMS 알림 전송 - 사용자: {}, 뉴스 제목: {}",
                user.getEmail(), news.getOriginalContent().getTitle());
        // TODO: 실제 SMS 전송 로직 구현
    }
}