package com.sleekydz86.finsight.core.global.exception;

public class UserNotFoundException extends BaseException {
    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. ID: " + userId,
                "USER_001", "User Not Found", 404);
        this.userId = userId;
    }

    public UserNotFoundException(String email) {
        super("해당 이메일로 가입된 사용자를 찾을 수 없습니다: " + email,
                "USER_002", "User Not Found", 404);
        this.userId = null;
    }

    public Long getUserId() {
        return userId;
    }
}