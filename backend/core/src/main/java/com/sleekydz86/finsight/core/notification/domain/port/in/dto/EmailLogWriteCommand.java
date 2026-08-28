package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import com.sleekydz86.finsight.core.notification.domain.EmailSendContext;

public record EmailLogWriteCommand(
        String recipient,
        String subject,
        String fromAddress,
        String templateType,
        String rawBodyPreview,
        EmailSendContext context,
        String errorMessage) {

    public boolean isFailure() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
