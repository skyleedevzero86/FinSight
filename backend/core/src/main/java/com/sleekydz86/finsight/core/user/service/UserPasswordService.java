package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserPasswordHistory;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserPasswordChangeRequest;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPasswordService {

    private final UserPersistencePort userPersistencePort;
    private final UserPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_DAILY_PASSWORD_CHANGES = 3;
    private static final int PASSWORD_HISTORY_CHECK_COUNT = 5;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>_+=\\-\\[\\]\\\\;'`~])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>_+=\\-\\[\\]\\\\;'`~]{8,}$"
    );

    @Transactional
    public void changePassword(Long userId, UserPasswordChangeRequest request) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        validatePasswordChangeRequest(request);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("기존 비밀번호가 올바르지 않습니다.");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new RuntimeException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (!user.canChangePassword()) {
            throw new RuntimeException("일일 비밀번호 변경 횟수를 초과했습니다.");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());

        passwordHistoryRepository.save(
                UserPasswordHistory.builder()
                        .user(user)
                        .passwordHash(newPasswordHash)
                        .build()
        );

        user.changePassword(newPasswordHash);
        userPersistencePort.save(user);

        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    private void validatePasswordChangeRequest(UserPasswordChangeRequest request) {
        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            throw new RuntimeException("기존 비밀번호는 필수입니다.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호는 필수입니다.");
        }
        if (request.getNewPasswordConfirm() == null || request.getNewPasswordConfirm().trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호 확인은 필수입니다.");
        }
        if (!isValidPassword(request.getNewPassword())) {
            throw new RuntimeException("비밀번호는 영문 대소문자, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }
    }

    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private void validatePasswordHistory(Long userId, String newPassword) {
        List<UserPasswordHistory> recentHistory = passwordHistoryRepository
                .findRecentPasswordHistoryWithLimit(userId, PASSWORD_HISTORY_CHECK_COUNT);

        for (UserPasswordHistory history : recentHistory) {
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                throw new RuntimeException("최근 사용한 비밀번호는 사용할 수 없습니다.");
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isPasswordChangeRequired(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return user.isPasswordChangeRequired();
    }

    @Transactional(readOnly = true)
    public boolean isPasswordChangeRecommended(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return user.isPasswordChangeRecommended();
    }

    @Transactional(readOnly = true)
    public long getTodayPasswordChangeCount(Long userId) {
        return passwordHistoryRepository.countTodayPasswordChanges(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<UserPasswordHistory> getPasswordHistory(Long userId, int limit) {
        return passwordHistoryRepository.findRecentPasswordHistoryWithLimit(userId, limit);
    }
}