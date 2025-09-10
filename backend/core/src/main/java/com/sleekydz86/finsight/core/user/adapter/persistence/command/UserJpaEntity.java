package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.BaseTimeEntity;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_api_key", columnList = "apiKey")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserJpaEntity extends BaseTimeEntity {

    @Column(unique = true, length = 50, nullable = false)
    private String username;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 64)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column
    private LocalDateTime accountLockedAt;

    @Column
    private Long approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_change_count")
    @Builder.Default
    private Integer passwordChangeCount = 0;

    @Column(name = "last_password_change_date")
    private LocalDate lastPasswordChangeDate;

    @ElementCollection(targetClass = TargetCategory.class)
    @CollectionTable(name = "user_watchlist", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private List<TargetCategory> watchlist = new ArrayList<>();

    @ElementCollection(targetClass = NotificationType.class)
    @CollectionTable(name = "user_notification_preferences", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @Builder.Default
    private List<NotificationType> notificationPreferences = new ArrayList<>();

    @Column(name = "otp_secret", length = 100)
    private String otpSecret;

    @Column(name = "otp_enabled", nullable = false)
    @Builder.Default
    private Boolean otpEnabled = false;

    @Column(name = "otp_verified", nullable = false)
    @Builder.Default
    private Boolean otpVerified = false;
}