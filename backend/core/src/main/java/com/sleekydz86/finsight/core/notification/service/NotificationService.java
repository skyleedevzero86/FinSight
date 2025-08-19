package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final UserPersistencePort userPersistencePort;
    private final NotificationSenderPort notificationSenderPort;

    public NotificationService(UserPersistencePort userPersistencePort,
            NotificationSenderPort notificationSenderPort) {
        this.userPersistencePort = userPersistencePort;
        this.notificationSenderPort = notificationSenderPort;
    }

    public void notifyUsersAboutNews(News news) {
        List<TargetCategory> newsCategories = news.getAiOverView().getTargetCategories();
        List<User> interestedUsers = userPersistencePort.findByWatchlistCategories(newsCategories);

        for (User user : interestedUsers) {
            if (user.getNotificationPreferences().contains(NotificationType.EMAIL)) {
                notificationSenderPort.sendEmailNotification(user, news);
            }
            if (user.getNotificationPreferences().contains(NotificationType.PUSH)) {
                notificationSenderPort.sendPushNotification(user, news);
            }
        }
    }
}