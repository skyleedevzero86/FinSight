package com.sleekydz86.finsight.core.notification.domain.port.out;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationPersistencePort {
    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    Notification findById(Long id);

    Page<Notification> findByUser(User user, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByScheduledAtBefore(LocalDateTime dateTime);

    List<Notification> findByFailedStatus();

    long countByUser(User user);

    List<Notification> findByDateRange(LocalDateTime start, LocalDateTime end);

    void deleteById(Long id);
}