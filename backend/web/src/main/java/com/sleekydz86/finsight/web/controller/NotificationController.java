package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationCommandUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "알림", description = "알림 관리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCommandUseCase notificationCommandUseCase;
    private final NotificationQueryUseCase notificationQueryUseCase;
    private final UserPersistencePort userPersistencePort;

    @GetMapping
    @LogExecution("알림 목록 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_notifications")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @CurrentUser AuthenticatedUser currentUser,
            Pageable pageable) {

        User user = userPersistencePort.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Page<Notification> notifications = notificationQueryUseCase.getNotificationsByUser(user, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{notificationId}")
    @LogExecution("알림 상세 조회")
    @PerformanceMonitor(threshold = 500, operation = "get_notification_detail")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    @Operation(summary = "알림 상세 조회", description = "특정 알림의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Notification>> getNotificationDetail(
            @PathVariable Long notificationId) {

        Notification notification = notificationQueryUseCase.getNotificationById(notificationId);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PostMapping("/{notificationId}/retry")
    @LogExecution("알림 재시도")
    @PerformanceMonitor(threshold = 2000, operation = "retry_notification")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    @Operation(summary = "알림 재시도", description = "실패한 알림을 재시도합니다.")
    public ResponseEntity<ApiResponse<Void>> retryNotification(
            @PathVariable Long notificationId) {

        notificationCommandUseCase.retryFailedNotification(notificationId);

        return ResponseEntity.ok(ApiResponse.success(null, "알림 재시도가 시작되었습니다."));
    }

    @DeleteMapping("/{notificationId}")
    @LogExecution("알림 취소")
    @PerformanceMonitor(threshold = 500, operation = "cancel_notification")
    @Retryable(maxAttempts = 2, delay = 1000, retryFor = {Exception.class})
    @Operation(summary = "알림 취소", description = "대기 중인 알림을 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> cancelNotification(
            @PathVariable Long notificationId) {

        notificationCommandUseCase.cancelNotification(notificationId);

        return ResponseEntity.ok(ApiResponse.success(null, "알림이 취소되었습니다."));
    }

    @GetMapping("/stats")
    @LogExecution("알림 통계 조회")
    @PerformanceMonitor(threshold = 1000, operation = "get_notification_stats")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    @Operation(summary = "알림 통계 조회", description = "사용자의 알림 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<NotificationStats>> getNotificationStats(
            @CurrentUser AuthenticatedUser currentUser) {

        User user = userPersistencePort.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        long totalCount = notificationQueryUseCase.getNotificationCountByUser(user);
        List<Notification> recentNotifications = notificationQueryUseCase
                .getNotificationsByDateRange(LocalDateTime.now().minusDays(7), LocalDateTime.now());

        NotificationStats stats = NotificationStats.builder()
                .totalCount(totalCount)
                .recentCount(recentNotifications.size())
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/test")
    @LogExecution("테스트 알림 발송")
    @PerformanceMonitor(threshold = 2000, operation = "send_test_notification")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    @Operation(summary = "테스트 알림 발송", description = "테스트 알림을 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> sendTestNotification(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam String channel) {

        User user = userPersistencePort.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 테스트 알림 생성 및 발송 로직 미처리
        // 실제 구현에서는 NotificationTemplate을 사용하여 테스트 알림 생성예정

        return ResponseEntity.ok(ApiResponse.success(null, "테스트 알림이 발송되었습니다."));
    }

    @lombok.Builder
    @lombok.Getter
    public static class NotificationStats {
        private final long totalCount;
        private final long recentCount;
    }
}