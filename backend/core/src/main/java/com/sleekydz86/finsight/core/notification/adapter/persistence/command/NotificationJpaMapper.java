package com.sleekydz86.finsight.core.notification.adapter.persistence.command;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationJpaMapper {

    public NotificationJpaEntity toEntity(Notification notification) {
        return NotificationJpaEntity.builder()
                .userId(notification.getUser().getId())
                .newsId(notification.getNews() != null ? notification.getNews().getId() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .status(notification.getStatus())
                .channel(notification.getChannel())
                .externalId(notification.getExternalId())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .failureReason(notification.getFailureReason())
                .metadata(notification.getMetadata())
                .build();
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .user(null)
                .news(null)
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .channel(entity.getChannel())
                .externalId(entity.getExternalId())
                .scheduledAt(entity.getScheduledAt())
                .sentAt(entity.getSentAt())
                .failureReason(entity.getFailureReason())
                .metadata(entity.getMetadata())
                .build();
    }

    public Notification toDomainWithReferences(NotificationJpaEntity entity, User user, NewsJpaEntity news) {
        return Notification.builder()
                .id(entity.getId())
                .user(user)
                .news(news)
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .channel(entity.getChannel())
                .externalId(entity.getExternalId())
                .scheduledAt(entity.getScheduledAt())
                .sentAt(entity.getSentAt())
                .failureReason(entity.getFailureReason())
                .metadata(entity.getMetadata())
                .build();
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