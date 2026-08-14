package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.sleekydz86.finsight.core.auth.config.GoogleOAuthProperties;
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
public class GoogleOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties properties;

    public GoogleOAuthClient(RestTemplate restTemplate, GoogleOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String createAuthorizeUrl(String state) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("access_type", "online")
                .queryParam("include_granted_scopes", "true");

        if (properties.getScope() != null && !properties.getScope().isBlank()) {
            builder.queryParam("scope", properties.getScope());
        }

        return builder.encode().build().toUriString();
    }

    public String createState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public GoogleOAuthTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<GoogleOAuthTokenResponse> response = restTemplate.exchange(
                properties.getTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                GoogleOAuthTokenResponse.class);

        GoogleOAuthTokenResponse body = response.getBody();
        if (body == null || body.getAccessToken() == null || body.getAccessToken().isBlank()) {
            String detail = body != null && body.getErrorDescription() != null
                    ? body.getErrorDescription()
                    : "empty body";
            log.warn("구글 토큰 발급 실패: {}", detail);
            throw new IllegalStateException("구글 토큰 발급에 실패했습니다");
        }
        return body;
    }

    public GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                properties.getUserInfoUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserInfoResponse.class);

        GoogleUserInfoResponse body = response.getBody();
        if (body == null || body.getSub() == null || body.getSub().isBlank()) {
            throw new IllegalStateException("구글 사용자 정보 조회에 실패했습니다");
        }
        return body;
    }
}
