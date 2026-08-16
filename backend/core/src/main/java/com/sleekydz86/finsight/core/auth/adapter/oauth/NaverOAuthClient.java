package com.sleekydz86.finsight.core.auth.adapter.oauth;

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
            log.warn("네이버 프로필 응답 코드가 정상이 아닙니다: {}", envelope.getResultcode());
            throw new IllegalStateException("네이버 프로필 조회에 실패했습니다: " + envelope.getMessage());
        }
        return envelope.getResponse();
    }
}
