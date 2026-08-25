package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordValidationService {

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.require-special-chars:true}")
    private boolean requireSpecialChars;

    @Value("${security.password.require-numbers:true}")
    private boolean requireNumbers;

    @Value("${security.password.require-uppercase:false}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:false}")
    private boolean requireLowercase;

    private final UserPersistencePort userPersistencePort;

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern NUMBERS_PATTERN = Pattern.compile("\\d");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");

    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("비밀번호를 입력해 주세요.");
            return new PasswordValidationResult(false, errors);
        }

        if (password.length() < minLength) {
            errors.add("비밀번호는 최소 " + minLength + "자 이상이어야 합니다.");
        }

        if (!LETTER_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 영문을 포함해 주세요.");
        }

        if (requireSpecialChars && !SPECIAL_CHARS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 특수문자를 포함해 주세요.");
        }

        if (requireNumbers && !NUMBERS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 숫자를 포함해 주세요.");
        }

        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 대문자를 포함해 주세요.");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 소문자를 포함해 주세요.");
        }

        if (hasConsecutiveCharacters(password)) {
            errors.add("연속된 문자는 사용할 수 없습니다.");
        }

        if (hasRepeatedCharacters(password)) {
            errors.add("같은 문자를 3번 이상 반복할 수 없습니다.");
        }

        if (isCommonWeakPassword(password)) {
            errors.add("너무 단순하거나 흔한 비밀번호입니다.");
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    public boolean isPasswordChangeRequired(Long userId) {
        return userPersistencePort.findById(userId)
                .map(user -> user.isPasswordChangeRequired())
                .orElse(false);
    }

    public boolean isPasswordChangeRecommended(Long userId) {
        return userPersistencePort.findById(userId)
                .map(user -> user.isPasswordChangeRecommended())
                .orElse(false);
    }

    public long getTodayPasswordChangeCount(Long userId) {
        return userPersistencePort.findById(userId)
                .map(user -> (long) user.getTodayPasswordChangeCount())
                .orElse(0L);
    }

    private boolean hasConsecutiveCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char current = password.charAt(i);
            char next = password.charAt(i + 1);
            char nextNext = password.charAt(i + 2);

            if (Character.isLetter(current) && Character.isLetter(next) && Character.isLetter(nextNext)) {
                if (next == current + 1 && nextNext == next + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char current = password.charAt(i);
            char next = password.charAt(i + 1);
            char nextNext = password.charAt(i + 2);

            if (current == next && next == nextNext) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        List<String> weakPasswords = List.of(
                "password", "123456", "qwerty", "admin", "letmein",
                "welcome", "monkey", "dragon", "master", "hello"
        );

        return weakPasswords.contains(lowerPassword);
    }

    public static class PasswordValidationResult {
        private final boolean isValid;
        private final List<String> errors;

        public PasswordValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "비밀번호가 유효합니다.";
            }
            return String.join("; ", errors);
        }
    }
}