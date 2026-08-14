package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoAccount {

    private String email;
    private KakaoProfile profile;

    @JsonProperty("is_email_valid")
    private Boolean emailValid;

    @JsonProperty("is_email_verified")
    private Boolean emailVerified;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public KakaoProfile getProfile() {
        return profile;
    }

    public void setProfile(KakaoProfile profile) {
        this.profile = profile;
    }

    public Boolean getEmailValid() {
        return emailValid;
    }

    public void setEmailValid(Boolean emailValid) {
        this.emailValid = emailValid;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
