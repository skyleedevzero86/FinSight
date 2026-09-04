package com.sleekydz86.finsight.core.board.domain;

import java.time.LocalDateTime;
import java.util.List;

public record BoardModerationRun(
        Long id,
        String triggeredBy,
        String actorEmail,
        int reportThreshold,
        int hiddenCount,
        List<BoardModerationTarget> targets,
        LocalDateTime createdAt) {
}
