package com.sleekydz86.finsight.core.notification.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoMessageResponse {
    private boolean success;
    private List<String> successfulReceiver;
    private String errorMessage;
    private LocalDateTime timestamp;

    public static KakaoMessageResponse success(List<String> successfulReceiver) {
        return KakaoMessageResponse.builder()
                .success(true)
                .successfulReceiver(successfulReceiver)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static KakaoMessageResponse failed(String errorMessage) {
        return KakaoMessageResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}