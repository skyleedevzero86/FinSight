package com.sleekydz86.finsight.core.inbox.adapter.persistence;

import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxNotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InboxCategory category;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "actor_avatar_url", length = 500)
    private String actorAvatarUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "ref_type", length = 32)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public InboxNotificationJpaEntity(
            Long recipientUserId,
            InboxCategory category,
            Long actorUserId,
            String actorName,
            String actorAvatarUrl,
            String title,
            String body,
            String linkUrl,
            String refType,
            Long refId,
            boolean read,
            LocalDateTime readAt,
            boolean deleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.recipientUserId = recipientUserId;
        this.category = category;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorAvatarUrl = actorAvatarUrl;
        this.title = title;
        this.body = body;
        this.linkUrl = linkUrl;
        this.refType = refType;
        this.refId = refId;
        this.read = read;
        this.readAt = readAt;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
    }
}
