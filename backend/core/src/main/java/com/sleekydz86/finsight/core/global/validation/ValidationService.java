package com.sleekydz86.finsight.core.global.validation;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣_]{2,50}$");

    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("이메일은 필수입니다");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("올바른 이메일 형식이 아닙니다");
        }
        if (email.length() > 100) {
            throw new ValidationException("이메일은 100자를 초과할 수 없습니다");
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("비밀번호는 필수입니다");
        }
        if (password.length() < 12) {
            throw new ValidationException("비밀번호는 12자 이상이어야 합니다");
        }
        if (password.length() > 128) {
            throw new ValidationException("비밀번호는 128자를 초과할 수 없습니다");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다");
        }
    }

    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("사용자명은 필수입니다");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException("사용자명은 2-50자의 영문, 숫자, 한글, 언더스코어만 사용 가능합니다");
        }
    }

    public void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new ValidationException("페이지 번호는 0 이상이어야 합니다");
        }
        if (size <= 0) {
            throw new ValidationException("페이지 크기는 1 이상이어야 합니다");
        }
        if (size > 100) {
            throw new ValidationException("페이지 크기는 100을 초과할 수 없습니다");
        }
    }

    public void validateId(Long id, String fieldName) {
        if (id == null) {
            throw new ValidationException(fieldName + "은(는) 필수입니다");
        }
        if (id <= 0) {
            throw new ValidationException(fieldName + "은(는) 0보다 커야 합니다");
        }
    }

    public void validateString(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + "은(는) 필수입니다");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다");
        }
    }

    public void validateString(String value, String fieldName, int minLength, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + "은(는) 필수입니다");
        }
        if (value.length() < minLength) {
            throw new ValidationException(fieldName + "은(는) " + minLength + "자 이상이어야 합니다");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다");
        }
    }

    public void validateListSize(List<?> list, String fieldName, int maxSize) {
        if (list != null && list.size() > maxSize) {
            throw new ValidationException(fieldName + "은(는) " + maxSize + "개를 초과할 수 없습니다");
        }
    }

    public void validateBusinessRules(Object request) {
        List<String> errors = new ArrayList<>();

        if (request instanceof com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest) {
            validateBoardCreateRequest((com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest) request, errors);
        } else if (request instanceof com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest) {
            validateCommentCreateRequest((com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest) request, errors);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("비즈니스 규칙 위반: " + String.join(", ", errors));
        }
    }

    private void validateBoardCreateRequest(com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest request, List<String> errors) {
        if (request.getTitle() != null && request.getTitle().contains("스팸")) {
            errors.add("스팸성 제목은 허용되지 않습니다");
        }
        if (request.getContent() != null && request.getContent().contains("광고")) {
            errors.add("광고성 내용은 허용되지 않습니다");
        }
        if (request.getHashtags() != null && request.getHashtags().size() > 10) {
            errors.add("해시태그는 10개를 초과할 수 없습니다");
        }
    }

    private void validateCommentCreateRequest(com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest request, List<String> errors) {
        if (request.getContent() != null && request.getContent().contains("욕설")) {
            errors.add("욕설은 허용되지 않습니다");
        }
        if (request.getContent() != null && request.getContent().length() < 2) {
            errors.add("댓글은 2자 이상이어야 합니다");
        }
    }
}