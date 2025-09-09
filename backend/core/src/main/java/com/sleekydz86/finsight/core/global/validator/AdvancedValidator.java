package com.sleekydz86.finsight.core.global.validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class AdvancedValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$");

    public List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.trim().isEmpty()) {
            errors.add("이메일은 필수입니다.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("올바른 이메일 형식이 아닙니다.");
        } else if (email.length() > 255) {
            errors.add("이메일은 255자를 초과할 수 없습니다.");
        }

        return errors;
    }

    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.trim().isEmpty()) {
            errors.add("비밀번호는 필수입니다.");
        } else if (password.length() < 8) {
            errors.add("비밀번호는 최소 8자 이상이어야 합니다.");
        } else if (password.length() > 128) {
            errors.add("비밀번호는 128자를 초과할 수 없습니다.");
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errors.add("비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함해야 합니다.");
        }

        return errors;
    }

    public List<String> validateUrl(String url) {
        List<String> errors = new ArrayList<>();

        if (url == null || url.trim().isEmpty()) {
            errors.add("URL은 필수입니다.");
        } else if (!URL_PATTERN.matcher(url).matches()) {
            errors.add("올바른 URL 형식이 아닙니다.");
        } else if (url.length() > 2048) {
            errors.add("URL은 2048자를 초과할 수 없습니다.");
        }

        return errors;
    }

    public List<String> validateTitle(String title) {
        List<String> errors = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            errors.add("제목은 필수입니다.");
        } else if (title.length() > 200) {
            errors.add("제목은 200자를 초과할 수 없습니다.");
        } else if (title.trim().length() < 2) {
            errors.add("제목은 최소 2자 이상이어야 합니다.");
        }

        return errors;
    }

    public List<String> validateContent(String content) {
        List<String> errors = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            errors.add("내용은 필수입니다.");
        } else if (content.length() > 10000) {
            errors.add("내용은 10000자를 초과할 수 없습니다.");
        } else if (content.trim().length() < 10) {
            errors.add("내용은 최소 10자 이상이어야 합니다.");
        }

        return errors;
    }
}