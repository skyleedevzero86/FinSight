package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.adapter.oauth.KakaoOAuthClient;
import com.sleekydz86.finsight.core.auth.adapter.oauth.KakaoOAuthTokenResponse;
import com.sleekydz86.finsight.core.auth.adapter.oauth.KakaoProfileResponse;
import com.sleekydz86.finsight.core.auth.adapter.oauth.NaverOAuthClient;
import com.sleekydz86.finsight.core.auth.adapter.oauth.NaverProfileResponse;
import com.sleekydz86.finsight.core.auth.adapter.oauth.NaverTokenResponse;
import com.sleekydz86.finsight.core.auth.config.KakaoOAuthProperties;
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
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public SocialAuthService(
            NaverOAuthProperties naverOAuthProperties,
            NaverOAuthClient naverOAuthClient,
            KakaoOAuthProperties kakaoOAuthProperties,
            KakaoOAuthClient kakaoOAuthClient,
            UserPersistencePort userPersistencePort,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.naverOAuthProperties = naverOAuthProperties;
        this.naverOAuthClient = naverOAuthClient;
        this.kakaoOAuthProperties = kakaoOAuthProperties;
        this.kakaoOAuthClient = kakaoOAuthClient;
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

    public Map<String, String> createKakaoAuthorizeUrl() {
        if (!kakaoOAuthProperties.isConfigured()) {
            throw new IllegalStateException("카카오 로그인 설정(KAKAO_CLIENT_ID/SECRET)이 없습니다");
        }
        String state = kakaoOAuthClient.createState();
        return Map.of(
                "provider", AuthProvider.KAKAO.name(),
                "authorizeUrl", kakaoOAuthClient.createAuthorizeUrl(state),
                "state", state);
    }

    public LoginResultResponse loginWithNaver(String code, String state) {
        if (!naverOAuthProperties.isConfigured()) {
            throw new IllegalStateException("네이버 로그인 설정(NAVER_CLIENT_ID/SECRET)이 없습니다");
        }
        try {
            NaverTokenResponse tokenResponse = naverOAuthClient.exchangeCode(code, state);
            NaverProfileResponse profile = naverOAuthClient.fetchProfile(tokenResponse.getAccessToken());
            User user = findOrCreateNaverUser(profile);
            user.updateLastLoginAt(LocalDateTime.now());
            user = userPersistencePort.save(user);

            JwtToken jwt = issueToken(user);
            log.info("네이버 로그인 성공: 이메일={}, 네이버ID={}", user.getEmail(), profile.getId());
            return LoginResultResponse.of(
                    jwt,
                    AuthProvider.NAVER,
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl());
        } catch (Exception e) {
            log.error("네이버 로그인 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("네이버");
        }
    }

    public LoginResultResponse loginWithKakao(String code, String state) {
        if (!kakaoOAuthProperties.isConfigured()) {
            throw new IllegalStateException("카카오 로그인 설정(KAKAO_CLIENT_ID/SECRET)이 없습니다");
        }
        try {
            KakaoOAuthTokenResponse tokenResponse = kakaoOAuthClient.exchangeCode(code);
            KakaoProfileResponse profile = kakaoOAuthClient.fetchProfile(tokenResponse.getAccessToken());
            User user = findOrCreateKakaoUser(profile, tokenResponse);
            user.updateLastLoginAt(LocalDateTime.now());
            user = userPersistencePort.save(user);

            JwtToken jwt = issueToken(user);
            log.info("카카오 로그인 성공: 이메일={}, 카카오ID={}, state={}",
                    user.getEmail(), profile.getIdAsString(), state);
            return LoginResultResponse.of(
                    jwt,
                    AuthProvider.KAKAO,
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl());
        } catch (Exception e) {
            log.error("카카오 로그인 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("카카오");
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
            log.info("네이버 연결 해제 처리 완료: 사용자ID={}, 네이버ID={}", user.getId(), naverId);
        });
    }

    public void unlinkKakaoAccount(String kakaoUserId) {
        if (kakaoUserId == null || kakaoUserId.isBlank()) {
            return;
        }
        userPersistencePort.findByKakaoUserId(kakaoUserId).ifPresent(user -> {
            user.clearKakaoLink();
            userPersistencePort.save(user);
            log.info("카카오 연결 해제 처리 완료: 사용자ID={}, 카카오ID={}", user.getId(), kakaoUserId);
        });
    }

    private User findOrCreateKakaoUser(KakaoProfileResponse profile, KakaoOAuthTokenResponse tokenResponse) {
        String kakaoUserId = profile.getIdAsString();
        Optional<User> byKakaoId = userPersistencePort.findByKakaoUserId(kakaoUserId);
        if (byKakaoId.isPresent()) {
            User existing = byKakaoId.get();
            existing.applyKakaoProfile(profile.getNickname(), profile.getEmail(), profile.getProfileImageUrl());
            applyKakaoTokens(existing, kakaoUserId, tokenResponse);
            return existing;
        }

        String email = resolveKakaoEmail(profile);
        Optional<User> byEmail = userPersistencePort.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getAuthProvider() != null
                    && existing.getAuthProvider() != AuthProvider.WEB
                    && existing.getAuthProvider() != AuthProvider.KAKAO) {
                throw new IllegalStateException("이미 다른 SNS로 가입된 이메일입니다: " + existing.getAuthProvider());
            }
            existing.linkKakaoLogin(kakaoUserId);
            existing.applyKakaoProfile(profile.getNickname(), profile.getEmail(), profile.getProfileImageUrl());
            applyKakaoTokens(existing, kakaoUserId, tokenResponse);
            return existing;
        }

        String nickname = resolveKakaoNickname(profile);
        String username = uniqueUsername("kakao_" + kakaoUserId);
        LocalDateTime now = LocalDateTime.now();

        User created = User.builder()
                .username(username)
                .password(passwordEncoder.encode("SNS-" + UUID.randomUUID()))
                .nickname(truncate(nickname, 50))
                .email(email)
                .status(UserStatus.APPROVED)
                .role(UserRole.USER)
                .authProvider(AuthProvider.KAKAO)
                .kakaoUserId(kakaoUserId)
                .profileImageUrl(profile.getProfileImageUrl())
                .approvedAt(now)
                .passwordChangedAt(now)
                .passwordChangeCount(0)
                .loginFailCount(0)
                .otpEnabled(false)
                .otpVerified(false)
                .build();
        applyKakaoTokens(created, kakaoUserId, tokenResponse);
        return created;
    }

    private void applyKakaoTokens(User user, String kakaoUserId, KakaoOAuthTokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            return;
        }
        LocalDateTime expiresAt = null;
        if (tokenResponse.getExpiresIn() != null) {
            expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
        }
        user.updateKakaoInfo(kakaoUserId, tokenResponse.getAccessToken(), expiresAt, tokenResponse.getRefreshToken());
    }

    private User findOrCreateNaverUser(NaverProfileResponse profile) {
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

    private String resolveEmail(NaverProfileResponse profile) {
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            return profile.getEmail().trim();
        }
        return "naver_" + profile.getId() + "@oauth.finsight.local";
    }

    private String resolveKakaoEmail(KakaoProfileResponse profile) {
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            return profile.getEmail().trim();
        }
        return "kakao_" + profile.getIdAsString() + "@oauth.finsight.local";
    }

    private String resolveNickname(NaverProfileResponse profile) {
        if (profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname().trim();
        }
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName().trim();
        }
        return "네이버사용자";
    }

    private String resolveKakaoNickname(KakaoProfileResponse profile) {
        if (profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname().trim();
        }
        return "카카오사용자";
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
