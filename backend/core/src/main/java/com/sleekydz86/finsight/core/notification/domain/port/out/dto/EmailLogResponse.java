package com.sleekydz86.finsight.core.notification.domain.port.out.dto;

import com.sleekydz86.finsight.core.notification.domain.EmailActorType;
import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.EmailStatus;

import java.time.LocalDateTime;

public record EmailLogResponse(
        Long id,
        String recipient,
        String subject,
        String templateType,
        EmailMailPurpose purpose,
        String purposeLabel,
        EmailStatus status,
        String statusLabel,
        String fromAddress,
        Long userId,
        EmailActorType actorType,
        String actorTypeLabel,
        Long actorUserId,
        String requestIp,
        String requestLocation,
        String userAgent,
        String bodyPreview,
        String errorMessage,
        String relatedRef,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
