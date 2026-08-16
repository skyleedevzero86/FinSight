package com.sleekydz86.finsight.web.controller.user;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserPasswordChangeRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.PasswordStatusResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.ProfileUpdateResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserDashboardResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;
import com.sleekydz86.finsight.core.user.service.ProfileImageStorageService;
import com.sleekydz86.finsight.core.user.service.StoredProfileImage;
import com.sleekydz86.finsight.core.user.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "로그인 사용자 본인 계정")
public class UserAccountController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
    private final UserApplicationService userApplicationService;
    private final ProfileImageStorageService profileImageStorageService;
    private final AuthenticationService authenticationService;

    @GetMapping("/profile")
    @LogExecution("사용자 프로필 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_profile")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    @Operation(summary = "현재 사용자 정보 조회")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@CurrentUser AuthenticatedUser currentUser) {
        try {
            UserResponse response = userApplicationService.getCurrentUserInfo(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(response, "사용자 프로필을 성공적으로 조회했습니다"));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            throw new SystemException("사용자 프로필 조회 중 오류가 발생했습니다", "USER_PROFILE_ERROR", e);
        }
    }

    @PutMapping("/profile")
    @LogExecution("사용자 프로필 수정")
    @PerformanceMonitor(threshold = 2000, operation = "user_profile_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    @Operation(summary = "사용자 프로필 수정")
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateUserProfile(
            @RequestBody @Valid UserUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            validateUpdateRequest(request);
            String previousEmail = currentUser.getEmail();
            UserResponse response = userApplicationService.updateProfile(currentUser.getId(), request);
            JwtToken token = null;
            if (response.getEmail() != null && previousEmail != null
                    && !response.getEmail().equalsIgnoreCase(previousEmail)) {
                token = authenticationService.issueTokens(
                        userQueryUseCase.findById(currentUser.getId())
                                .orElseThrow(() -> new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"))));
            }
            return ResponseEntity.ok(ApiResponse.success(
                    ProfileUpdateResponse.of(response, token),
                    "사용자 프로필이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ValidationException(e.getMessage(), List.of("PROFILE_UPDATE_FAILED"));
        } catch (Exception e) {
            log.error("사용자 프로필 수정 실패: {}", e.getMessage());
            throw new SystemException("사용자 프로필 수정 중 오류가 발생했습니다", "USER_PROFILE_UPDATE_ERROR", e);
        }
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 사진 업로드")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @RequestPart("image") MultipartFile image,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            UserResponse current = userApplicationService.getCurrentUserInfo(currentUser.getId());
            if (current.getAuthProvider() == null
                    || current.getAuthProvider() == com.sleekydz86.finsight.core.user.domain.AuthProvider.WEB) {
                throw new ValidationException("SNS 계정만 프로필 사진을 변경할 수 있습니다", List.of("SNS_ONLY"));
            }
            String url = profileImageStorageService.store(currentUser.getId(), image);
            UserResponse response = userApplicationService.updateProfile(
                    currentUser.getId(),
                    UserUpdateRequest.builder().profileImageUrl(url).build());
            return ResponseEntity.ok(ApiResponse.success(response, "프로필 사진이 변경되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new SystemException("프로필 사진 저장 중 오류가 발생했습니다", "PROFILE_IMAGE_SAVE_ERROR", e);
        }
    }

    @GetMapping("/avatars/{userId}")
    @Operation(summary = "프로필 사진 조회")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) {
        Optional<StoredProfileImage> stored = profileImageStorageService.load(userId);
        if (stored.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        StoredProfileImage image = stored.get();
        Resource resource = new FileSystemResource(image.getPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    @GetMapping("/watchlist")
    @LogExecution("사용자 관심목록 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_watchlist")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<TargetCategory>>> getUserWatchlist(@CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            List<TargetCategory> watchlist = userQueryUseCase.getUserWatchlist(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success(watchlist, "관심목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("관심목록 조회 중 오류가 발생했습니다", "USER_WATCHLIST_ERROR", e);
        }
    }

    @PutMapping("/watchlist")
    @LogExecution("사용자 관심목록 수정")
    @PerformanceMonitor(threshold = 2000, operation = "user_watchlist_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> updateUserWatchlist(
            @RequestBody @Valid WatchlistUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            validateWatchlistRequest(request);
            userCommandUseCase.updateWatchlist(userOpt.get().getId(), request);
            return ResponseEntity.ok(ApiResponse.success(null, "관심목록이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("관심목록 수정 중 오류가 발생했습니다", "USER_WATCHLIST_UPDATE_ERROR", e);
        }
    }

    @GetMapping("/notification-preferences")
    @LogExecution("사용자 알림 설정 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_notification_preferences")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<NotificationType>>> getUserNotificationPreferences(
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            List<NotificationType> preferences = userQueryUseCase.getUserNotificationPreferences(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success(preferences, "알림 설정을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("알림 설정 조회 중 오류가 발생했습니다", "USER_NOTIFICATION_PREFERENCES_ERROR", e);
        }
    }

    @PutMapping("/notification-preferences")
    @LogExecution("사용자 알림 설정 수정")
    @PerformanceMonitor(threshold = 2000, operation = "user_notification_preferences_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> updateUserNotificationPreferences(
            @RequestBody @Valid List<NotificationType> preferences,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            userCommandUseCase.updateNotificationPreferences(userOpt.get().getId(), preferences);
            return ResponseEntity.ok(ApiResponse.success(null, "알림 설정이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("알림 설정 수정 중 오류가 발생했습니다", "USER_NOTIFICATION_PREFERENCES_UPDATE_ERROR", e);
        }
    }

    @PostMapping("/password/change")
    @Operation(summary = "비밀번호 변경")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UserPasswordChangeRequest request,
            @CurrentUser AuthenticatedUser user) {
        try {
            userApplicationService.changePassword(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/password/status")
    @Operation(summary = "비밀번호 상태 조회")
    public ResponseEntity<ApiResponse<PasswordStatusResponse>> getPasswordStatus(@CurrentUser AuthenticatedUser user) {
        try {
            PasswordStatusResponse response = userApplicationService.getPasswordStatus(user.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("비밀번호 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @LogExecution("사용자 대시보드 조회")
    @PerformanceMonitor(threshold = 3000, operation = "user_dashboard")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<UserDashboardResponse>> getUserDashboard(@CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            User user = userOpt.get();
            UserDashboardResponse dashboard = UserDashboardResponse.of(
                    user,
                    userQueryUseCase.getUserWatchlist(user.getId()),
                    userQueryUseCase.getUserNotificationPreferences(user.getId()));

            return ResponseEntity.ok(ApiResponse.success(dashboard, "사용자 대시보드를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("사용자 대시보드 조회 중 오류가 발생했습니다", "USER_DASHBOARD_ERROR", e);
        }
    }

    private void validateUpdateRequest(UserUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("사용자 수정 요청이 null입니다", List.of("REQUEST_NULL"));
        }
        if (request.getUsername() != null && request.getUsername().trim().isEmpty()) {
            throw new ValidationException("사용자명은 비어있을 수 없습니다", List.of("USERNAME_EMPTY"));
        }
        if (request.getUsername() != null && request.getUsername().length() > 50) {
            throw new ValidationException("사용자명은 50자를 초과할 수 없습니다", List.of("USERNAME_TOO_LONG"));
        }
        if (request.getNickname() != null && request.getNickname().length() > 50) {
            throw new ValidationException("닉네임은 50자를 초과할 수 없습니다", List.of("NICKNAME_TOO_LONG"));
        }
        if (request.getEmail() != null && request.getEmail().trim().isEmpty()) {
            throw new ValidationException("이메일은 비어있을 수 없습니다", List.of("EMAIL_EMPTY"));
        }
    }

    private void validateWatchlistRequest(WatchlistUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("관심목록 수정 요청이 null입니다", List.of("REQUEST_NULL"));
        }
        if (request.getCategories() != null && request.getCategories().size() > 20) {
            throw new ValidationException("관심 카테고리는 20개를 초과할 수 없습니다", List.of("CATEGORIES_TOO_MANY"));
        }
    }
}
