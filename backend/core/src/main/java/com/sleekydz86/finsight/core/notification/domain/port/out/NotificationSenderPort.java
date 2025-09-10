package com.sleekydz86.finsight.core.notification.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.notification.domain.Notification;

public interface NotificationSenderPort {
    void sendEmailNotification(User user, News news);
    void sendEmailNotification(User user, Notification notification);
    void sendPushNotification(User user, News news);
    void sendPushNotification(User user, Notification notification);
    void sendSmsNotification(User user, News news);
    void sendSmsNotification(User user, Notification notification);
    void sendKakaoTalkNotification(User user, News news);
    void sendKakaoTalkNotification(User user, Notification notification);
    void sendSlackNotification(User user, Notification notification);
    void sendWebhookNotification(User user, Notification notification);
}