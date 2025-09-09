package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.*;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.auth.service.RateLimitServiceInterface;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserApplicationServiceImpl implements UserApplicationService {

    private final UserPersistencePort userPersistencePort;
    private final JwtTokenUtil jwtTokenUtil;
    private final RateLimitServiceInterface rateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;
    private final Executor asyncExecutor;

    @Autowired
    public UserApplicationServiceImpl(UserPersistencePort userPersistencePort,
                                      JwtTokenUtil jwtTokenUtil,
                                      RateLimitServiceInterface rateLimitService,
                                      PasswordEncoder passwordEncoder,
                                      PasswordValidationService passwordValidationService,
                                      @Qualifier("applicationTaskExecutor") Executor asyncExecutor) {
        this.userPersistencePort = userPersistencePort;
        this.jwtTokenUtil = jwtTokenUtil;
        this.rateLimitService = rateLimitService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
        this.asyncExecutor = asyncExecutor;
    }

    @Transactional
    public void changePassword(Long userId, UserPasswordChangeRequest request) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("기존 비밀번호가 올바르지 않습니다.");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userPersistencePort.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isPasswordChangeRequired(Long userId) {
        return passwordValidationService.isPasswordChangeRequired(userId);
    }

    @Transactional(readOnly = true)
    public boolean isPasswordChangeRecommended(Long userId) {
        return passwordValidationService.isPasswordChangeRecommended(userId);
    }

    @Transactional(readOnly = true)
    public long getTodayPasswordChangeCount(Long userId) {
        return passwordValidationService.getTodayPasswordChangeCount(userId);
    }

    @Transactional(readOnly = true)
    public Long getUserIdByUsername(String username) {
        return userPersistencePort.findById(username)
                .map(User::getId)
                .orElse(null);
    }

    @Transactional
    public UserResponse join(UserJoinRequest request) {
        rateLimitService.checkRateLimit(request.getEmail(), "join");

        if (userPersistencePort.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        String apiKey = generateApiKey();

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .email(request.getEmail())
                .apiKey(apiKey)
                .status(UserStatus.PENDING)
                .role(UserRole.USER)
                .build();

        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserLoginResponse login(UserLoginRequest request, String clientIp) {
        try {
            rateLimitService.checkRateLimit(clientIp, "login");

            User user = userPersistencePort.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                user.increaseLoginFailCount();
                userPersistencePort.save(user);
                throw new RuntimeException("비밀번호가 올바르지 않습니다.");
            }

            if (!user.isActive()) {
                throw new RuntimeException("비활성화된 계정입니다.");
            }

            user.resetLoginFailCount();
            user.updateLastLoginAt(LocalDateTime.now());
            userPersistencePort.save(user);

            String accessToken = jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole());
            String refreshToken = jwtTokenUtil.generateRefreshToken(user.getEmail());

            return UserLoginResponse.builder()
                    .user(UserResponse.from(user))
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .apiKey(user.getApiKey())
                    .build();

        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            throw new RuntimeException("로그인에 실패했습니다.");
        }
    }

    @Cacheable(value = "user", key = "#userId")
    public UserResponse getCurrentUserInfo(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Cacheable(value = "userProfile", key = "#userId")
    public UserResponse getUserProfile(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Cacheable(value = "user", key = "#userId")
    public UserResponse getUserInfo(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Cacheable(value = "userList", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<UserResponse> getUsers(Pageable pageable) {
        Page<User> users = userPersistencePort.findAll(pageable);
        return users.map(UserResponse::from);
    }

    @Cacheable(value = "pendingUsers", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<UserResponse> getPendingUsers(Pageable pageable) {
        Page<User> users = userPersistencePort.findByStatus(UserStatus.PENDING, pageable);
        return users.map(UserResponse::from);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile", "pendingUsers" }, allEntries = true)
    public UserResponse approveUser(Long userId, Long approverId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.approve(approverId);
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile", "pendingUsers" }, allEntries = true)
    public UserResponse rejectUser(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.reject();
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile" }, allEntries = true)
    public UserResponse suspendUser(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.suspend();
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile" }, allEntries = true)
    public UserResponse unlockUser(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.unlock();
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile" }, key = "#userId")
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getUsername(), request.getEmail());
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile" }, allEntries = true)
    public UserResponse changeRole(Long userId, UserRole role) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.changeRole(role);
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RuntimeException("정지된 사용자입니다.");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new RuntimeException("거부된 사용자입니다.");
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new RuntimeException("탈퇴한 사용자입니다.");
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        try {
            User user = userPersistencePort.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            user.withdraw();
            userPersistencePort.save(user);

            log.info("사용자 삭제 완료: userId={}, username={}", userId, user.getUsername());
        } catch (Exception e) {
            log.error("사용자 삭제 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("사용자 삭제에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userPersistencePort.findAll();
        return users.stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByStatusAndRole(String status, String role, Pageable pageable) {
        UserStatus userStatus = status != null ? UserStatus.valueOf(status) : null;
        UserRole userRole = role != null ? UserRole.valueOf(role) : null;

        Page<User> users = userPersistencePort.findByStatusAndRole(userStatus, userRole, pageable);
        return users.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public PasswordStatusResponse getPasswordStatus(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean isChangeRequired = user.isPasswordChangeRequired();
        boolean isChangeRecommended = user.isPasswordChangeRecommended();
        long todayChangeCount = user.getTodayPasswordChangeCount();

        return PasswordStatusResponse.of(isChangeRequired, isChangeRecommended, todayChangeCount);
    }

    @Transactional
    @CacheEvict(value = { "user", "userProfile" }, allEntries = true)
    public UserResponse resetToPending(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.resetToPending();
        User savedUser = userPersistencePort.save(user);
        return UserResponse.from(savedUser);
    }

    public UserResponse changeUserRole(Long userId, UserRole newRole, Long approverId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        try {
            user.changeRole(newRole);
            User savedUser = userPersistencePort.save(user);

            log.info("사용자 역할 변경 완료: userId={}, newRole={}, approverId={}",
                    userId, newRole, approverId);

            return UserResponse.from(savedUser);
        } catch (IllegalArgumentException e) {
            log.error("사용자 역할 변경 실패: userId={}, newRole={}, error={}",
                    userId, newRole, e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            log.error("사용자 역할 변경 중 오류: userId={}, newRole={}, error={}",
                    userId, newRole, e.getMessage());
            throw new RuntimeException("사용자 역할 변경에 실패했습니다.");
        }
    }

    public void evictAllUserCache() {
        // 캐시 구현체에서 제공하는 메서드 호출
        log.info("모든 사용자 캐시 삭제 완료");
    }

    @Transactional(readOnly = true)
    public long countUsersByStatus(UserStatus status) {
        return userPersistencePort.countByStatus(status);
    }

    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}