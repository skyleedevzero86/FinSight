package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SMS 수동 발송 결과")
public record SmsManualSendResponse(
        boolean success,
        SmsSendStatus status,
        MessageType messageType,
        String messageId,
        String toPhone,
        String errorMessage
) {
}
