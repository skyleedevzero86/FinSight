package com.sleekydz86.finsight.core.notification.domain;

import com.sleekydz86.finsight.core.notification.domain.port.out.dto.EmailLogResponse;

public final class EmailLogMapper {

    private EmailLogMapper() {
    }

    public static EmailLogResponse toResponse(EmailLog log) {
        return new EmailLogResponse(
                log.getId(),
                log.getRecipient(),
                log.getSubject(),
                log.getTemplateType(),
                log.getPurpose(),
                log.getPurposeLabel(),
                log.getStatus(),
                log.getStatus() != null ? log.getStatus().getDescription() : null,
                log.getFromAddress(),
                log.getUserId(),
                log.getActorType(),
                log.getActorType() != null ? log.getActorType().getLabel() : null,
                log.getActorUserId(),
                log.getRequestIp(),
                log.getRequestLocation(),
                log.getUserAgent(),
                log.getBodyPreview(),
                log.getErrorMessage(),
                log.getRelatedRef(),
                log.getSentAt(),
                log.getCreatedAt(),
                log.getUpdatedAt());
    }
}
