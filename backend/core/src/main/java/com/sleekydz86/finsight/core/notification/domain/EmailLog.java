package com.sleekydz86.finsight.core.notification.domain;

import java.time.LocalDateTime;

public class EmailLog {

    private Long id;
    private String recipient;
    private String subject;
    private String templateType;
    private EmailMailPurpose purpose;
    private String purposeLabel;
    private EmailStatus status;
    private String fromAddress;
    private Long userId;
    private EmailActorType actorType;
    private Long actorUserId;
    private String requestIp;
    private String requestLocation;
    private String userAgent;
    private String bodyPreview;
    private String errorMessage;
    private String relatedRef;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private LocalDateTime bouncedAt;
    private String bounceReason;
    private LocalDateTime createdAt;
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

    public static EmailLog restore(
            Long id,
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
            LocalDateTime deliveredAt,
            LocalDateTime openedAt,
            LocalDateTime clickedAt,
            LocalDateTime bouncedAt,
            String bounceReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        EmailLog log = new EmailLog(
                recipient, subject, templateType, purpose, purposeLabel, status, fromAddress, userId,
                actorType, actorUserId, requestIp, requestLocation, userAgent, bodyPreview, errorMessage,
                relatedRef, sentAt, createdAt);
        log.id = id;
        log.deliveredAt = deliveredAt;
        log.openedAt = openedAt;
        log.clickedAt = clickedAt;
        log.bouncedAt = bouncedAt;
        log.bounceReason = bounceReason;
        log.updatedAt = updatedAt;
        return log;
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
