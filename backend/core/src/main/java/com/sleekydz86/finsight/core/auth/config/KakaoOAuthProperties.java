package com.sleekydz86.finsight.core.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakao.oauth")
public class KakaoOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:3000/auth/kakao/callback";
    private String authorizeUrl = "https://kauth.kakao.com/oauth/authorize";
    private String tokenUrl = "https://kauth.kakao.com/oauth/token";
    private String profileUrl = "https://kapi.kakao.com/v2/user/me";
    private String scope = "profile_nickname,profile_image,account_email";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri == null ? "" : redirectUri.trim();
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
