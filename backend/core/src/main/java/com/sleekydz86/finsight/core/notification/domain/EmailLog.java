package com.sleekydz86.finsight.core.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "template_type", length = 64)
    private String templateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private EmailMailPurpose purpose;

    @Column(name = "purpose_label", nullable = false, length = 100)
    private String purposeLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private EmailActorType actorType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "request_location", length = 255)
    private String requestLocation;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "body_preview", columnDefinition = "TEXT")
    private String bodyPreview;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "related_ref", length = 100)
    private String relatedRef;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    @Column(name = "bounce_reason", length = 500)
    private String bounceReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected EmailLog() {
    }

    EmailLog(
            String recipient,
            String subject,
            String templateType,
            EmailMailPurpose purpose,
            String purposeLabel,
            EmailStatus status,
            String fromAddress,
            Long userId,
            EmailActorType actorType,
            Long actorUserId,
            String requestIp,
            String requestLocation,
            String userAgent,
            String bodyPreview,
            String errorMessage,
            String relatedRef,
            LocalDateTime sentAt,
            LocalDateTime createdAt) {
        this.recipient = recipient;
        this.subject = subject;
        this.templateType = templateType;
        this.purpose = purpose;
        this.purposeLabel = purposeLabel;
        this.status = status;
        this.fromAddress = fromAddress;
        this.userId = userId;
        this.actorType = actorType;
        this.actorUserId = actorUserId;
        this.requestIp = requestIp;
        this.requestLocation = requestLocation;
        this.userAgent = userAgent;
        this.bodyPreview = bodyPreview;
        this.errorMessage = errorMessage;
        this.relatedRef = relatedRef;
        this.sentAt = sentAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (purpose != null && (purposeLabel == null || purposeLabel.isBlank())) {
            purposeLabel = purpose.getLabel();
        }
        if (actorType == null) {
            actorType = EmailActorType.SYSTEM;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplateType() {
        return templateType;
    }

    public EmailMailPurpose getPurpose() {
        return purpose;
    }

    public String getPurposeLabel() {
        return purposeLabel;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public Long getUserId() {
        return userId;
    }

    public EmailActorType getActorType() {
        return actorType;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public String getRequestLocation() {
        return requestLocation;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRelatedRef() {
        return relatedRef;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public LocalDateTime getBouncedAt() {
        return bouncedAt;
    }

    public String getBounceReason() {
        return bounceReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
