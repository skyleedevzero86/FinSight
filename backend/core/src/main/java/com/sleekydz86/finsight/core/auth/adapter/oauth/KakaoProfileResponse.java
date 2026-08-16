package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoProfileResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdAsString() {
        return id == null ? null : String.valueOf(id);
    }

    public KakaoAccount getKakaoAccount() {
        return kakaoAccount;
    }

    public void setKakaoAccount(KakaoAccount kakaoAccount) {
        this.kakaoAccount = kakaoAccount;
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
            return null;
        }
        return kakaoAccount.getProfile().getNickname();
    }

    public String getEmail() {
        return kakaoAccount == null ? null : kakaoAccount.getEmail();
    }

    public String getProfileImageUrl() {
        if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
            return null;
        }
        String image = kakaoAccount.getProfile().getProfileImageUrl();
        if (image != null && !image.isBlank()) {
            return image;
        }
        return kakaoAccount.getProfile().getThumbnailImageUrl();
    }
}
