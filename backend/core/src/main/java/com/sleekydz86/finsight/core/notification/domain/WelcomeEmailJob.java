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
@Table(name = "welcome_email_jobs")
public class WelcomeEmailJob {

    public enum Status {
        PENDING,
        SENT,
        EXPIRED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
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
}
