package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.*;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.OtpNotEnabledException;
import com.sleekydz86.finsight.core.global.exception.OtpVerificationFailedException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpAuthenticationService {

    private final OtpService otpService;
    private final UserPersistencePort userPersistencePort;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    public OtpSetupResponse setupOtp(OtpSetupRequest request) {
        log.info("OTP 설정 요청: {}", request.getEmail());

        User user = userPersistencePort.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (user.getOtpEnabled() && user.getOtpVerified()) {
            throw new RuntimeException("이미 OTP가 활성화되어 있습니다.");
        }

        String secret = otpService.generateSecretKey();
        String qrCode = otpService.generateQRCodeImage(user.getUsername(), secret);

        user.enableOtp(secret);
        userPersistencePort.save(user);

        log.info("OTP 설정 완료: {}", request.getEmail());

        return new OtpSetupResponse(secret, "data:image/png;base64," + qrCode,
                "QR 코드를 스캔하고 6자리 코드를 입력하여 OTP를 활성화하세요.");
    }

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        log.info("OTP 검증 요청: {}", request.getEmail());

        User user = userPersistencePort.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!user.getOtpEnabled()) {
            throw new OtpNotEnabledException(request.getEmail());
        }

        if (otpService.verifyCode(user.getOtpSecret(), request.getOtpCode())) {
            user.verifyOtp();
            userPersistencePort.save(user);

            log.info("OTP 검증 성공: {}", request.getEmail());
            return new OtpVerifyResponse(true, "OTP가 성공적으로 활성화되었습니다.");
        } else {
            log.warn("OTP 검증 실패: {}", request.getEmail());
            return new OtpVerifyResponse(false, "잘못된 OTP 코드입니다.");
        }
    }

    public ApiResponse<JwtToken> loginWithOtp(OtpLoginRequest request) {
        log.info("OTP 로그인 요청: {}", request.getEmail());

        User user = userPersistencePort.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!user.getOtpEnabled() || !user.getOtpVerified()) {
            throw new OtpNotEnabledException(request.getEmail());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        if (!otpService.verifyCode(user.getOtpSecret(), request.getOtpCode())) {
            throw new OtpVerificationFailedException(request.getEmail());
        }

        String accessToken = jwtTokenUtil.generateAccessToken(request.getEmail(), user.getRole());
        String refreshToken = jwtTokenUtil.generateRefreshToken(request.getEmail());

        JwtToken token = JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpiration() / 1000))
                .build();

        user.updateLastLoginAt(LocalDateTime.now());
        userPersistencePort.save(user);

        log.info("OTP 로그인 성공: {}", request.getEmail());

        return ApiResponse.success(token, "OTP 로그인에 성공했습니다.");
    }

    public OtpVerifyResponse disableOtp(OtpVerifyRequest request) {
        log.info("OTP 비활성화 요청: {}", request.getEmail());

        User user = userPersistencePort.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!user.getOtpEnabled()) {
            throw new OtpNotEnabledException(request.getEmail());
        }

        if (otpService.verifyCode(user.getOtpSecret(), request.getOtpCode())) {
            user.disableOtp();
            userPersistencePort.save(user);

            log.info("OTP 비활성화 성공: {}", request.getEmail());
            return new OtpVerifyResponse(true, "OTP가 비활성화되었습니다.");
        } else {
            log.warn("OTP 비활성화 실패: {}", request.getEmail());
            return new OtpVerifyResponse(false, "잘못된 OTP 코드입니다.");
        }
    }

    public ApiResponse<JwtToken> checkOtpRequired(LoginRequest request) {
        log.info("OTP 필요 여부 확인: {}", request.getEmail());

        User user = userPersistencePort.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        if (user.isOtpRequired()) {
            log.info("OTP 필요: {}", request.getEmail());
            return ApiResponse.success(null, "OTP 코드를 입력하세요.");
        } else {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtTokenUtil.generateAccessToken(request.getEmail(), user.getRole());
            String refreshToken = jwtTokenUtil.generateRefreshToken(request.getEmail());

            JwtToken token = JwtToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpiration() / 1000))
                    .build();

            user.updateLastLoginAt(LocalDateTime.now());
            userPersistencePort.save(user);

            log.info("일반 로그인 성공: {}", request.getEmail());
            return ApiResponse.success(token, "로그인에 성공했습니다.");
        }
    }
}