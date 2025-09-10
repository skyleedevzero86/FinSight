package com.sleekydz86.finsight.core.notification.domain.port.in;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationTemplate;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;

import java.util.List;

public interface NotificationCommandUseCase {
    Notification createNotification(NotificationTemplate template, User user);

    void sendNotification(Notification notification);

    void sendBulkNotifications(List<Notification> notifications);

    void scheduleNotification(Notification notification);

    void cancelNotification(Long notificationId);

    void retryFailedNotification(Long notificationId);

    void sendNewsAlert(News news, List<User> users);

    void sendSystemAlert(String title, String content, List<User> users);
}