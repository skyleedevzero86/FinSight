package com.sleekydz86.finsight.core.notification.domain;

import com.sleekydz86.finsight.core.global.BaseTimeEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    private User user;
    private NewsJpaEntity news;
    private NotificationType type;
    private String title;
    private String content;
    private NotificationStatus status;
    private NotificationChannel channel;
    private NotificationPriority priority = NotificationPriority.NORMAL;
    private String externalId;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String failureReason;
    private Map<String, String> metadata;

    @Builder
    public Notification(Long id, User user, NewsJpaEntity news, NotificationType type, String title, String content,
            NotificationStatus status, NotificationChannel channel, NotificationPriority priority, String externalId,
            LocalDateTime scheduledAt, LocalDateTime sentAt, String failureReason, Map<String, String> metadata) {
        if (id != null) {
            setId(id);
        }
        this.user = user;
        this.news = news;
        this.type = type;
        this.title = title;
        this.content = content;
        this.status = status;
        this.channel = channel;
        this.priority = priority != null ? priority : NotificationPriority.NORMAL;
        this.externalId = externalId;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        this.failureReason = failureReason;
        this.metadata = metadata;
    }

    public static Notification create(User user, NewsJpaEntity news, NotificationType type, String title,
            String content, NotificationStatus status, NotificationChannel channel, NotificationPriority priority,
            String externalId, LocalDateTime scheduledAt, Map<String, String> metadata) {
        return Notification.builder()
                .user(user)
                .news(news)
                .type(type)
                .title(title)
                .content(content)
                .status(status)
                .channel(channel)
                .priority(priority)
                .externalId(externalId)
                .scheduledAt(scheduledAt)
                .metadata(metadata)
                .build();
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getNewsId() {
        return news != null ? news.getId() : null;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsPending() {
        this.status = NotificationStatus.PENDING;
    }

    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean canBeSent() {
        return status == NotificationStatus.PENDING
                && (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now()));
    }

    public void updatePriority(NotificationPriority priority) {
        this.priority = priority != null ? priority : NotificationPriority.NORMAL;
    }

    public boolean isHighPriority() {
        return this.priority == NotificationPriority.HIGH || this.priority == NotificationPriority.URGENT;
    }

    public boolean isUrgent() {
        return this.priority == NotificationPriority.URGENT;
    }
}
