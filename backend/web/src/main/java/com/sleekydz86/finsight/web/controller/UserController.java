package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<User>> getUserProfile(Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            User user = userQueryUseCase.getUserByEmail(userEmail);
            return ResponseEntity.ok(ApiResponse.success(user, "사용자 프로필을 성공적으로 조회했습니다"));
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
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            validateUpdateRequest(request);
            User user = userCommandUseCase.updateUser(userEmail, request);
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
    public ResponseEntity<ApiResponse<List<String>>> getUserWatchlist(Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            List<String> watchlist = userQueryUseCase.getUserWatchlist(userEmail);
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
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            validateWatchlistRequest(request);
            userCommandUseCase.updateWatchlist(userEmail, request);
            return ResponseEntity.ok(ApiResponse.success(null, "관심목록이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("관심목록 수정 중 오류가 발생했습니다", "USER_WATCHLIST_UPDATE_ERROR", e);
        }
    }

    @GetMapping("/bookmarks")
    @LogExecution("사용자 북마크 조회")
    @PerformanceMonitor(threshold = 2000, operation = "user_bookmarks")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<List<Object>>> getUserBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다");
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다");
            }

            List<Object> bookmarks = userQueryUseCase.getUserBookmarks(userEmail, page, size);
            return ResponseEntity.ok(ApiResponse.success(bookmarks, "북마크 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("북마크 목록 조회 중 오류가 발생했습니다", "USER_BOOKMARKS_ERROR", e);
        }
    }

    @DeleteMapping("/account")
    @LogExecution("사용자 계정 삭제")
    @PerformanceMonitor(threshold = 3000, operation = "user_account_delete")
    @Retryable(maxAttempts = 1, delay = 5000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> deleteUserAccount(Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            userCommandUseCase.deleteUser(userEmail);
            return ResponseEntity.ok(ApiResponse.success(null, "계정이 성공적으로 삭제되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("계정 삭제 중 오류가 발생했습니다", "USER_ACCOUNT_DELETE_ERROR", e);
        }
    }

    @PostMapping("/change-password")
    @LogExecution("사용자 비밀번호 변경")
    @PerformanceMonitor(threshold = 2000, operation = "user_password_change")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            validatePasswordChangeRequest(request);
            userCommandUseCase.changePassword(userEmail, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 성공적으로 변경되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("비밀번호 변경 중 오류가 발생했습니다", "USER_PASSWORD_CHANGE_ERROR", e);
        }
    }

    private String getCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("인증이 필요합니다");
        }
        return authentication.getName();
    }

    private void validateUpdateRequest(UserUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("사용자 수정 요청이 null입니다");
        }
        if (request.getUsername() != null && request.getUsername().trim().isEmpty()) {
            throw new ValidationException("사용자명은 비어있을 수 없습니다");
        }
        if (request.getUsername() != null && request.getUsername().length() > 50) {
            throw new ValidationException("사용자명은 50자를 초과할 수 없습니다");
        }
    }

    private void validateWatchlistRequest(WatchlistUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("관심목록 수정 요청이 null입니다");
        }
        if (request.getCategories() != null && request.getCategories().size() > 20) {
            throw new ValidationException("관심 카테고리는 20개를 초과할 수 없습니다");
        }
    }

    private void validatePasswordChangeRequest(PasswordChangeRequest request) {
        if (request == null) {
            throw new ValidationException("비밀번호 변경 요청이 null입니다");
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new ValidationException("현재 비밀번호는 필수입니다");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new ValidationException("새 비밀번호는 필수입니다");
        }
        if (request.getNewPassword().length() < 12) {
            throw new ValidationException("새 비밀번호는 12자 이상이어야 합니다");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ValidationException("새 비밀번호는 현재 비밀번호와 달라야 합니다");
        }
    }

    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
