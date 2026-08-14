package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.adapter.oauth.NaverOAuthClient;
import com.sleekydz86.finsight.core.auth.config.NaverOAuthProperties;
import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.LoginResultResponse;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.exception.AuthenticationFailedException;
import com.sleekydz86.finsight.core.user.domain.AuthProvider;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SocialAuthService {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthService.class);

    private final NaverOAuthProperties naverOAuthProperties;
    private final NaverOAuthClient naverOAuthClient;
    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public SocialAuthService(
            NaverOAuthProperties naverOAuthProperties,
            NaverOAuthClient naverOAuthClient,
            UserPersistencePort userPersistencePort,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.naverOAuthProperties = naverOAuthProperties;
        this.naverOAuthClient = naverOAuthClient;
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public Map<String, String> createNaverAuthorizeUrl() {
        if (!naverOAuthProperties.isConfigured()) {
            throw new IllegalStateException("네이버 로그인 설정(NAVER_CLIENT_ID/SECRET)이 없습니다");
        }
        String state = naverOAuthClient.createState();
        return Map.of(
                "provider", AuthProvider.NAVER.name(),
                "authorizeUrl", naverOAuthClient.createAuthorizeUrl(state),
                "state", state);
    }

    public LoginResultResponse loginWithNaver(String code, String state) {
        if (!naverOAuthProperties.isConfigured()) {
            throw new IllegalStateException("네이버 로그인 설정(NAVER_CLIENT_ID/SECRET)이 없습니다");
        }
        try {
            NaverOAuthClient.NaverTokenResponse tokenResponse = naverOAuthClient.exchangeCode(code, state);
            NaverOAuthClient.NaverProfileResponse profile = naverOAuthClient.fetchProfile(tokenResponse.getAccessToken());
            User user = findOrCreateNaverUser(profile);
            user.updateLastLoginAt(LocalDateTime.now());
            user = userPersistencePort.save(user);

            JwtToken jwt = issueToken(user);
            log.info("네이버 로그인 성공: email={}, naverId={}", user.getEmail(), profile.getId());
            return LoginResultResponse.of(
                    jwt,
                    AuthProvider.NAVER,
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl());
        } catch (Exception e) {
            log.error("네이버 로그인 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("naver");
        }
    }

    public LoginResultResponse toWebLoginResult(User user, JwtToken token) {
        AuthProvider provider = user.getAuthProvider() != null ? user.getAuthProvider() : AuthProvider.WEB;
        return LoginResultResponse.of(
                token,
                provider,
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl());
    }

    public void unlinkNaverAccount(String naverId) {
        if (naverId == null || naverId.isBlank()) {
            return;
        }
        userPersistencePort.findByNaverId(naverId).ifPresent(user -> {
            user.clearNaverLink();
            userPersistencePort.save(user);
            log.info("네이버 연결 해제 처리 완료: userId={}, naverId={}", user.getId(), naverId);
        });
    }

    private User findOrCreateNaverUser(NaverOAuthClient.NaverProfileResponse profile) {
        Optional<User> byNaverId = userPersistencePort.findByNaverId(profile.getId());
        if (byNaverId.isPresent()) {
            User existing = byNaverId.get();
            existing.applyNaverProfile(profile.getNickname(), profile.getName(), profile.getEmail(),
                    profile.getProfileImage(), profile.getMobile());
            return existing;
        }

        String email = resolveEmail(profile);
        Optional<User> byEmail = userPersistencePort.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getAuthProvider() != null
                    && existing.getAuthProvider() != AuthProvider.WEB
                    && existing.getAuthProvider() != AuthProvider.NAVER) {
                throw new IllegalStateException("이미 다른 SNS로 가입된 이메일입니다: " + existing.getAuthProvider());
            }
            existing.linkNaver(profile.getId());
            existing.applyNaverProfile(profile.getNickname(), profile.getName(), profile.getEmail(),
                    profile.getProfileImage(), profile.getMobile());
            return existing;
        }

        String nickname = resolveNickname(profile);
        String username = uniqueUsername("naver_" + profile.getId());
        LocalDateTime now = LocalDateTime.now();

        return User.builder()
                .username(username)
                .password(passwordEncoder.encode("SNS-" + UUID.randomUUID()))
                .nickname(truncate(nickname, 50))
                .email(email)
                .status(UserStatus.APPROVED)
                .role(UserRole.USER)
                .authProvider(AuthProvider.NAVER)
                .naverId(profile.getId())
                .profileImageUrl(profile.getProfileImage())
                .phoneNumber(profile.getMobile())
                .approvedAt(now)
                .passwordChangedAt(now)
                .passwordChangeCount(0)
                .loginFailCount(0)
                .otpEnabled(false)
                .otpVerified(false)
                .build();
    }

    private JwtToken issueToken(User user) {
        String accessToken = jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getEmail());
        return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpiration() / 1000))
                .build();
    }

    private String resolveEmail(NaverOAuthClient.NaverProfileResponse profile) {
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            return profile.getEmail().trim();
        }
        return "naver_" + profile.getId() + "@oauth.finsight.local";
    }

    private String resolveNickname(NaverOAuthClient.NaverProfileResponse profile) {
        if (profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname().trim();
        }
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName().trim();
        }
        return "네이버사용자";
    }

    private String uniqueUsername(String preferred) {
        String base = truncate(preferred.replaceAll("[^a-zA-Z0-9_]", "_"), 40);
        if (!userPersistencePort.existsByUsername(base)) {
            return base;
        }
        for (int i = 1; i < 1000; i++) {
            String candidate = truncate(base, 40) + "_" + i;
            if (!userPersistencePort.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return truncate(base, 30) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
