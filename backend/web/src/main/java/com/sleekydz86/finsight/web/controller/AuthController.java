package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.*;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.auth.service.OtpAuthenticationService;
import com.sleekydz86.finsight.core.auth.service.SocialAuthService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.BaseException;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.AuthProvider;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "인증", description = "사용자 인증 및 OTP 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final OtpAuthenticationService otpAuthenticationService;
    private final SocialAuthService socialAuthService;
    private final UserPersistencePort userPersistencePort;

    public AuthController(AuthenticationService authenticationService,
            UserService userService,
            OtpAuthenticationService otpAuthenticationService,
            SocialAuthService socialAuthService,
            UserPersistencePort userPersistencePort) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.otpAuthenticationService = otpAuthenticationService;
        this.socialAuthService = socialAuthService;
        this.userPersistencePort = userPersistencePort;
    }

    @Operation(summary = "사용자 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    @LogExecution("사용자 로그인")
    @PerformanceMonitor(threshold = 2000, metricName = "user_login")
    public ResponseEntity<ApiResponse<LoginResultResponse>> login(@RequestBody @Valid LoginRequest request) {
        try {
            JwtToken token = authenticationService.login(request);
            User user = userPersistencePort.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            LoginResultResponse result = socialAuthService.toWebLoginResult(user, token);
            return ResponseEntity.ok(ApiResponse.success(result, "로그인에 성공했습니다"));
        } catch (Exception e) {
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "SNS 인가 URL 조회", description = "카카오/네이버/구글 로그인 인가 URL을 반환합니다.")
    @GetMapping("/oauth/{provider}/url")
    @LogExecution("SNS 인가 URL 조회")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOAuthAuthorizeUrl(
            @PathVariable String provider) {
        AuthProvider authProvider = parseProvider(provider);
        if (authProvider == AuthProvider.NAVER) {
            return ResponseEntity.ok(ApiResponse.success(
                    socialAuthService.createNaverAuthorizeUrl(),
                    "네이버 인가 URL을 생성했습니다"));
        }
        if (authProvider == AuthProvider.KAKAO) {
            return ResponseEntity.ok(ApiResponse.success(
                    socialAuthService.createKakaoAuthorizeUrl(),
                    "카카오 인가 URL을 생성했습니다"));
        }
        if (authProvider == AuthProvider.GOOGLE) {
            return ResponseEntity.ok(ApiResponse.success(
                    socialAuthService.createGoogleAuthorizeUrl(),
                    "구글 인가 URL을 생성했습니다"));
        }
        throw new IllegalArgumentException("지원하지 않는 SNS 제공자입니다: " + provider);
    }

    @Operation(summary = "네이버 로그인", description = "네이버 인가 코드로 로그인합니다.")
    @PostMapping("/oauth/naver")
    @LogExecution("네이버 로그인")
    @PerformanceMonitor(threshold = 3000, metricName = "naver_login")
    public ResponseEntity<ApiResponse<LoginResultResponse>> loginWithNaver(
            @RequestBody @Valid SocialOAuthCodeRequest request) {
        LoginResultResponse result = socialAuthService.loginWithNaver(request.getCode(), request.getState());
        return ResponseEntity.ok(ApiResponse.success(result, "네이버 로그인에 성공했습니다"));
    }

    @Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인합니다.")
    @PostMapping("/oauth/kakao")
    @LogExecution("카카오 로그인")
    @PerformanceMonitor(threshold = 3000, metricName = "kakao_login")
    public ResponseEntity<ApiResponse<LoginResultResponse>> loginWithKakao(
            @RequestBody @Valid SocialOAuthCodeRequest request) {
        LoginResultResponse result = socialAuthService.loginWithKakao(request.getCode(), request.getState());
        return ResponseEntity.ok(ApiResponse.success(result, "카카오 로그인에 성공했습니다"));
    }

    @Operation(summary = "구글 로그인", description = "구글 인가 코드로 로그인합니다.")
    @PostMapping("/oauth/google")
    @LogExecution("구글 로그인")
    @PerformanceMonitor(threshold = 3000, metricName = "google_login")
    public ResponseEntity<ApiResponse<LoginResultResponse>> loginWithGoogle(
            @RequestBody @Valid SocialOAuthCodeRequest request) {
        LoginResultResponse result = socialAuthService.loginWithGoogle(request.getCode(), request.getState());
        return ResponseEntity.ok(ApiResponse.success(result, "구글 로그인에 성공했습니다"));
    }

    @Operation(summary = "네이버 연결 끊기 콜백", description = "네이버 연결 끊기 알림을 처리합니다.")
    @RequestMapping(value = "/oauth/naver/unlink", method = { RequestMethod.GET, RequestMethod.POST })
    @LogExecution("네이버 연결 끊기")
    public ResponseEntity<String> unlinkNaver(
            @RequestParam(value = "uid", required = false) String uid,
            @RequestParam(value = "user_id", required = false) String userId) {
        String naverId = uid != null && !uid.isBlank() ? uid : userId;
        socialAuthService.unlinkNaverAccount(naverId);
        return ResponseEntity.ok("SUCCESS");
    }

    @Operation(summary = "카카오 연결 끊기 콜백", description = "카카오 연결 끊기 알림을 처리합니다.")
    @RequestMapping(value = "/oauth/kakao/unlink", method = { RequestMethod.GET, RequestMethod.POST })
    @LogExecution("카카오 연결 끊기")
    public ResponseEntity<String> unlinkKakao(
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "id", required = false) String id) {
        String kakaoUserId = userId != null && !userId.isBlank() ? userId : id;
        socialAuthService.unlinkKakaoAccount(kakaoUserId);
        return ResponseEntity.ok("SUCCESS");
    }

    private AuthProvider parseProvider(String provider) {
        try {
            return AuthProvider.valueOf(provider.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("지원하지 않는 SNS 제공자입니다: " + provider);
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
        } catch (BaseException e) {
            throw e;
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
    public ResponseEntity<ApiResponse<AuthenticatedUser>> getCurrentUser(
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("로그인이 필요합니다", 401));
        }
        return ResponseEntity.ok(ApiResponse.success(currentUser, "현재 사용자 정보를 성공적으로 조회했습니다"));
    }
}
