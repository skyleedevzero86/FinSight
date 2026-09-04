package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "SMS 발송 설정")
public record SmsSettingsResponse(
        @Schema(description = "SMS 마스터 스위치") boolean enabled,
        @Schema(description = "뉴스 알림 자동 SMS") boolean newsAlertEnabled,
        @Schema(description = "OTP SMS") boolean otpEnabled,
        @Schema(description = "계정 복구 SMS") boolean accountRecoveryEnabled,
        @Schema(description = "시스템 알림 SMS") boolean systemAlertEnabled,
        @Schema(description = "일반 알림 SMS") boolean notificationEnabled,
        @Schema(description = "기본 메시지 타입", example = "SMS") String defaultMessageType,
        @Schema(description = "기본 발신번호") String defaultFromNumber,
        @Schema(description = "Solapi 연동 활성화 여부(설정값)") boolean solapiEnabled
) {
}
