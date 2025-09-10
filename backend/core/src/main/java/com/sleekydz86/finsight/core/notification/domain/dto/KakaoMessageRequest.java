package com.sleekydz86.finsight.core.notification.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoMessageRequest {
    private List<String> receiverUuids;
    private Map<String, Object> templateObject;
}