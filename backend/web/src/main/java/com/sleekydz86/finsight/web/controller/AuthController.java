package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.LoginRequest;
import com.sleekydz86.finsight.core.auth.dto.RefreshTokenRequest;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtToken>> login(@RequestBody @Valid LoginRequest request) {
        JwtToken token = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.success(token, "로그인 성공"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody @Valid UserRegistrationRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "회원가입 성공"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtToken>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        JwtToken token = authenticationService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(token, "토큰 갱신 성공"));
    }
}