package com.sleekydz86.finsight.core.notification.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationPriority;
import com.sleekydz86.finsight.core.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationJpaMapper {

    public NotificationJpaEntity toEntity(Notification notification) {
        Long userId = notification.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("알림 저장에는 userId가 필요합니다");
        }
        return NotificationJpaEntity.builder()
                .userId(userId)
                .newsId(notification.getNewsId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .status(notification.getStatus())
                .channel(notification.getChannel())
                .priority(notification.getPriority() != null
                        ? notification.getPriority()
                        : NotificationPriority.NORMAL)
                .externalId(notification.getExternalId())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .failureReason(notification.getFailureReason())
                .metadata(notification.getMetadata())
                .build();
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        return toDomainWithReferences(entity, null, null);
    }

    public Notification toDomainWithReferences(NotificationJpaEntity entity, User user, NewsJpaEntity news) {
        Notification notification = Notification.builder()
                .id(entity.getId())
                .user(user)
                .news(news)
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .channel(entity.getChannel())
                .priority(entity.getPriority() != null ? entity.getPriority() : NotificationPriority.NORMAL)
                .externalId(entity.getExternalId())
                .scheduledAt(entity.getScheduledAt())
                .sentAt(entity.getSentAt())
                .failureReason(entity.getFailureReason())
                .metadata(entity.getMetadata())
                .build();
        if (entity.getCreatedAt() != null) {
            notification.setCreatedAt(entity.getCreatedAt());
        }
        if (entity.getUpdatedAt() != null) {
            notification.setUpdatedAt(entity.getUpdatedAt());
        }
        return notification;
    }

    public void updateEntity(NotificationJpaEntity entity, Notification notification) {
        entity.updateStatus(notification.getStatus());
        if (notification.getSentAt() != null) {
            entity.markAsSent();
        }
        if (notification.getFailureReason() != null) {
            entity.markAsFailed(notification.getFailureReason());
        }
    }
}
