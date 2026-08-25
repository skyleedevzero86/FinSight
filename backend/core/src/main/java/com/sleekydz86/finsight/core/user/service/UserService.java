package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.AuthProvider;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.auth.email.EmailVerificationService;
import com.sleekydz86.finsight.core.user.domain.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserCommandUseCase, UserQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserPersistencePort userPersistencePort,
            PasswordEncoder passwordEncoder,
            PasswordValidationService passwordValidationService,
            EmailVerificationService emailVerificationService,
            ApplicationEventPublisher eventPublisher) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
        this.emailVerificationService = emailVerificationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(value = "userCache", key = "#request.email")
    public User registerUser(UserRegistrationRequest request) {
        log.info("신규 사용자 등록 시작: 이메일={}", request.getEmail());

        if (userPersistencePort.existsByEmail(request.getEmail())) {
            log.warn("사용자 등록 실패: 이미 존재하는 이메일 - {}", request.getEmail());
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        emailVerificationService.requirePassedSignup(request.getEmail());

        PasswordValidationService.PasswordValidationResult validationResult = passwordValidationService
                .validatePassword(request.getPassword());

        if (!validationResult.isValid()) {
            log.warn("사용자 등록 실패: 비밀번호가 유효하지 않음 - 이메일={}", request.getEmail());
            throw new InvalidPasswordException(validationResult.getErrors());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .username(request.getUsername())
                .nickname(request.getUsername())
                .role(UserRole.USER)
                .status(UserStatus.APPROVED)
                .authProvider(AuthProvider.WEB)
                .passwordChangedAt(LocalDateTime.now())
                .notificationPreferences(Arrays.asList(NotificationType.EMAIL))
                .build();

        User savedUser = userPersistencePort.save(newUser);
        emailVerificationService.consumeSignupVerification(request.getEmail());
        LocalDateTime registeredAt = savedUser.getCreatedAt() != null
                ? savedUser.getCreatedAt()
                : LocalDateTime.now();
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId(), registeredAt));
        log.info("사용자 등록 완료: ID={}", savedUser.getId());

        return savedUser;
    }

    @Override
    @Cacheable(value = "userCache", key = "#userId")
    public Optional<User> findById(Long userId) {
        log.debug("사용자 ID로 조회: {}", userId);
        return userPersistencePort.findById(userId);
    }

    @Override
    @Cacheable(value = "userCache", key = "#email")
    public Optional<User> findByEmail(String email) {
        log.debug("이메일로 사용자 조회: {}", email);
        return userPersistencePort.findByEmail(email);
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public User updateUser(Long userId, UserUpdateRequest request) {
        log.info("사용자 정보 수정: ID={}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            PasswordValidationService.PasswordValidationResult validationResult = passwordValidationService
                    .validatePassword(request.getPassword());

            if (!validationResult.isValid()) {
                log.warn("사용자 수정 실패: 비밀번호가 유효하지 않음 - ID={}", userId);
                throw new InvalidPasswordException(validationResult.getErrors());
            }

            user.changePassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getUsername() != null) {
            String nickname = request.getNickname() != null ? request.getNickname() : request.getUsername();
            String email = request.getEmail() != null ? request.getEmail() : user.getEmail();
            user.updateProfile(nickname, email);
        }

        User updatedUser = userPersistencePort.save(user);
        log.info("사용자 정보 수정 완료: ID={}", userId);

        return updatedUser;
    }

    @Override
    @CacheEvict(value = { "userCache", "user", "userProfile" }, key = "#userId")
    public void updateWatchlist(Long userId, WatchlistUpdateRequest request) {
        log.info("관심종목 수정: 사용자ID={}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        if (request.getCategories() != null) {
            user.updateWatchlist(request.getCategories());
            userPersistencePort.save(user);
            log.info("관심종목 수정 완료: 사용자ID={}", userId);
        }
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void updateNotificationPreferences(Long userId, List<NotificationType> preferences) {
        log.info("알림 설정 수정: 사용자ID={}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        if (preferences != null) {
            user.updateNotificationPreferences(preferences);
            userPersistencePort.save(user);
            log.info("알림 설정 수정 완료: 사용자ID={}", userId);
        }
    }

    @Override
    @Cacheable(value = "userCache", key = "'watchlist_' + #userId")
    public List<TargetCategory> getUserWatchlist(Long userId) {
        log.debug("관심종목 조회: 사용자ID={}", userId);
        return userPersistencePort.findById(userId)
                .map(User::getWatchlist)
                .orElse(List.of());
    }

    @Override
    @Cacheable(value = "userCache", key = "'notifications_' + #userId")
    public List<NotificationType> getUserNotificationPreferences(Long userId) {
        log.debug("알림 설정 조회: 사용자ID={}", userId);
        return userPersistencePort.findById(userId)
                .map(User::getNotificationPreferences)
                .orElse(List.of());
    }

    @CacheEvict(value = "userCache", key = "#userId")
    public void approveUser(Long userId, Long approverId) {
        log.info("사용자 승인: ID={}, 승인자ID={}", userId, approverId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        user.approve(approverId);
        userPersistencePort.save(user);
        log.info("사용자 승인 완료: ID={}", userId);
    }

    @CacheEvict(value = "userCache", key = "#userId")
    public void suspendUser(Long userId) {
        log.info("사용자 정지: ID={}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        user.suspend();
        userPersistencePort.save(user);
        log.info("사용자 정지 완료: ID={}", userId);
    }

    @CacheEvict(value = "userCache", key = "#userId")
    public void unlockUser(Long userId) {
        log.info("사용자 잠금 해제: ID={}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        user.unlock();
        userPersistencePort.save(user);
        log.info("사용자 잠금 해제 완료: ID={}", userId);
    }

    public Optional<User> findByEmailAndUsername(String email, String username) {
        return userPersistencePort.findByEmailAndUsername(email, username);
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        User user = findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);

        userPersistencePort.save(user);

        log.info("비밀번호 업데이트 완료 - 사용자: {}", user.getUsername());
    }

}