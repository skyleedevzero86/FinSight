package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.sleekydz86.finsight.core.auth.config.KakaoOAuthProperties;
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
public class KakaoOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);

    private final RestTemplate restTemplate;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(RestTemplate restTemplate, KakaoOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String createAuthorizeUrl(String state) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state);

        if (properties.getScope() != null && !properties.getScope().isBlank()) {
            builder.queryParam("scope", properties.getScope());
        }

        return builder.encode().build().toUriString();
    }

    public String createState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public KakaoOAuthTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<KakaoOAuthTokenResponse> response = restTemplate.exchange(
                properties.getTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                KakaoOAuthTokenResponse.class);

        KakaoOAuthTokenResponse body = response.getBody();
        if (body == null || body.getAccessToken() == null || body.getAccessToken().isBlank()) {
            String detail = body != null && body.getErrorDescription() != null
                    ? body.getErrorDescription()
                    : "empty body";
            log.warn("카카오 토큰 발급 실패: {}", detail);
            throw new IllegalStateException("카카오 토큰 발급에 실패했습니다");
        }
        return body;
    }

    public KakaoProfileResponse fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ResponseEntity<KakaoProfileResponse> response = restTemplate.exchange(
                properties.getProfileUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoProfileResponse.class);

        KakaoProfileResponse body = response.getBody();
        if (body == null || body.getId() == null) {
            throw new IllegalStateException("카카오 프로필 조회에 실패했습니다");
        }
        return body;
    }
}
