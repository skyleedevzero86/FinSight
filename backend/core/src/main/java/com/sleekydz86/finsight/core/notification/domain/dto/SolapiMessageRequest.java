package com.sleekydz86.finsight.core.notification.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SolapiMessageRequest {

    private String from;
    private String to;
    private String text;
    private String subject;
    private String imageId;
    private MessageType messageType;
    private LocalDateTime scheduledDate;
    private KakaoMessageOptions kakaoOptions;

    @Data
    @Builder
    public static class KakaoMessageOptions {
        private String pfId;
        private String templateId;
        private boolean disableSms;
        private Map<String, String> variables;
        private List<KakaoButton> buttons;

        @Data
        @Builder
        public static class KakaoButton {
            private String name;
            private String type;
            private String urlMo;
            private String urlPc;
            private String schemeIos;
            private String schemeAndroid;
        }
    }
}