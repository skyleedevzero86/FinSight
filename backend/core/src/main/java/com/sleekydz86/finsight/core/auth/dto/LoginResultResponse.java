package com.sleekydz86.finsight.core.auth.dto;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.user.domain.AuthProvider;

public class LoginResultResponse {
    private JwtToken token;
    private AuthProvider authProvider;
    private String email;
    private String nickname;
    private String profileImageUrl;

    public LoginResultResponse() {
    }

    public LoginResultResponse(
            JwtToken token,
            AuthProvider authProvider,
            String email,
            String nickname,
            String profileImageUrl) {
        this.token = token;
        this.authProvider = authProvider;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static LoginResultResponse of(
            JwtToken token,
            AuthProvider authProvider,
            String email,
            String nickname,
            String profileImageUrl) {
        return new LoginResultResponse(token, authProvider, email, nickname, profileImageUrl);
    }

    public JwtToken getToken() {
        return token;
    }

    public void setToken(JwtToken token) {
        this.token = token;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
