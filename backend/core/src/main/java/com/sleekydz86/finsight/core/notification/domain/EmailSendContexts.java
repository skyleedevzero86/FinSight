package com.sleekydz86.finsight.core.notification.domain;

public final class EmailSendContexts {

    private EmailSendContexts() {
    }

    public static EmailSendContext system(EmailMailPurpose purpose) {
        return new EmailSendContext(
                purpose,
                EmailActorType.SYSTEM,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static EmailSendContext forUser(EmailMailPurpose purpose, Long userId) {
        return new EmailSendContext(
                purpose,
                EmailActorType.USER,
                userId,
                userId,
                null,
                null,
                null,
                null,
                null);
    }

    public static EmailSendContext anonymous(
            EmailMailPurpose purpose,
            String requestIp,
            String requestLocation,
            String userAgent,
            String relatedRef) {
        return new EmailSendContext(
                purpose,
                EmailActorType.ANONYMOUS,
                null,
                null,
                requestIp,
                requestLocation,
                userAgent,
                relatedRef,
                null);
    }

    public static EmailSendContext anonymousForUser(
            EmailMailPurpose purpose,
            Long userId,
            String requestIp,
            String requestLocation,
            String userAgent) {
        return new EmailSendContext(
                purpose,
                EmailActorType.ANONYMOUS,
                userId,
                null,
                requestIp,
                requestLocation,
                userAgent,
                null,
                null);
    }
}
