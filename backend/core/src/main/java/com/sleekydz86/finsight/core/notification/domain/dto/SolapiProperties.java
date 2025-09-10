package com.sleekydz86.finsight.core.notification.domain.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "solapi")
public class SolapiProperties {

    private String apiKey;
    private String apiSecret;
    private String baseUrl = "https://api.solapi.com";
    private String defaultFromNumber;
    private boolean enabled = false;

    private KakaoConfig kakao = new KakaoConfig();

    @Data
    public static class KakaoConfig {
        private String pfId;
        private String templateId;
        private boolean disableSms = false;
    }
}