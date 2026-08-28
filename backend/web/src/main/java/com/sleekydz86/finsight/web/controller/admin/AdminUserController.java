package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.AdminPasswordResetRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;
import com.sleekydz86.finsight.core.user.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Admin Users", description = "관리자 사용자 관리")
public class AdminUserController {

    private final UserApplicationService userApplicationService;

    @GetMapping
    @Operation(summary = "사용자 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reveal) {
        boolean revealUsername = containsReveal(reveal, "username");
        boolean revealEmail = containsReveal(reveal, "email");
        boolean revealPhone = containsReveal(reveal, "phone");
        log.info("관리자 사용자 목록 조회: page={}, size={}, status={}, keyword={}", page, size, status, keyword);
        PaginationResponse<UserResponse> response = PaginationResponse.from(
                userApplicationService.searchAdminUsers(
                        status,
                        keyword,
                        revealUsername,
                        revealEmail,
                        revealPhone,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "id"))));
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 목록을 조회했습니다"));
    }

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 사용자 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginationResponse<UserResponse> response =
                    PaginationResponse.from(userApplicationService.getPendingUsers(
                            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("승인 대기 사용자 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 단건 조회")
    public ResponseEntity<ApiResponse<UserResponse>> getOne(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.getUserInfo(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "상태별 사용자 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> listByStatus(
            @PathVariable UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(
                    userApplicationService.getUsersByStatusAndRole(status.name(), null,
                            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("상태별 사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "역할별 사용자 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> listByRole(
            @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(
                    userApplicationService.getUsersByStatusAndRole(null, role.name(),
                            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("역할별 사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "사용자 승인")
    public ResponseEntity<ApiResponse<UserResponse>> approve(
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

    @PostMapping("/{userId}/reject")
    @Operation(summary = "사용자 거부")
    public ResponseEntity<ApiResponse<UserResponse>> reject(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.rejectUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 거부 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "사용자 정지")
    public ResponseEntity<ApiResponse<UserResponse>> suspend(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser actor) {
        try {
            if (actor.getId() != null && actor.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("본인 계정은 정지할 수 없습니다."));
            }
            UserResponse response = userApplicationService.suspendUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "계정을 정지했습니다"));
        } catch (Exception e) {
            log.error("사용자 정지 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "사용자 잠금 해제")
    public ResponseEntity<ApiResponse<UserResponse>> unlock(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.unlockUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "정지를 해제했습니다"));
        } catch (Exception e) {
            log.error("사용자 잠금 해제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/restore")
    @Operation(summary = "정지·탈퇴 계정 복구")
    public ResponseEntity<ApiResponse<UserResponse>> restore(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.restoreUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "계정을 복구해 다시 로그인할 수 있습니다"));
        } catch (Exception e) {
            log.error("사용자 복구 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/reset-pending")
    @Operation(summary = "사용자를 승인 대기로 되돌림")
    public ResponseEntity<ApiResponse<UserResponse>> resetPending(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.resetToPending(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("승인 대기 초기화 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{userId}/password")
    @Operation(summary = "관리자 비밀번호 재설정")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminPasswordResetRequest request,
            @CurrentUser AuthenticatedUser actor) {
        try {
            userApplicationService.adminResetPassword(userId, request, actor.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호를 변경했습니다"));
        } catch (Exception e) {
            log.error("관리자 비밀번호 재설정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 DB 삭제 (관리자 탈퇴)")
    public ResponseEntity<ApiResponse<Void>> hardDelete(
            @PathVariable Long userId,
            @CurrentUser AuthenticatedUser actor) {
        try {
            userApplicationService.hardDeleteUser(userId, actor.getId());
            return ResponseEntity.ok(ApiResponse.success(null, "사용자를 데이터베이스에서 삭제했습니다"));
        } catch (Exception e) {
            log.error("사용자 DB 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/role")
    @Operation(summary = "사용자 역할 변경")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long userId,
            @RequestParam UserRole role,
            @CurrentUser AuthenticatedUser actor) {
        try {
            UserResponse response = userApplicationService.changeUserRole(userId, role, actor.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 역할 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private boolean containsReveal(String reveal, String field) {
        if (reveal == null || reveal.isBlank()) {
            return false;
        }
        String[] parts = reveal.split(",");
        for (String part : parts) {
            if (field.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }
}
