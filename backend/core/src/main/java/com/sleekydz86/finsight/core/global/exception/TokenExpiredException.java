package com.sleekydz86.finsight.core.global.exception;

public class TokenExpiredException extends BaseException {
    private final String tokenType;

    public TokenExpiredException(String tokenType) {
        super(tokenType + " 토큰이 만료되었습니다. 다시 로그인해주세요",
                "AUTH_002", "Token Expired", 401);
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }
}