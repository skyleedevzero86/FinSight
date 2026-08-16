package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateResponse {

    private UserResponse profile;
    private JwtToken token;

    public static ProfileUpdateResponse of(UserResponse profile, JwtToken token) {
        return ProfileUpdateResponse.builder()
                .profile(profile)
                .token(token)
                .build();
    }
}
