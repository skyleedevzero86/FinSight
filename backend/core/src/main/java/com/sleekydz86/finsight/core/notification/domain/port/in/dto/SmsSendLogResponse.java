package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SMS 발송 이력 항목")
public record SmsSendLogResponse(
        Long id,
        SmsPurpose purpose,
        String purposeLabel,
        MessageType messageType,
        String toPhone,
        String fromPhone,
        String contentPreview,
        SmsSendStatus status,
        String externalMessageId,
        String errorMessage,
        Long recipientUserId,
        Long actorUserId,
        LocalDateTime createdAt
) {
}
