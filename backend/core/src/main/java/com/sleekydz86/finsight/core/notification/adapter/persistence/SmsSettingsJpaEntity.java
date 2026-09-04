package com.sleekydz86.finsight.core.notification.adapter.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsSettingsJpaEntity {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "news_alert_enabled", nullable = false)
    private boolean newsAlertEnabled;

    @Column(name = "otp_enabled", nullable = false)
    private boolean otpEnabled;

    @Column(name = "account_recovery_enabled", nullable = false)
    private boolean accountRecoveryEnabled;

    @Column(name = "system_alert_enabled", nullable = false)
    private boolean systemAlertEnabled;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "default_message_type", nullable = false, length = 16)
    private String defaultMessageType = "SMS";

    @Column(name = "default_from_number", length = 32)
    private String defaultFromNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public SmsSettingsJpaEntity(
            Long id,
            boolean enabled,
            boolean newsAlertEnabled,
            boolean otpEnabled,
            boolean accountRecoveryEnabled,
            boolean systemAlertEnabled,
            boolean notificationEnabled,
            String defaultMessageType,
            String defaultFromNumber,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id != null ? id : 1L;
        this.enabled = enabled;
        this.newsAlertEnabled = newsAlertEnabled;
        this.otpEnabled = otpEnabled;
        this.accountRecoveryEnabled = accountRecoveryEnabled;
        this.systemAlertEnabled = systemAlertEnabled;
        this.notificationEnabled = notificationEnabled;
        this.defaultMessageType = defaultMessageType != null ? defaultMessageType : "SMS";
        this.defaultFromNumber = defaultFromNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SmsSettingsJpaEntity defaults() {
        LocalDateTime now = LocalDateTime.now();
        return SmsSettingsJpaEntity.builder()
                .id(1L)
                .enabled(false)
                .newsAlertEnabled(false)
                .otpEnabled(true)
                .accountRecoveryEnabled(true)
                .systemAlertEnabled(false)
                .notificationEnabled(false)
                .defaultMessageType("SMS")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void apply(
            boolean enabled,
            boolean newsAlertEnabled,
            boolean otpEnabled,
            boolean accountRecoveryEnabled,
            boolean systemAlertEnabled,
            boolean notificationEnabled,
            String defaultMessageType,
            String defaultFromNumber) {
        this.enabled = enabled;
        this.newsAlertEnabled = newsAlertEnabled;
        this.otpEnabled = otpEnabled;
        this.accountRecoveryEnabled = accountRecoveryEnabled;
        this.systemAlertEnabled = systemAlertEnabled;
        this.notificationEnabled = notificationEnabled;
        this.defaultMessageType = defaultMessageType != null && !defaultMessageType.isBlank()
                ? defaultMessageType.trim().toUpperCase()
                : "SMS";
        this.defaultFromNumber = blankToNull(defaultFromNumber);
        this.updatedAt = LocalDateTime.now();
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
        if (id == null) {
            id = 1L;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
