package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthCommandUseCase;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "헬스", description = "시스템 헬스·메트릭·외부 서비스 상태 API. 실시간 추이는 관리자 통계(/api/v1/admin/stats)에서 차트·폴링으로 제공합니다.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthQueryUseCase healthQueryUseCase;
    private final HealthCommandUseCase healthCommandUseCase;

    public HealthController(HealthQueryUseCase healthQueryUseCase, HealthCommandUseCase healthCommandUseCase) {
        this.healthQueryUseCase = healthQueryUseCase;
        this.healthCommandUseCase = healthCommandUseCase;
    }

    @Operation(
            summary = "시스템 상태 조회",
            description = "전체 시스템 헬스 스냅샷을 조회합니다. 시계열·관리자 대시보드는 /api/v1/admin/stats 를 사용하세요.")
    @GetMapping
    @LogExecution("시스템 상태 조회")
    @PerformanceMonitor(threshold = 1000, operation = "health_check")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Health>> getSystemHealth() {
        try {
            Health health = healthQueryUseCase.getCompleteHealth();
            return ResponseEntity.ok(ApiResponse.success(health, "시스템 상태를 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("시스템 상태 조회 중 오류가 발생했습니다", "HEALTH_CHECK_ERROR", e);
        }
    }

    @Operation(
            summary = "시스템 메트릭 조회",
            description = "JVM·OS 메트릭 실시간 스냅샷. 일별 추이는 관리자 통계 charts/metrics 를 사용하세요.")
    @GetMapping("/metrics")
    @LogExecution("시스템 메트릭 조회")
    @PerformanceMonitor(threshold = 2000, operation = "health_metrics")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMetrics() {
        try {
            SystemMetrics systemMetrics = healthQueryUseCase.getSystemMetrics();
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("jvm", systemMetrics.getJvmMetrics());
            metrics.put("system", systemMetrics.getSystemMetrics());
            metrics.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.success(metrics, "시스템 메트릭을 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("시스템 메트릭 조회 중 오류가 발생했습니다", "HEALTH_METRICS_ERROR", e);
        }
    }

    @Operation(summary = "데이터베이스 상태 조회", description = "데이터베이스 헬스 상태를 조회합니다.")
    @GetMapping("/database")
    @LogExecution("데이터베이스 상태 조회")
    @PerformanceMonitor(threshold = 2000, operation = "health_database")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseHealth() {
        try {
            HealthStatus databaseHealth = healthQueryUseCase.getDatabaseHealth();
            Map<String, Object> result = new HashMap<>();
            result.put("status", databaseHealth.getStatus());
            result.put("message", databaseHealth.getMessage());
            result.put("details", databaseHealth.getDetails());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.success(result, "데이터베이스 상태를 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("데이터베이스 상태 조회 중 오류가 발생했습니다", "HEALTH_DATABASE_ERROR", e);
        }
    }

    @Operation(summary = "외부 서비스 상태 조회", description = "외부 API·서비스 헬스 상태를 조회합니다.")
    @GetMapping("/external-services")
    @LogExecution("외부 서비스 상태 조회")
    @PerformanceMonitor(threshold = 5000, operation = "health_external")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExternalServicesHealth() {
        try {
            Map<String, HealthStatus> externalHealth = healthQueryUseCase.getExternalApisHealth();
            Map<String, Object> result = new HashMap<>();
            externalHealth.forEach((service, status) -> {
                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("status", status.getStatus());
                serviceInfo.put("message", status.getMessage());
                serviceInfo.put("details", status.getDetails());
                result.put(service, serviceInfo);
            });
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.success(result, "외부 서비스 상태를 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("외부 서비스 상태 조회 중 오류가 발생했습니다", "HEALTH_EXTERNAL_ERROR", e);
        }
    }

    @Operation(
            summary = "상태 정보 새로고침",
            description = "헬스 상태를 재수집하고 저장합니다. 관리자 UI는 POST /api/v1/admin/stats/health/refresh 를 사용합니다.")
    @PostMapping("/refresh")
    @LogExecution("상태 정보 새로고침")
    @PerformanceMonitor(threshold = 3000, operation = "health_refresh")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> refreshHealthStatus() {
        try {
            Health health = healthQueryUseCase.getCompleteHealth();
            healthCommandUseCase.saveHealthCheck(health);
            return ResponseEntity.ok(ApiResponse.success(null, "상태 정보가 성공적으로 새로고침되었습니다"));
        } catch (Exception e) {
            throw new SystemException("상태 정보 새로고침 중 오류가 발생했습니다", "HEALTH_REFRESH_ERROR", e);
        }
    }
}
