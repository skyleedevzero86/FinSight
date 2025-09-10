package com.sleekydz86.finsight.core.notification.domain;

import com.sleekydz86.finsight.core.global.BaseTimeEntity;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @ElementCollection
    @CollectionTable(name = "notification_metadata", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;

    @Builder
    public Notification(User user, News news, NotificationType type, String title, String content,
                        NotificationStatus status, NotificationChannel channel, String externalId,
                        LocalDateTime scheduledAt, Map<String, String> metadata) {
        this.user = user;
        this.news = news;
        this.type = type;
        this.title = title;
        this.content = content;
        this.status = status;
        this.channel = channel;
        this.externalId = externalId;
        this.scheduledAt = scheduledAt;
        this.metadata = metadata;
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
        return status == NotificationStatus.PENDING &&
                (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now()));
    }
}