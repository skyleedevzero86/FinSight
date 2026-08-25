package com.sleekydz86.finsight.core.notification.domain;

public record EmailSendContext(
        EmailMailPurpose purpose,
        EmailActorType actorType,
        Long userId,
        Long actorUserId,
        String requestIp,
        String requestLocation,
        String userAgent,
        String relatedRef,
        String bodyPreview) {

    public EmailSendContext {
        purpose = purpose != null ? purpose : EmailMailPurpose.OTHER;
        actorType = actorType != null ? actorType : EmailActorType.SYSTEM;
    }

    public EmailSendContext withBodyPreview(String preview) {
        return new EmailSendContext(
                purpose,
                actorType,
                userId,
                actorUserId,
                requestIp,
                requestLocation,
                userAgent,
                relatedRef,
                preview);
    }

    public EmailSendContext withRequestLocation(String location) {
        return new EmailSendContext(
                purpose,
                actorType,
                userId,
                actorUserId,
                requestIp,
                location,
                userAgent,
                relatedRef,
                bodyPreview);
    }
}
