package com.sleekydz86.finsight.core.notification.domain.port.in;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationQueryUseCase {
    Page<Notification> getNotificationsByUser(User user, Pageable pageable);

    List<Notification> getNotificationsByStatus(NotificationStatus status);

    List<Notification> getScheduledNotifications(LocalDateTime before);

    List<Notification> getFailedNotifications();

    Notification getNotificationById(Long id);

    long getNotificationCountByUser(User user);

    List<Notification> getNotificationsByDateRange(LocalDateTime start, LocalDateTime end);
}