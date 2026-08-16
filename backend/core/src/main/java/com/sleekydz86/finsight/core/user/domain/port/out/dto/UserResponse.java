package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.AuthProvider;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "사용자 정보 응답 DTO", example = """
        {
          "id": 1,
          "username": "testuser",
          "nickname": "테스트사용자",
          "email": "test@example.com",
          "status": "APPROVED",
          "role": "USER",
          "otpEnabled": false,
          "otpVerified": false,
          "createDate": "2024-01-15T10:30:00",
          "modifyDate": "2024-01-15T10:30:00",
          "lastLoginAt": "2024-01-15T10:30:00",
          "loginFailCount": 0,
          "accountLockedAt": null,
          "approvedBy": 2,
          "approvedAt": "2024-01-15T11:00:00",
          "passwordChangedAt": "2024-01-15T10:30:00",
          "passwordChangeCount": 1,
          "lastPasswordChangeDate": "2024-01-15T10:30:00"
        }
        """)
public class UserResponse {

    @Schema(description = "사용자 고유 ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "사용자명 (로그인 ID)", example = "testuser", maxLength = 50)
    private String username;

    @Schema(description = "닉네임 (화면 표시용)", example = "테스트사용자", maxLength = 50)
    private String nickname;

    @Schema(description = "이메일 주소", example = "test@example.com", maxLength = 100, format = "email")
    private String email;

    @Schema(description = "전화번호")
    private String phoneNumber;

    @Schema(description = "아이디 마스킹 여부")
    private Boolean usernameMasked;

    @Schema(description = "이메일 마스킹 여부")
    private Boolean emailMasked;

    @Schema(description = "전화번호 마스킹 여부")
    private Boolean phoneMasked;

    @Schema(description = "가입 경로", example = "WEB")
    private AuthProvider authProvider;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "관심 타깃 카테고리")
    private java.util.List<TargetCategory> watchlist;

    @Schema(description = "사용자 상태", example = "APPROVED", implementation = UserStatus.class)
    private UserStatus status;

    @Schema(description = "사용자 역할", example = "USER", implementation = UserRole.class)
    private UserRole role;

    @Schema(description = "OTP 사용 여부", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean otpEnabled;

    @Schema(description = "OTP 검증 완료 여부", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean otpVerified;

    @Schema(description = "계정 생성일시", example = "2024-01-15T10:30:00", type = "string", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createDate;

    @Schema(description = "계정 수정일시", example = "2024-01-15T10:30:00", type = "string", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime modifyDate;

    @Schema(description = "마지막 로그인 일시", example = "2024-01-15T10:30:00", type = "string", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Schema(description = "로그인 실패 횟수", example = "0", minimum = "0", maximum = "5", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer loginFailCount;

    @Schema(description = "계정 잠금 일시", example = "null", type = "string", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime accountLockedAt;

    @Schema(description = "승인한 관리자 ID", example = "2", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    private Long approvedBy;

    @Schema(description = "승인 일시", example = "2024-01-15T11:00:00", type = "string", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @Schema(description = "비밀번호 변경 일시", example = "2024-01-15T10:30:00", type = "string", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime passwordChangedAt;

    @Schema(description = "비밀번호 변경 횟수", example = "1", minimum = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer passwordChangeCount;

    @Schema(description = "마지막 비밀번호 변경 날짜", example = "2024-01-15T10:30:00", type = "string", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastPasswordChangeDate;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .usernameMasked(false)
                .emailMasked(false)
                .phoneMasked(false)
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider() : AuthProvider.WEB)
                .profileImageUrl(user.getProfileImageUrl())
                .watchlist(user.getWatchlist())
                .status(user.getStatus())
                .role(user.getRole())
                .otpEnabled(Boolean.TRUE.equals(user.getOtpEnabled()))
                .otpVerified(Boolean.TRUE.equals(user.getOtpVerified()))
                .createDate(user.getCreatedAt())
                .modifyDate(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .loginFailCount(user.getLoginFailCount())
                .accountLockedAt(user.getAccountLockedAt())
                .approvedBy(user.getApprovedBy())
                .approvedAt(user.getApprovedAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .passwordChangeCount(user.getPasswordChangeCount())
                .lastPasswordChangeDate(user.getLastPasswordChangeDate() != null ? user.getLastPasswordChangeDate().atStartOfDay() : null)
                .build();
    }

    public static UserResponse fromAdmin(User user, boolean revealUsername, boolean revealEmail, boolean revealPhone) {
        return UserResponse.builder()
                .id(user.getId())
                .username(revealUsername ? user.getUsername() : user.getMaskedUsername())
                .nickname(user.getNickname())
                .email(revealEmail ? user.getEmail() : user.getMaskedEmail())
                .phoneNumber(revealPhone ? user.getPhoneNumber() : user.getMaskedPhoneNumber())
                .usernameMasked(!revealUsername)
                .emailMasked(!revealEmail)
                .phoneMasked(!revealPhone)
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider() : AuthProvider.WEB)
                .profileImageUrl(user.getProfileImageUrl())
                .watchlist(user.getWatchlist())
                .status(user.getStatus())
                .role(user.getRole())
                .otpEnabled(Boolean.TRUE.equals(user.getOtpEnabled()))
                .otpVerified(Boolean.TRUE.equals(user.getOtpVerified()))
                .createDate(user.getCreatedAt())
                .modifyDate(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .loginFailCount(user.getLoginFailCount())
                .accountLockedAt(user.getAccountLockedAt())
                .approvedBy(user.getApprovedBy())
                .approvedAt(user.getApprovedAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .passwordChangeCount(user.getPasswordChangeCount())
                .lastPasswordChangeDate(user.getLastPasswordChangeDate() != null ? user.getLastPasswordChangeDate().atStartOfDay() : null)
                .build();
    }
}