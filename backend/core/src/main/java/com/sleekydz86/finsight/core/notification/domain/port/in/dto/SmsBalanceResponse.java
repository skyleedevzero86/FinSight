package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solapi 잔액 조회 결과")
public record SmsBalanceResponse(
        @Schema(description = "잔액 표시 문자열") String balanceText,
        @Schema(description = "시뮬레이션 여부") boolean simulation
) {
}
