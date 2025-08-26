package com.sleekydz86.finsight.core.global.exception;

public class InvalidTokenException extends BaseException {
    private final String tokenType;

    public InvalidTokenException(String tokenType) {
        super("유효하지 않은 " + tokenType + " 토큰입니다",
                "AUTH_003", "Invalid Token", 401);
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }
}