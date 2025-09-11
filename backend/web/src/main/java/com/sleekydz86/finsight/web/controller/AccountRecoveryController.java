package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryRequest;
import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryResponse;
import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryVerifyRequest;
import com.sleekydz86.finsight.core.auth.dto.PasswordResetRequest;
import com.sleekydz86.finsight.core.auth.service.AccountRecoveryService;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/account-recovery")
@RequiredArgsConstructor
@Tag(name = "계정 복구", description = "계정 분실 시 OTP를 통한 계정 복구 API")
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/initiate")
    @Operation(summary = "계정 복구 시작", description = "이메일과 사용자명으로 계정 복구를 시작합니다.")
    @LogExecution("계정 복구 시작")
    @PerformanceMonitor(threshold = 2000, metricName = "account_recovery_initiate")
    @SecurityAudit(action = "ACCOUNT_RECOVERY_INITIATE", resource = "ACCOUNT_RECOVERY", level = SecurityAudit.SecurityLevel.INFO)
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<AccountRecoveryResponse>> initiateRecovery(
            @RequestBody @Valid AccountRecoveryRequest request) {

        try {
            AccountRecoveryResponse response = accountRecoveryService.initiateAccountRecovery(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("계정 복구 시작 실패 - 이메일: {}, 사용자명: {}, 오류: {}",
                    request.getEmail(), request.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("계정 복구 시작에 실패했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "복구 OTP 검증", description = "발송된 OTP 코드를 검증합니다.")
    @LogExecution("복구 OTP 검증")
    @PerformanceMonitor(threshold = 1000, metricName = "account_recovery_verify_otp")
    @SecurityAudit(action = "ACCOUNT_RECOVERY_VERIFY_OTP", resource = "ACCOUNT_RECOVERY", level = SecurityAudit.SecurityLevel.INFO)
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<AccountRecoveryResponse>> verifyOtp(
            @RequestBody @Valid AccountRecoveryVerifyRequest request) {

        try {
            AccountRecoveryResponse response = accountRecoveryService.verifyRecoveryOtp(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("복구 OTP 검증 실패 - 이메일: {}, 사용자명: {}, 오류: {}",
                    request.getEmail(), request.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OTP 검증에 실패했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "비밀번호 재설정", description = "복구 토큰을 사용하여 비밀번호를 재설정합니다.")
    @LogExecution("비밀번호 재설정")
    @PerformanceMonitor(threshold = 2000, metricName = "account_recovery_reset_password")
    @SecurityAudit(action = "ACCOUNT_RECOVERY_RESET_PASSWORD", resource = "ACCOUNT_RECOVERY", level = SecurityAudit.SecurityLevel.INFO)
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<AccountRecoveryResponse>> resetPassword(
            @RequestBody @Valid PasswordResetRequest request) {

        try {
            AccountRecoveryResponse response = accountRecoveryService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("비밀번호 재설정에 실패했습니다: " + e.getMessage()));
        }
    }
}