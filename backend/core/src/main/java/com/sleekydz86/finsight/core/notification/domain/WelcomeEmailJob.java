package com.sleekydz86.finsight.core.notification.domain;

import java.time.LocalDateTime;

public class WelcomeEmailJob {

    public enum Status {
        PENDING,
        SENT,
        EXPIRED,
        FAILED
    }

    private Long id;
    private Long userId;
    private LocalDateTime registeredAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime scheduledAt;
    private Status status;
    private int attemptCount;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected WelcomeEmailJob() {
    }

    public WelcomeEmailJob(
            Long userId,
            LocalDateTime registeredAt,
            LocalDateTime deadlineAt,
            LocalDateTime scheduledAt) {
        this.userId = userId;
        this.registeredAt = registeredAt;
        this.deadlineAt = deadlineAt;
        this.scheduledAt = scheduledAt;
        this.status = Status.PENDING;
        this.attemptCount = 0;
    }

    public static WelcomeEmailJob restore(
            Long id,
            Long userId,
            LocalDateTime registeredAt,
            LocalDateTime deadlineAt,
            LocalDateTime scheduledAt,
            Status status,
            int attemptCount,
            String lastError,
            LocalDateTime sentAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        WelcomeEmailJob job = new WelcomeEmailJob(userId, registeredAt, deadlineAt, scheduledAt);
        job.id = id;
        job.status = status;
        job.attemptCount = attemptCount;
        job.lastError = lastError;
        job.sentAt = sentAt;
        job.createdAt = createdAt;
        job.updatedAt = updatedAt;
        return job;
    }

    public void markSent() {
        this.status = Status.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markExpired() {
        this.status = Status.EXPIRED;
    }

    public void markFailed(String error) {
        this.status = Status.FAILED;
        this.lastError = error;
        this.attemptCount += 1;
    }

    public void retryLater(LocalDateTime nextScheduledAt, String error) {
        this.status = Status.PENDING;
        this.scheduledAt = nextScheduledAt;
        this.lastError = error;
        this.attemptCount += 1;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public Status getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
