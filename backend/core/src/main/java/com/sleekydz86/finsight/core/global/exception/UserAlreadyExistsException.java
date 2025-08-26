package com.sleekydz86.finsight.core.global.exception;

public class UserAlreadyExistsException extends BaseException {
    private final String email;

    public UserAlreadyExistsException(String email) {
        super("이미 가입된 이메일입니다: " + email,
                "USER_003", "User Already Exists", 409);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}