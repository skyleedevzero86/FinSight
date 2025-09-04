package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.dto.LoginRequest;
import com.sleekydz86.finsight.core.auth.dto.RefreshTokenRequest;
import com.sleekydz86.finsight.core.auth.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.exception.*;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.service.PasswordValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.AuthenticationException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtTokenUtil jwtTokenUtil,
                                 UserJpaRepository userJpaRepository,
                                 PasswordEncoder passwordEncoder,
                                 PasswordValidationService passwordValidationService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    public JwtToken login(LoginRequest request) {
        try {
            PasswordValidationService.PasswordValidationResult validationResult = passwordValidationService
                    .validatePassword(request.getPassword());
            if (!validationResult.isValid()) {
                log.warn("비밀번호 유효성 검증 실패: {}", validationResult.getErrors());
                throw new AuthenticationFailedException(request.getEmail());
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserJpaEntity userEntity = userJpaRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

            String accessToken = jwtTokenUtil.generateAccessToken(request.getEmail(), userEntity.getRole());
            String refreshToken = jwtTokenUtil.generateRefreshToken(request.getEmail());

            updateLastLoginTime(request.getEmail());

            log.info("로그인 성공: {}", request.getEmail());

            return JwtToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpiration() / 1000))
                    .build();

        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException(request.getEmail());
        }
    }

    public JwtToken refresh(RefreshTokenRequest request) {
        log.debug("토큰 갱신 시도");

        try {
            if (!jwtTokenUtil.validateRefreshToken(request.getRefreshToken())) {
                log.warn("유효하지 않은 리프레시 토큰");
                throw new InvalidTokenException("REFRESH");
            }

            String email = jwtTokenUtil.getEmailFromRefreshToken(request.getRefreshToken());

            if (!userJpaRepository.existsByEmail(email)) {
                log.warn("존재하지 않는 사용자: {}", email);
                throw new InvalidTokenException("REFRESH");
            }

            UserJpaEntity userEntity = userJpaRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));

            String newAccessToken = jwtTokenUtil.generateAccessToken(email, userEntity.getRole());
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(email);

            log.info("토큰 갱신 성공: {}", email);

            return JwtToken.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpiration() / 1000))
                    .build();

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new InvalidTokenException("REFRESH");
        }
    }

    public User register(UserRegistrationRequest request) {
        log.info("사용자 등록 시도: {}", request.getEmail());

        if (userJpaRepository.existsByEmail(request.getEmail())) {
            log.warn("이미 존재하는 이메일: {}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }

        PasswordValidationService.PasswordValidationResult validationResult = passwordValidationService
                .validatePassword(request.getPassword());
        if (!validationResult.isValid()) {
            log.warn("비밀번호 유효성 검증 실패: {}", validationResult.getErrors());
            throw new IllegalArgumentException(
                    "비밀번호가 요구사항을 충족하지 않습니다: " + String.join(", ", validationResult.getErrors()));
        }

        UserJpaEntity userEntity = new UserJpaEntity();
        userEntity.setEmail(request.getEmail());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setUsername(request.getUsername());
        userEntity.setRole(UserRole.USER);
        userEntity.setActive(true);
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());

        if (request.getWatchlist() != null && !request.getWatchlist().isEmpty()) {
            userEntity.setWatchlist(request.getWatchlist());
        } else {
            userEntity.setWatchlist(Arrays.asList(
                    com.sleekydz86.finsight.core.news.domain.vo.TargetCategory.SPY,
                    com.sleekydz86.finsight.core.news.domain.vo.TargetCategory.QQQ));
        }

        userEntity.setNotificationPreferences(Arrays.asList(NotificationType.EMAIL));

        UserJpaEntity savedEntity = userJpaRepository.save(userEntity);

        log.info("사용자 등록 성공: {} (ID: {})", request.getEmail(), savedEntity.getId());

        return convertToDomain(savedEntity);
    }

    private void updateLastLoginTime(String email) {
        userJpaRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userJpaRepository.save(user);
        });
    }

    private User convertToDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .username(entity.getUsername())
                .role(entity.getRole())
                .active(entity.isActive())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .watchlist(entity.getWatchlist())
                .notificationPreferences(entity.getNotificationPreferences())
                .build();
    }

    public JwtToken refreshToken(String refreshToken) {
        try {
            if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
                throw new InvalidTokenException("refresh");
            }

            String email = jwtTokenUtil.getEmailFromRefreshToken(refreshToken);
            UserJpaEntity userEntity = userJpaRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));

            UserRole role = userEntity.getRole();
            String newAccessToken = jwtTokenUtil.generateAccessToken(email, role);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(email);

            return JwtToken.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getExpirationPeriod() / 1000))
                    .build();

        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            throw new InvalidTokenException("refresh");
        }
    }

    public JwtToken authenticate(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserRole role = UserRole.valueOf(userDetails.getAuthorities().iterator().next().getAuthority());

            String accessToken = jwtTokenUtil.generateAccessToken(email, role);
            String refreshToken = jwtTokenUtil.generateRefreshToken(email);

            updateLastLoginTime(email);

            return JwtToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getExpirationPeriod() / 1000))
                    .build();

        } catch (AuthenticationException e) {
            log.error("로그인 실패: {}", email, e);
            throw new AuthenticationFailedException(email);
        }
    }
}