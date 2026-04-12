package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PageRequest;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;
import com.sleekydz86.finsight.core.user.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> list(PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response =
                    PaginationResponse.from(userApplicationService.getUsers(pageRequest.toPageable()));
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 사용자 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> listPending(PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response =
                    PaginationResponse.from(userApplicationService.getPendingUsers(pageRequest.toPageable()));
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
            PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(
                    userApplicationService.getUsersByStatusAndRole(status.name(), null, pageRequest.toPageable()));
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
            PageRequest pageRequest) {
        try {
            PaginationResponse<UserResponse> response = PaginationResponse.from(
                    userApplicationService.getUsersByStatusAndRole(null, role.name(), pageRequest.toPageable()));
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
    public ResponseEntity<ApiResponse<UserResponse>> suspend(@PathVariable Long userId) {
        try {
            UserResponse response = userApplicationService.suspendUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
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
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("사용자 잠금 해제 실패: {}", e.getMessage());
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

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 탈퇴 처리")
    public ResponseEntity<ApiResponse<Void>> withdraw(@PathVariable Long userId) {
        try {
            userApplicationService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("사용자 탈퇴 처리 실패: {}", e.getMessage());
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
}
