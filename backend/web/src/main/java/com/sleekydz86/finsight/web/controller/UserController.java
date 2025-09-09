package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserCommandUseCase userCommandUseCase;

    public UserController(UserQueryUseCase userQueryUseCase, UserCommandUseCase userCommandUseCase) {
        this.userQueryUseCase = userQueryUseCase;
        this.userCommandUseCase = userCommandUseCase;
    }

    @GetMapping("/profile")
    @LogExecution("사용자 프로필 조회")
    @PerformanceMonitor(threshold = 1000, operation = "user_profile")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<User>> getUserProfile(@CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(userOpt.get(), "사용자 프로필을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("사용자 프로필 조회 중 오류가 발생했습니다", "USER_PROFILE_ERROR", e);
        }
    }

    @PutMapping("/profile")
    @LogExecution("사용자 프로필 수정")
    @PerformanceMonitor(threshold = 2000, operation = "user_profile_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<User>> updateUserProfile(
            @RequestBody @Valid UserUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Optional<User> userOpt = userQueryUseCase.findByEmail(currentUser.getEmail());
            if (userOpt.isEmpty()) {
                throw new ValidationException("사용자를 찾을 수 없습니다", List.of("USER_NOT_FOUND"));
            }

            validateUpdateRequest(request);
            User user = userCommandUseCase.updateUser(userOpt.get().getId(), request);
            return ResponseEntity.ok(ApiResponse.success(user, "사용자 프로필이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
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