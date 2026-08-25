package com.sleekydz86.finsight.core.notification.domain;

import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogWriteCommand;

import java.time.LocalDateTime;

public final class EmailLogFactory {

    private EmailLogFactory() {
    }

    public static EmailLog fromCommand(EmailLogWriteCommand command, String sanitizedBodyPreview) {
        EmailSendContext context = command.context() != null
                ? command.context()
                : EmailSendContexts.system(EmailMailPurpose.OTHER);
        boolean failed = command.isFailure();
        return new EmailLog(
                truncate(command.recipient(), 255),
                truncate(command.subject(), 500),
                truncate(command.templateType(), 64),
                context.purpose(),
                context.purpose().getLabel(),
                failed ? EmailStatus.FAILED : EmailStatus.SENT,
                truncate(command.fromAddress(), 255),
                context.userId(),
                context.actorType(),
                context.actorUserId(),
                truncate(context.requestIp(), 64),
                truncate(context.requestLocation(), 255),
                truncate(context.userAgent(), 512),
                sanitizedBodyPreview,
                truncate(command.errorMessage(), 4000),
                truncate(context.relatedRef(), 100),
                failed ? null : LocalDateTime.now(),
                LocalDateTime.now());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
