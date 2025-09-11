package com.sleekydz86.finsight.core.user.domain;

import com.sleekydz86.finsight.core.global.BaseTimeEntity;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Slf4j
public class User extends BaseTimeEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String nickname;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
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

    @Column(nullable = false)
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column
    private LocalDateTime accountLockedAt;

    @Column
    private Long approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime passwordChangedAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer passwordChangeCount = 0;

    @Column
    private LocalDate lastPasswordChangeDate;

    @ElementCollection(targetClass = TargetCategory.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_watchlist", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private List<TargetCategory> watchlist = new ArrayList<>();

    @ElementCollection(targetClass = NotificationType.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_notification_preferences", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "notification_type")
    @Builder.Default
    private List<NotificationType> notificationPreferences = new ArrayList<>();

    @Column
    private String otpSecret;

    @Column(nullable = false)
    @Builder.Default
    private Boolean otpEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean otpVerified = false;

    @Column(length = 1000)
    private String deviceToken;

    @Column
    private String deviceType;

    @Column
    private LocalDateTime deviceTokenUpdatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushNotificationEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotificationEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsNotificationEnabled = false;

    @Column
    private String phoneNumber;

    @Column
    private String profileImageUrl;

    @Column
    private String timezone;

    @Column
    private String language;

    @Column
    private String kakaoUserId;

    @Column
    private String kakaoAccessToken;

    @Column
    private LocalDateTime kakaoTokenExpiresAt;

    @Column
    private String kakaoRefreshToken;

    @Column(nullable = false)
    @Builder.Default
    private Boolean kakaoNotificationEnabled = false;

    @Column
    private String telegramUserId;

    @Column
    private String telegramChatId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean telegramNotificationEnabled = false;

    @Column
    private String slackUserId;

    @Column
    private String slackChannelId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean slackNotificationEnabled = false;

    @Column
    private String discordUserId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean discordNotificationEnabled = false;

    @Column
    private String lineUserId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lineNotificationEnabled = false;

    @Column(length = 1000)
    private String webhookUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean webhookNotificationEnabled = false;

    public void changePassword(String newPassword) {
        log.info("비밀번호 변경 실행 - userId: {}, 이전 passwordChangedAt: {}",
                this.getId(), this.passwordChangedAt);

        this.password = newPassword;
        this.passwordChangedAt = LocalDateTime.now();

        LocalDate today = LocalDate.now();
        if (this.lastPasswordChangeDate == null || !this.lastPasswordChangeDate.equals(today)) {
            this.passwordChangeCount = 1;
            this.lastPasswordChangeDate = today;
        } else {
            this.passwordChangeCount = (this.passwordChangeCount == null ? 0 : this.passwordChangeCount) + 1;
        }

        log.info("비밀번호 변경 완료 - userId: {}, passwordChangedAt: {}, 오늘 변경 횟수: {}",
                this.getId(), this.passwordChangedAt, this.passwordChangeCount);
    }

    public boolean canChangePassword() {
        LocalDate today = LocalDate.now();
        if (this.lastPasswordChangeDate == null || !this.lastPasswordChangeDate.equals(today)) {
            return true;
        }

        int maxDailyChanges = 3;
        return this.passwordChangeCount == null || this.passwordChangeCount < maxDailyChanges;
    }

    public int getTodayPasswordChangeCount() {
        LocalDate today = LocalDate.now();
        if (this.lastPasswordChangeDate == null || !this.lastPasswordChangeDate.equals(today)) {
            return 0;
        }
        return this.passwordChangeCount == null ? 0 : this.passwordChangeCount;
    }

    public boolean isPasswordChangeRequired() {
        if (this.passwordChangedAt == null) {
            log.debug("비밀번호 변경 필요 - 최초 변경 안함: userId={}", this.getId());
            return true;
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        boolean isRequired = this.passwordChangedAt.isBefore(thirtyDaysAgo);

        log.debug("비밀번호 변경 필요 여부: userId={}, passwordChangedAt={}, required={}",
                this.getId(), this.passwordChangedAt, isRequired);

        return isRequired;
    }

    public boolean isPasswordChangeRecommended() {
        if (this.passwordChangedAt == null) {
            log.debug("비밀번호 변경 권장 - 최초 변경 안함: userId={}", this.getId());
            return true;
        }

        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        boolean isRecommended = this.passwordChangedAt.isBefore(fourteenDaysAgo);

        log.debug("비밀번호 변경 권장 여부: userId={}, passwordChangedAt={}, recommended={}",
                this.getId(), this.passwordChangedAt, isRecommended);

        return isRecommended;
    }

    public String getName() {
        return this.username;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));

        if (this.role == UserRole.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        } else if (this.role == UserRole.MANAGER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    public boolean isActive() {
        boolean isStatusActive = this.status == UserStatus.APPROVED;
        boolean isNotLocked = !isLocked();

        log.debug("회원 활성 상태 확인: userId={}, status={}, isLocked={}, isActive={}",
                this.getId(), this.status, isLocked(), isStatusActive && isNotLocked);

        return isStatusActive && isNotLocked;
    }

    public boolean isLocked() {
        return this.loginFailCount >= 5 || this.status == UserStatus.SUSPENDED;
    }

    public boolean matchPassword(String rawPassword) {
        return true;
    }

    public void increaseLoginFailCount() {
        this.loginFailCount = (this.loginFailCount == null ? 0 : this.loginFailCount) + 1;

        if (this.loginFailCount >= 5) {
            this.status = UserStatus.SUSPENDED;
            this.accountLockedAt = LocalDateTime.now();
            log.warn("계정 잠금 처리: userId={}, loginFailCount={}", this.getId(), this.loginFailCount);
        }

        log.debug("로그인 실패 카운트 증가: userId={}, loginFailCount={}", this.getId(), this.loginFailCount);
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
        this.accountLockedAt = null;
        log.debug("로그인 실패 카운트 초기화: userId={}", this.getId());
    }

    public void updateProfile(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        log.info("프로필 업데이트: userId={}, nickname={}, email={}", this.getId(), nickname, email);
    }

    public void approve(Long approverId) {
        this.status = UserStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.accountLockedAt = null;
        this.loginFailCount = 0;
        log.info("회원 승인: userId={}, approverId={}, approvedAt={}", this.getId(), approverId, this.approvedAt);
    }

    public void reject() {
        this.status = UserStatus.REJECTED;
        this.approvedBy = null;
        this.approvedAt = null;
        log.info("회원 거부: userId={}", this.getId());
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.accountLockedAt = LocalDateTime.now();
        log.info("회원 정지: userId={}, accountLockedAt={}", this.getId(), this.accountLockedAt);
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        log.info("회원 탈퇴: userId={}", this.getId());
    }

    public void changeRole(UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("역할은 null일 수 없습니다.");
        }

        if (this.role == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            throw new IllegalArgumentException("ADMIN 역할에서 다른 역할로 변경할 수 없습니다.");
        }

        this.role = newRole;
    }

    public void unlock() {
        if (this.status == UserStatus.SUSPENDED) {
            this.status = UserStatus.APPROVED;
        }
        this.accountLockedAt = null;
        this.loginFailCount = 0;
        log.info("회원 잠금 해제: userId={}", this.getId());
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        log.debug("마지막 로그인 시간 업데이트: userId={}, lastLoginAt={}", this.getId(), lastLoginAt);
    }

    public String getMaskedEmail() {
        if (this.email == null || !this.email.contains("@")) {
            return this.email;
        }

        String[] parts = this.email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = localPart.charAt(0) + "*";
        } else {
            maskedLocal = localPart.charAt(0) + "*".repeat(localPart.length() - 2)
                    + localPart.charAt(localPart.length() - 1);
        }

        String maskedDomain;
        if (domainPart.length() <= 2) {
            maskedDomain = "*" + domainPart.charAt(domainPart.length() - 1);
        } else {
            maskedDomain = domainPart.charAt(0) + "*".repeat(domainPart.length() - 2)
                    + domainPart.charAt(domainPart.length() - 1);
        }

        return maskedLocal + "@" + maskedDomain;
    }

    public void resetToPending() {
        this.status = UserStatus.PENDING;
        this.approvedBy = null;
        this.approvedAt = null;
        this.accountLockedAt = null;
        this.loginFailCount = 0;
        log.info("회원 상태를 승인 대기로 재설정: userId={}, username={}", this.getId(), this.username);
    }

    public void addToWatchlist(TargetCategory category) {
        if (!this.watchlist.contains(category)) {
            this.watchlist.add(category);
        }
    }

    public void removeFromWatchlist(TargetCategory category) {
        this.watchlist.remove(category);
    }

    public void updateNotificationPreferences(List<NotificationType> preferences) {
        this.notificationPreferences = new ArrayList<>(preferences);
    }

    public void updateWatchlist(List<TargetCategory> watchlist) {
        this.watchlist = new ArrayList<>(watchlist);
    }

    public void updateDeviceToken(String deviceToken, String deviceType) {
        this.deviceToken = deviceToken;
        this.deviceType = deviceType;
        this.deviceTokenUpdatedAt = LocalDateTime.now();
        log.info("디바이스 토큰 업데이트: userId={}, deviceType={}", this.getId(), deviceType);
    }

    public void removeDeviceToken() {
        this.deviceToken = null;
        this.deviceType = null;
        this.deviceTokenUpdatedAt = null;
        log.info("디바이스 토큰 제거: userId={}", this.getId());
    }

    public boolean canReceivePushNotification() {
        return this.pushNotificationEnabled &&
                this.deviceToken != null &&
                !this.deviceToken.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveEmailNotification() {
        return this.emailNotificationEnabled &&
                this.email != null &&
                !this.email.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveSmsNotification() {
        return this.smsNotificationEnabled &&
                this.phoneNumber != null &&
                !this.phoneNumber.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveKakaoNotification() {
        return this.kakaoNotificationEnabled &&
                this.kakaoUserId != null &&
                !this.kakaoUserId.trim().isEmpty() &&
                this.isActive() &&
                isKakaoTokenValid();
    }

    public boolean canReceiveTelegramNotification() {
        return this.telegramNotificationEnabled &&
                this.telegramUserId != null &&
                !this.telegramUserId.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveSlackNotification() {
        return this.slackNotificationEnabled &&
                this.slackUserId != null &&
                !this.slackUserId.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveDiscordNotification() {
        return this.discordNotificationEnabled &&
                this.discordUserId != null &&
                !this.discordUserId.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveLineNotification() {
        return this.lineNotificationEnabled &&
                this.lineUserId != null &&
                !this.lineUserId.trim().isEmpty() &&
                this.isActive();
    }

    public boolean canReceiveWebhookNotification() {
        return this.webhookNotificationEnabled &&
                this.webhookUrl != null &&
                !this.webhookUrl.trim().isEmpty() &&
                this.isActive();
    }

    public void updateNotificationSettings(Boolean pushEnabled, Boolean emailEnabled, Boolean smsEnabled) {
        if (pushEnabled != null) {
            this.pushNotificationEnabled = pushEnabled;
        }
        if (emailEnabled != null) {
            this.emailNotificationEnabled = emailEnabled;
        }
        if (smsEnabled != null) {
            this.smsNotificationEnabled = smsEnabled;
        }
        log.info("알림 설정 업데이트: userId={}, push={}, email={}, sms={}",
                this.getId(), this.pushNotificationEnabled, this.emailNotificationEnabled, this.smsNotificationEnabled);
    }

    public void updateExternalNotificationSettings(Boolean kakaoEnabled, Boolean telegramEnabled,
            Boolean slackEnabled, Boolean discordEnabled,
            Boolean lineEnabled, Boolean webhookEnabled) {
        if (kakaoEnabled != null) {
            this.kakaoNotificationEnabled = kakaoEnabled;
        }
        if (telegramEnabled != null) {
            this.telegramNotificationEnabled = telegramEnabled;
        }
        if (slackEnabled != null) {
            this.slackNotificationEnabled = slackEnabled;
        }
        if (discordEnabled != null) {
            this.discordNotificationEnabled = discordEnabled;
        }
        if (lineEnabled != null) {
            this.lineNotificationEnabled = lineEnabled;
        }
        if (webhookEnabled != null) {
            this.webhookNotificationEnabled = webhookEnabled;
        }
        log.info("외부 알림 설정 업데이트: userId={}, kakao={}, telegram={}, slack={}, discord={}, line={}, webhook={}",
                this.getId(), this.kakaoNotificationEnabled, this.telegramNotificationEnabled,
                this.slackNotificationEnabled, this.discordNotificationEnabled, this.lineNotificationEnabled,
                this.webhookNotificationEnabled);
    }

    public void updateKakaoInfo(String kakaoUserId, String accessToken, LocalDateTime expiresAt, String refreshToken) {
        this.kakaoUserId = kakaoUserId;
        this.kakaoAccessToken = accessToken;
        this.kakaoTokenExpiresAt = expiresAt;
        this.kakaoRefreshToken = refreshToken;
        log.info("카카오 정보 업데이트: userId={}, kakaoUserId={}", this.getId(), kakaoUserId);
    }

    public void updateTelegramInfo(String telegramUserId, String chatId) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = chatId;
        log.info("텔레그램 정보 업데이트: userId={}, telegramUserId={}", this.getId(), telegramUserId);
    }

    public void updateSlackInfo(String slackUserId, String channelId) {
        this.slackUserId = slackUserId;
        this.slackChannelId = channelId;
        log.info("슬랙 정보 업데이트: userId={}, slackUserId={}", this.getId(), slackUserId);
    }

    public void updateDiscordInfo(String discordUserId) {
        this.discordUserId = discordUserId;
        log.info("디스코드 정보 업데이트: userId={}, discordUserId={}", this.getId(), discordUserId);
    }

    public void updateLineInfo(String lineUserId) {
        this.lineUserId = lineUserId;
        log.info("라인 정보 업데이트: userId={}, lineUserId={}", this.getId(), lineUserId);
    }

    public void updateWebhookInfo(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        log.info("웹훅 정보 업데이트: userId={}, webhookUrl={}", this.getId(), webhookUrl);
    }

    public boolean isKakaoTokenValid() {
        return this.kakaoAccessToken != null &&
                this.kakaoTokenExpiresAt != null &&
                LocalDateTime.now().isBefore(this.kakaoTokenExpiresAt);
    }

    public void removeKakaoInfo() {
        this.kakaoUserId = null;
        this.kakaoAccessToken = null;
        this.kakaoTokenExpiresAt = null;
        this.kakaoRefreshToken = null;
        this.kakaoNotificationEnabled = false;
        log.info("카카오 정보 제거: userId={}", this.getId());
    }

    public void removeTelegramInfo() {
        this.telegramUserId = null;
        this.telegramChatId = null;
        this.telegramNotificationEnabled = false;
        log.info("텔레그램 정보 제거: userId={}", this.getId());
    }

    public void removeSlackInfo() {
        this.slackUserId = null;
        this.slackChannelId = null;
        this.slackNotificationEnabled = false;
        log.info("슬랙 정보 제거: userId={}", this.getId());
    }

    public void removeDiscordInfo() {
        this.discordUserId = null;
        this.discordNotificationEnabled = false;
        log.info("디스코드 정보 제거: userId={}", this.getId());
    }

    public void removeLineInfo() {
        this.lineUserId = null;
        this.lineNotificationEnabled = false;
        log.info("라인 정보 제거: userId={}", this.getId());
    }

    public void removeWebhookInfo() {
        this.webhookUrl = null;
        this.webhookNotificationEnabled = false;
        log.info("웹훅 정보 제거: userId={}", this.getId());
    }

    public void updateProfileSettings(String nickname, String phoneNumber, String profileImageUrl, String timezone,
            String language) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (timezone != null) {
            this.timezone = timezone;
        }
        if (language != null) {
            this.language = language;
        }
        log.info("프로필 설정 업데이트: userId={}, nickname={}, timezone={}, language={}",
                this.getId(), this.nickname, this.timezone, this.language);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(getId(), user.getId()) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), email);
    }

    public void enableOtp(String secret) {
        this.otpSecret = secret;
        this.otpEnabled = true;
        this.otpVerified = false;
        log.info("OTP 활성화: userId={}", this.getId());
    }

    public void verifyOtp() {
        if (!this.otpEnabled) {
            throw new IllegalStateException("OTP가 활성화되지 않았습니다.");
        }
        this.otpVerified = true;
        log.info("OTP 검증 완료: userId={}", this.getId());
    }

    public void disableOtp() {
        this.otpSecret = null;
        this.otpEnabled = false;
        this.otpVerified = false;
        log.info("OTP 비활성화: userId={}", this.getId());
    }

    public boolean isOtpRequired() {
        return this.otpEnabled && this.otpVerified;
    }

    public com.sleekydz86.finsight.core.notification.domain.NotificationChannel getPreferredNotificationChannel() {
        if (canReceiveEmailNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.EMAIL;
        } else if (canReceivePushNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.PUSH;
        } else if (canReceiveSmsNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.SMS;
        } else if (canReceiveKakaoNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.KAKAO;
        } else if (canReceiveSlackNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.SLACK;
        } else if (canReceiveWebhookNotification()) {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.WEBHOOK;
        } else {
            return com.sleekydz86.finsight.core.notification.domain.NotificationChannel.EMAIL;
        }
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
        this.passwordChangeCount++;
    }
}