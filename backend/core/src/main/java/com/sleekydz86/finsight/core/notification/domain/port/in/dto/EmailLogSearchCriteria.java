package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import com.sleekydz86.finsight.core.notification.domain.EmailActorType;
import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.EmailStatus;

import java.time.LocalDateTime;

public record EmailLogSearchCriteria(
        String keyword,
        EmailStatus status,
        EmailMailPurpose purpose,
        EmailActorType actorType,
        String requestIp,
        LocalDateTime from,
        LocalDateTime to) {

    public EmailLogSearchCriteria {
        keyword = blankToNull(keyword);
        requestIp = blankToNull(requestIp);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
