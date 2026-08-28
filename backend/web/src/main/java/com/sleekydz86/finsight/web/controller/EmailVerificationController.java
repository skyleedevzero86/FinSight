package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.email.EmailVerificationService;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationChallengeResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationConfirmRequest;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationConfirmResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationDisputeResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationIssueResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationRequestDto;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationResetPasswordRequest;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.EmailVerificationException;
import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이메일 인증", description = "회원가입/아이디찾기/비밀번호찾기 이메일 검증 코드")
@RestController
@RequestMapping("/api/v1/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(summary = "검증 코드 메일 발송")
    @PostMapping("/verify-request")
    public ResponseEntity<ApiResponse<EmailVerificationIssueResponse>> requestVerification(
            @RequestBody @Valid EmailVerificationRequestDto request,
            HttpServletRequest httpRequest) {
        try {
            EmailVerificationIssueResponse response = emailVerificationService.issue(
                    request.getEmail(), request.getPurpose(), httpRequest);
            return ResponseEntity.ok(ApiResponse.success(response, "인증 메일을 발송했습니다."));
        } catch (EmailVerificationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    @Operation(summary = "암호화된 인증 페이지 정보 조회")
    @GetMapping("/challenge")
    public ResponseEntity<ApiResponse<EmailVerificationChallengeResponse>> getChallenge(
            @RequestParam("token") String token) {
        try {
            return ResponseEntity.ok(ApiResponse.success(emailVerificationService.getChallenge(token)));
        } catch (EmailVerificationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "6자리 검증 코드 확인")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<EmailVerificationConfirmResponse>> verify(
            @RequestBody @Valid EmailVerificationConfirmRequest request) {
        try {
            EmailVerificationConfirmResponse response = emailVerificationService.confirm(
                    request.getToken(), request.getCode());
            return ResponseEntity.ok(ApiResponse.success(response, "이메일 인증이 완료되었습니다."));
        } catch (EmailVerificationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "비밀번호 찾기 인증 후 재설정")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid EmailVerificationResetPasswordRequest request) {
        try {
            emailVerificationService.resetPassword(
                    request.getToken(), request.getUsername(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 변경되었습니다."));
        } catch (EmailVerificationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (InvalidPasswordException e) {
            String detail = e.getValidationErrors() == null || e.getValidationErrors().isEmpty()
                    ? e.getMessage()
                    : String.join(" ", e.getValidationErrors());
            return ResponseEntity.badRequest().body(ApiResponse.error(detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    @Operation(summary = "요청하지 않은 인증 신고 · 계정 정지")
    @PostMapping("/dispute")
    public ResponseEntity<ApiResponse<EmailVerificationDisputeResponse>> dispute(
            @RequestParam("token") String token) {
        try {
            EmailVerificationDisputeResponse response = emailVerificationService.dispute(token);
            return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
        } catch (EmailVerificationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("요청 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }
}
