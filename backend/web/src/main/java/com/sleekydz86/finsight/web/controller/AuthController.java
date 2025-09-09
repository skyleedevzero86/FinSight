package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.LoginRequest;
import com.sleekydz86.finsight.core.auth.dto.RefreshTokenRequest;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

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

    @GetMapping("/me")
    @LogExecution("현재 사용자 정보 조회")
    @PerformanceMonitor(threshold = 1000, metricName = "current_user_info")
    public ResponseEntity<ApiResponse<AuthenticatedUser>> getCurrentUser(@CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(currentUser, "현재 사용자 정보를 성공적으로 조회했습니다"));
    }
}