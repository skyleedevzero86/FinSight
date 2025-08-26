package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import com.sleekydz86.finsight.core.global.exception.UserAlreadyExistsException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserCommandUseCase, UserQueryUseCase {

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserPersistencePort userPersistencePort, PasswordEncoder passwordEncoder) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @CacheEvict(value = "userCache", key = "#request.email")
    public User registerUser(UserRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("사용자 등록 정보는 필수입니다");
        }

        if (userPersistencePort.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        PasswordValidationService.PasswordValidationResult validationResult =
                passwordValidationService.validatePassword(request.getPassword());

        if (!validationResult.isValid()) {
            throw new InvalidPasswordException(validationResult.getErrors());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), encodedPassword, request.getUsername());
        return userPersistencePort.save(user);
    }

    @Override
    @Cacheable(value = "userCache", key = "#userId")
    public Optional<User> findById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다: " + userId);
        }

        Optional<User> user = userPersistencePort.findById(userId);
        if (user.isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    @Override
    @Cacheable(value = "userCache", key = "#email")
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }

        Optional<User> user = userPersistencePort.findByEmail(email);
        if (user.isEmpty()) {
            throw new UserNotFoundException(email);
        }
        return user;
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userPersistencePort.save(user);
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void updateWatchlist(Long userId, WatchlistUpdateRequest request) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.getWatchlist().clear();
        user.getWatchlist().addAll(request.getCategories());
        userPersistencePort.save(user);
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void updateNotificationPreferences(Long userId, List<NotificationType> preferences) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.getNotificationPreferences().clear();
        user.getNotificationPreferences().addAll(preferences);
        userPersistencePort.save(user);
    }

    @Override
    @Cacheable(value = "userCache", key = "'watchlist_' + #userId")
    public List<TargetCategory> getUserWatchlist(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new ArrayList<>(user.getWatchlist());
    }

    @Override
    @Cacheable(value = "userCache", key = "'notifications_' + #userId")
    public List<NotificationType> getUserNotificationPreferences(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new ArrayList<>(user.getNotificationPreferences());
    }
}