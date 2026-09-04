package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.notification.service.SmsAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Tag(
        name = "관리자 SMS",
        description = "ADMIN/MANAGER Solapi SMS 설정·수동발송·잔액·이미지업로드·이력·통계. "
                + "마스터/용도별 체크 설정이 자동·수동 발송을 제어합니다.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/sms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminSmsController {

    private final SmsAdminService smsAdminService;

    @Operation(summary = "SMS 발송 설정 조회", description = "마스터·뉴스·OTP·복구·시스템·일반 알림 on/off 및 기본 타입")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SmsSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(smsAdminService.getSettings(), "SMS 설정을 조회했습니다"));
    }

    @Operation(summary = "SMS 발송 설정 저장", description = "체크박스 설정 저장. enabled=false면 모든 자동/수동 발송 차단")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SmsSettingsResponse>> updateSettings(
            @Valid @RequestBody SmsSettingsUpdateRequest request) {
        log.info("SMS 설정 변경 - enabled={}", request.enabled());
        return ResponseEntity.ok(ApiResponse.success(smsAdminService.updateSettings(request), "SMS 설정을 저장했습니다"));
    }

    @Operation(summary = "SMS 수동 발송", description = "toPhone 또는 userEmail로 발송. 마스터 스위치 필요")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SmsManualSendResponse>> send(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody SmsManualSendRequest request) {
        log.info("관리자 SMS 수동 발송 - actorId={}, toPhone={}, email={}",
                currentUser.getId(), request.toPhone(), request.userEmail());
        SmsManualSendResponse result = smsAdminService.sendManual(request, currentUser.getId());
        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(ApiResponse.success(result, result.success() ? "SMS를 발송했습니다" : "SMS 발송에 실패했습니다"));
    }

    @Operation(summary = "Solapi 잔액 조회")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<SmsBalanceResponse>> balance() {
        return ResponseEntity.ok(ApiResponse.success(smsAdminService.balance(), "잔액을 조회했습니다"));
    }

    @Operation(summary = "MMS 이미지 업로드", description = "multipart file 업로드 후 imageId 반환")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestPart("file") MultipartFile file) throws IOException {
        String imageId = smsAdminService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("imageId", imageId), "이미지를 업로드했습니다"));
    }

    @Operation(summary = "SMS 발송 이력", description = "페이지 목록. status·purpose 필터 가능")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PaginationResponse<SmsSendLogResponse>>> logs(
            @Parameter(description = "상태 필터") @RequestParam(required = false) SmsSendStatus status,
            @Parameter(description = "용도 필터") @RequestParam(required = false) SmsPurpose purpose,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                smsAdminService.listLogs(status, purpose, page, size),
                "SMS 이력을 조회했습니다"));
    }

    @Operation(summary = "SMS 발송 통계", description = "성공/실패/스킵·용도별·최근 7일 일별")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SmsStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success(smsAdminService.stats(), "SMS 통계를 조회했습니다"));
    }
}
