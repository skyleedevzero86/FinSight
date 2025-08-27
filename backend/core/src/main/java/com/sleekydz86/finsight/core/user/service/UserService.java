package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.in.UserCommandUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.UserQueryUseCase;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserCommandUseCase, UserQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public UserService(UserPersistencePort userPersistencePort,
                       PasswordEncoder passwordEncoder,
                       PasswordValidationService passwordValidationService) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    @Override
    @CacheEvict(value = "userCache", key = "#request.email")
    public User registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userPersistencePort.existsByEmail(request.getEmail())) {
            log.warn("User registration failed: email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        PasswordValidationService.PasswordValidationResult validationResult =
                passwordValidationService.validatePassword(request.getPassword());

        if (!validationResult.isValid()) {
            log.warn("User registration failed: invalid password for email - {}", request.getEmail());
            throw new InvalidPasswordException(validationResult.getErrors());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = new User(request.getEmail(), encodedPassword, request.getUsername());
        newUser.setRole(UserRole.USER);
        newUser.setActive(true);

        User savedUser = userPersistencePort.save(newUser);
        log.info("User registered successfully: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    @Cacheable(value = "userCache", key = "#userId")
    public Optional<User> findById(Long userId) {
        log.debug("Finding user by ID: {}", userId);
        return userPersistencePort.findById(userId);
    }

    @Override
    @Cacheable(value = "userCache", key = "#email")
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userPersistencePort.findByEmail(email);
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public User updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            PasswordValidationService.PasswordValidationResult validationResult =
                    passwordValidationService.validatePassword(request.getPassword());

            if (!validationResult.isValid()) {
                log.warn("User update failed: invalid password for user - {}", userId);
                throw new InvalidPasswordException(validationResult.getErrors());
            }

            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userPersistencePort.save(user);
        log.info("User updated successfully: {}", userId);

        return updatedUser;
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void updateWatchlist(Long userId, WatchlistUpdateRequest request) {
        log.info("Updating watchlist for user: {}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (request.getCategories() != null) {
            user.setWatchlist(request.getCategories());
            user.setUpdatedAt(LocalDateTime.now());
            userPersistencePort.save(user);
            log.info("Watchlist updated for user: {}", userId);
        }
    }

    @Override
    @CacheEvict(value = "userCache", key = "#userId")
    public void updateNotificationPreferences(Long userId, List<NotificationType> preferences) {
        log.info("Updating notification preferences for user: {}", userId);

        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (preferences != null) {
            user.setNotificationPreferences(preferences);
            user.setUpdatedAt(LocalDateTime.now());
            userPersistencePort.save(user);
            log.info("Notification preferences updated for user: {}", userId);
        }
    }

    @Override
    @Cacheable(value = "userCache", key = "'watchlist_' + #userId")
    public List<TargetCategory> getUserWatchlist(Long userId) {
        log.debug("Getting watchlist for user: {}", userId);
        return userPersistencePort.findById(userId)
                .map(User::getWatchlist)
                .orElse(List.of());
    }

    @Override
    @Cacheable(value = "userCache", key = "'notifications_' + #userId")
    public List<NotificationType> getUserNotificationPreferences(Long userId) {
        log.debug("Getting notification preferences for user: {}", userId);
        return userPersistencePort.findById(userId)
                .map(User::getNotificationPreferences)
                .orElse(List.of());
    }
}