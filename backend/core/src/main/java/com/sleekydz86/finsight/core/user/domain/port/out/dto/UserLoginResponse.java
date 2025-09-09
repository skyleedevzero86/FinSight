package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.sleekydz86.finsight.core.user.domain.User;
import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class UserLoginResponse {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private String apiKey;
}