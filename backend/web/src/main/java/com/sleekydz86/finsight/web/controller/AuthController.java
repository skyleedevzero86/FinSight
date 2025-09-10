package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.*;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.auth.service.OtpAuthenticationService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "사용자 인증 및 OTP 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final OtpAuthenticationService otpAuthenticationService;

    public AuthController(AuthenticationService authenticationService,
            UserService userService,
            OtpAuthenticationService otpAuthenticationService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.otpAuthenticationService = otpAuthenticationService;
    }

    @Operation(summary = "사용자 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    @LogExecution("사용자 로그인")
    @PerformanceMonitor(threshold = 2000, metricName = "user_login")
    public ResponseEntity<ApiResponse<JwtToken>> login(@RequestBody @Valid LoginRequest request) {
        try {
            JwtToken token = authenticationService.login(request);
            return ResponseEntity.ok(ApiResponse.success(token, "로그인에 성공했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "OTP 필요 여부 확인", description = "로그인 시 OTP가 필요한지 확인합니다.")
    @PostMapping("/login/check-otp")
    @LogExecution("OTP 필요 여부 확인")
    @PerformanceMonitor(threshold = 1000, metricName = "check_otp_required")
    public ResponseEntity<ApiResponse<JwtToken>> checkOtpRequired(@RequestBody @Valid LoginRequest request) {
        try {
            ApiResponse<JwtToken> response = otpAuthenticationService.checkOtpRequired(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("OTP 확인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "OTP 로그인", description = "OTP 코드와 함께 로그인합니다.")
    @PostMapping("/login/otp")
    @LogExecution("OTP 로그인")
    @PerformanceMonitor(threshold = 2000, metricName = "otp_login")
    public ResponseEntity<ApiResponse<JwtToken>> loginWithOtp(@RequestBody @Valid OtpLoginRequest request) {
        try {
            ApiResponse<JwtToken> response = otpAuthenticationService.loginWithOtp(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("OTP 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "OTP 설정", description = "사용자의 OTP를 설정하고 QR 코드를 생성합니다.")
    @PostMapping("/otp/setup")
    @LogExecution("OTP 설정")
    @PerformanceMonitor(threshold = 2000, metricName = "otp_setup")
    public ResponseEntity<ApiResponse<OtpSetupResponse>> setupOtp(@RequestBody @Valid OtpSetupRequest request) {
        try {
            OtpSetupResponse response = otpAuthenticationService.setupOtp(request);
            return ResponseEntity.ok(ApiResponse.success(response, "OTP 설정이 완료되었습니다"));
        } catch (Exception e) {
            throw new RuntimeException("OTP 설정 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "OTP 검증", description = "OTP 코드를 검증하여 활성화합니다.")
    @PostMapping("/otp/verify")
    @LogExecution("OTP 검증")
    @PerformanceMonitor(threshold = 1000, metricName = "otp_verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(@RequestBody @Valid OtpVerifyRequest request) {
        try {
            OtpVerifyResponse response = otpAuthenticationService.verifyOtp(request);
            return ResponseEntity
                    .ok(ApiResponse.success(response, response.isSuccess() ? "OTP 검증에 성공했습니다" : "OTP 검증에 실패했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("OTP 검증 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "OTP 비활성화", description = "OTP를 비활성화합니다.")
    @PostMapping("/otp/disable")
    @LogExecution("OTP 비활성화")
    @PerformanceMonitor(threshold = 1000, metricName = "otp_disable")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> disableOtp(@RequestBody @Valid OtpVerifyRequest request) {
        try {
            OtpVerifyResponse response = otpAuthenticationService.disableOtp(request);
            return ResponseEntity
                    .ok(ApiResponse.success(response, response.isSuccess() ? "OTP 비활성화에 성공했습니다" : "OTP 비활성화에 실패했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("OTP 비활성화 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "사용자 회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/register")
    @LogExecution("사용자 회원가입")
    @PerformanceMonitor(threshold = 3000, metricName = "user_registration")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody @Valid UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(request);
            return ResponseEntity.ok(ApiResponse.success(user, "회원가입에 성공했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    @LogExecution("토큰 갱신")
    @PerformanceMonitor(threshold = 1000, metricName = "token_refresh")
    public ResponseEntity<ApiResponse<JwtToken>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        try {
            JwtToken token = authenticationService.refresh(request);
            return ResponseEntity.ok(ApiResponse.success(token, "토큰 갱신에 성공했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("토큰 갱신 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "사용자 로그아웃", description = "현재 사용자를 로그아웃합니다.")
    @PostMapping("/logout")
    @LogExecution("사용자 로그아웃")
    @PerformanceMonitor(threshold = 500, metricName = "user_logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentUser AuthenticatedUser currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃에 성공했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    @LogExecution("현재 사용자 정보 조회")
    @PerformanceMonitor(threshold = 1000, metricName = "current_user_info")
    public ResponseEntity<ApiResponse<AuthenticatedUser>> getCurrentUser(@CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(currentUser, "현재 사용자 정보를 성공적으로 조회했습니다"));
    }
}