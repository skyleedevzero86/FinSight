package com.sleekydz86.finsight.core.global.exception;

import java.util.List;

public class InvalidPasswordException extends BaseException {
    private final List<String> validationErrors;

    public InvalidPasswordException(List<String> validationErrors) {
        super("비밀번호가 보안 요구사항을 충족하지 않습니다",
                "USER_004", "Invalid Password", 400);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}