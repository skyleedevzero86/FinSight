package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.*;
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
import com.sleekydz86.finsight.core.user.domain.port.out.dto.PasswordStatusResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserLoginResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;
import com.sleekydz86.finsight.core.user.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.dto.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 및 인증 API")
public class UserController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;
    private final UserApplicationService userApplicationService;

    @GetMapping("/profile")
    @LogExecution("사용자 프로필 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_profile")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    @Operation(summary = "현재 사용자 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
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
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    @Operation(summary = "사용자 프로필 수정", description = "사용자의 프로필 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @RequestBody @Valid UserUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            validateUpdateRequest(request);
            UserResponse response = userApplicationService.updateProfile(currentUser.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(response, "사용자 프로필이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 프로필 수정 실패: {}", e.getMessage());
            throw new SystemException("사용자 프로필 수정 중 오류가 발생했습니다", "USER_PROFILE_UPDATE_ERROR", e);
        }
    }

    @GetMapping("/watchlist")
    @LogExecution("사용자 관심목록 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_watchlist")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
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
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
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
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<List<NotificationType>>> getUserNotificationPreferences(@CurrentUser AuthenticatedUser currentUser) {
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
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
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

    @PostMapping("/join")
    @Operation(summary = "사용자 가입", description = "새로운 사용자를 등록합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> join(@Valid @RequestBody UserJoinRequest request) {
        try {
            UserResponse response = userApplicationService.join(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 인증을 수행합니다.")
    public ResponseEntity<ApiResponse<UserLoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            UserLoginResponse response = userApplicationService.login(request, clientIp);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/password/change")
    @Operation(summary = "비밀번호 변경", description = "사용자의 비밀번호를 변경합니다.")
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
    @Operation(summary = "비밀번호 상태 조회", description = "사용자의 비밀번호 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<PasswordStatusResponse>> getPasswordStatus(@CurrentUser AuthenticatedUser user) {
        try {
            PasswordStatusResponse response = userApplicationService.getPasswordStatus(user.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("비밀번호 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin")
    @Operation(summary = "사용자 목록 조회", description = "관리자가 모든 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getUsers(PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(userApplicationService.getUsers(pageRequest.toPageable()));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/admin/pending")
    @Operation(summary = "승인 대기 사용자 목록", description = "승인 대기 중인 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getPendingUsers(PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(userApplicationService.getPendingUsers(pageRequest.toPageable()));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("승인 대기 사용자 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @PostMapping("/admin/{userId}/approve")
    @Operation(summary = "사용자 승인", description = "승인 대기 중인 사용자를 승인합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser approver) {
        try {
            UserResponse response = userApplicationService.approveUser(userId, approver.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 승인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/{userId}/reject")
    @Operation(summary = "사용자 거부", description = "승인 대기 중인 사용자를 거부합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.rejectUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 거부 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/{userId}/suspend")
    @Operation(summary = "사용자 정지", description = "사용자 계정을 정지합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.suspendUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 정지 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/{userId}/unlock")
    @Operation(summary = "사용자 잠금 해제", description = "정지된 사용자 계정을 잠금 해제합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.unlockUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 잠금 해제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/admin/{userId}/delete")
    @Operation(summary = "사용자 삭제", description = "사용자 계정을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        try {
            userApplicationService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("사용자 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin/status/{status}")
    @Operation(summary = "상태별 사용자 조회", description = "특정 상태의 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getUsersByStatus(
            @PathVariable UserStatus status,
            PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(userApplicationService.getUsersByStatusAndRole(status.name(), null, pageRequest.toPageable()));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("상태별 사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/admin/role/{role}")
    @Operation(summary = "역할별 사용자 조회", description = "특정 역할의 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getUsersByRole(
            @PathVariable UserRole role,
            PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(userApplicationService.getUsersByStatusAndRole(null, role.name(), pageRequest.toPageable()));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("역할별 사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @PostMapping("/admin/{userId}/role")
    @Operation(summary = "사용자 역할 변경", description = "사용자의 역할을 변경합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable Long userId,
            @RequestParam UserRole role,
            @CurrentUser AuthenticatedUser approver) {
        try {
            UserResponse response = userApplicationService.changeUserRole(userId, role, approver.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 역할 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @LogExecution("사용자 대시보드 조회")
    @PerformanceMonitor(threshold = 3000, operation = "user_dashboard")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Object>> getUserDashboard(@CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            User user = userOpt.get();
            Object dashboard = Map.of(
                    "user", user,
                    "watchlist", userQueryUseCase.getUserWatchlist(user.getId()),
                    "notificationPreferences", userQueryUseCase.getUserNotificationPreferences(user.getId()),
                    "lastLoginAt", user.getLastLoginAt(),
                    "createdAt", user.getCreatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success(dashboard, "사용자 대시보드를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("사용자 대시보드 조회 중 오류가 발생했습니다", "USER_DASHBOARD_ERROR", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
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