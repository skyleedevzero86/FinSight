package com.sleekydz86.finsight.core.user.domain.event;

import java.time.LocalDateTime;

public record UserRegisteredEvent(Long userId, LocalDateTime registeredAt) {
}
