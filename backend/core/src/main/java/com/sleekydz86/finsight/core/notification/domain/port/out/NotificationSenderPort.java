package com.sleekydz86.finsight.core.notification.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;

public interface NotificationSenderPort {
    void sendEmailNotification(User user, News news);

    void sendPushNotification(User user, News news);

    void sendSmsNotification(User user, News news);
}