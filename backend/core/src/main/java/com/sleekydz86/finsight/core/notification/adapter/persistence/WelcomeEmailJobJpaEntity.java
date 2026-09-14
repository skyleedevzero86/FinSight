package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob;
import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob.Status;
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
public class WelcomeEmailJobJpaEntity {

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

    public WelcomeEmailJobJpaEntity() {
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

    public void apply(WelcomeEmailJob job) {
        this.userId = job.getUserId();
        this.registeredAt = job.getRegisteredAt();
        this.deadlineAt = job.getDeadlineAt();
        this.scheduledAt = job.getScheduledAt();
        this.status = job.getStatus();
        this.attemptCount = job.getAttemptCount();
        this.lastError = job.getLastError();
        this.sentAt = job.getSentAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
