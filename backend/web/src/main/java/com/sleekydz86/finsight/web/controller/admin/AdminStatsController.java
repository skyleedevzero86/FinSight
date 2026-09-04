package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsChartResponse;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsOverviewResponse;
import com.sleekydz86.finsight.core.user.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "관리자 통계", description = "관리자 사용자·콘텐츠·헬스·JVM 메트릭 통계 API (ADMIN/MANAGER)")
@SecurityRequirement(name = "BearerAuth")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/overview")
    @Operation(
            summary = "통계 개요",
            description = "회원·콘텐츠 집계, 헬스 스냅샷, JVM 메트릭 스냅샷을 반환합니다. 프론트 30초 폴링용.")
    public ResponseEntity<ApiResponse<AdminStatsOverviewResponse>> overview() {
        log.info("관리자 통계 개요 API 호출");
        AdminStatsOverviewResponse response = adminStatsService.overview();
        return ResponseEntity.ok(ApiResponse.success(response, "통계 개요를 조회했습니다"));
    }

    @GetMapping("/charts/{chartKey}")
    @Operation(
            summary = "시계열/분류 차트",
            description = "관리자 통계 차트. days(1~90) 또는 from/to(yyyy-MM-dd)로 기간을 지정합니다.")
    public ResponseEntity<ApiResponse<AdminStatsChartResponse>> chart(
            @Parameter(
                    description = "차트 키",
                    required = true,
                    schema = @Schema(allowableValues = {
                            "signups", "providers", "logins", "cumulative", "status",
                            "content", "news", "health", "metrics"
                    }))
            @PathVariable String chartKey,
            @Parameter(description = "조회 일수 (1~90)", example = "7")
            @RequestParam(required = false, defaultValue = "7") Integer days,
            @Parameter(description = "시작일 (yyyy-MM-dd)", example = "2026-08-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "종료일 (yyyy-MM-dd)", example = "2026-09-05")
            @RequestParam(required = false) String to) {
        log.info("관리자 통계 차트 API 호출 - chartKey={}, days={}, from={}, to={}", chartKey, days, from, to);
        AdminStatsChartResponse response = adminStatsService.chart(
                chartKey,
                days,
                parseDate(from),
                parseDate(to));
        return ResponseEntity.ok(ApiResponse.success(response, "통계 차트를 조회했습니다"));
    }

    private java.time.LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/health/refresh")
    @Operation(
            summary = "헬스 상태 새로고침",
            description = "전체 헬스·외부 API를 재수집하고 이력으로 저장합니다. 차트용 스냅샷에도 반영됩니다.")
    public ResponseEntity<ApiResponse<Health>> refreshHealth() {
        log.info("관리자 헬스 새로고침 API 호출");
        Health health = adminStatsService.refreshHealth();
        return ResponseEntity.ok(ApiResponse.success(health, "헬스 상태를 새로고침했습니다"));
    }
}
