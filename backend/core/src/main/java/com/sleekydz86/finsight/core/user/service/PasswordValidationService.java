package com.sleekydz86.finsight.core.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidationService {

    @Value("${security.password.min-length:12}")
    private int minLength;

    @Value("${security.password.require-special-chars:true}")
    private boolean requireSpecialChars;

    @Value("${security.password.require-numbers:true}")
    private boolean requireNumbers;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern NUMBERS_PATTERN = Pattern.compile("\\d");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");

    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < minLength) {
            errors.add("비밀번호는 최소 " + minLength + "자 이상이어야 합니다.");
        }

        if (requireSpecialChars && !SPECIAL_CHARS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호는 최소 1개의 특수문자를 포함해야 합니다.");
        }

        if (requireNumbers && !NUMBERS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호는 최소 1개의 숫자를 포함해야 합니다.");
        }

        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호는 최소 1개의 대문자를 포함해야 합니다.");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호는 최소 1개의 소문자를 포함해야 합니다.");
        }

        if (hasConsecutiveCharacters(password)) {
            errors.add("비밀번호에 연속된 3개 이상의 문자를 사용할 수 없습니다.");
        }

        if (hasRepeatedCharacters(password)) {
            errors.add("비밀번호에 동일한 문자가 3번 이상 반복될 수 없습니다.");
        }

        if (isCommonWeakPassword(password)) {
            errors.add("너무 일반적이거나 예측 가능한 비밀번호입니다.");
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    private boolean hasConsecutiveCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c1 == c2 && c2 == c3) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();

        String[] weakPatterns = {
                "password", "123456", "qwerty", "admin", "user",
                "letmein", "welcome", "monkey", "dragon", "master"
        };

        for (String pattern : weakPatterns) {
            if (lowerPassword.contains(pattern)) {
                return true;
            }
        }

        return false;
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

        public List<String> errors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}