package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_send_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsSendLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SmsPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 16)
    private MessageType messageType;

    @Column(name = "to_phone", nullable = false, length = 32)
    private String toPhone;

    @Column(name = "from_phone", length = 32)
    private String fromPhone;

    @Column(name = "content_preview", length = 500)
    private String contentPreview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SmsSendStatus status;

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public SmsSendLogJpaEntity(
            SmsPurpose purpose,
            MessageType messageType,
            String toPhone,
            String fromPhone,
            String contentPreview,
            SmsSendStatus status,
            String externalMessageId,
            String errorMessage,
            Long recipientUserId,
            Long actorUserId,
            LocalDateTime createdAt) {
        this.purpose = purpose;
        this.messageType = messageType;
        this.toPhone = toPhone;
        this.fromPhone = fromPhone;
        this.contentPreview = contentPreview;
        this.status = status;
        this.externalMessageId = externalMessageId;
        this.errorMessage = errorMessage;
        this.recipientUserId = recipientUserId;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
