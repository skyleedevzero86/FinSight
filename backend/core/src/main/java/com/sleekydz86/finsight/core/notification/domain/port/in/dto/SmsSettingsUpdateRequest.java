package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 발송 설정 변경")
public record SmsSettingsUpdateRequest(
        @NotNull Boolean enabled,
        @NotNull Boolean newsAlertEnabled,
        @NotNull Boolean otpEnabled,
        @NotNull Boolean accountRecoveryEnabled,
        @NotNull Boolean systemAlertEnabled,
        @NotNull Boolean notificationEnabled,
        @NotBlank @Size(max = 16) String defaultMessageType,
        @Size(max = 32) String defaultFromNumber
) {
}
