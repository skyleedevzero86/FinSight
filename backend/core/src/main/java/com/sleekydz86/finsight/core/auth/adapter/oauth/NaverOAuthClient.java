package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sleekydz86.finsight.core.auth.config.NaverOAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
public class NaverOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthClient.class);

    private final RestTemplate restTemplate;
    private final NaverOAuthProperties properties;

    public NaverOAuthClient(RestTemplate restTemplate, NaverOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String createAuthorizeUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(properties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    public String createState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public NaverTokenResponse exchangeCode(String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("code", code);
        if (state != null && !state.isBlank()) {
            form.add("state", state);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
                properties.getTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                NaverTokenResponse.class);

        NaverTokenResponse body = response.getBody();
        if (body == null || body.getAccessToken() == null || body.getAccessToken().isBlank()) {
            throw new IllegalStateException("네이버 토큰 발급에 실패했습니다");
        }
        return body;
    }

    public NaverProfileResponse fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ResponseEntity<NaverProfileEnvelope> response = restTemplate.exchange(
                properties.getProfileUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                NaverProfileEnvelope.class);

        NaverProfileEnvelope envelope = response.getBody();
        if (envelope == null || envelope.getResponse() == null || envelope.getResponse().getId() == null) {
            throw new IllegalStateException("네이버 프로필 조회에 실패했습니다");
        }
        if (!"00".equals(envelope.getResultcode())) {
            log.warn("네이버 프로필 응답 코드 이상: {}", envelope.getResultcode());
            throw new IllegalStateException("네이버 프로필 조회에 실패했습니다: " + envelope.getMessage());
        }
        return envelope.getResponse();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private String expiresIn;
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(String expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public void setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverProfileEnvelope {
        private String resultcode;
        private String message;
        private NaverProfileResponse response;

        public String getResultcode() {
            return resultcode;
        }

        public void setResultcode(String resultcode) {
            this.resultcode = resultcode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public NaverProfileResponse getResponse() {
            return response;
        }

        public void setResponse(NaverProfileResponse response) {
            this.response = response;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverProfileResponse {
        private String id;
        private String nickname;
        private String name;
        private String email;
        private String gender;
        private String age;
        private String birthday;
        @JsonProperty("birthyear")
        private String birthyear;
        private String mobile;
        @JsonProperty("profile_image")
        private String profileImage;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getBirthday() {
            return birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public String getBirthyear() {
            return birthyear;
        }

        public void setBirthyear(String birthyear) {
            this.birthyear = birthyear;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public void setProfileImage(String profileImage) {
            this.profileImage = profileImage;
        }
    }
}
