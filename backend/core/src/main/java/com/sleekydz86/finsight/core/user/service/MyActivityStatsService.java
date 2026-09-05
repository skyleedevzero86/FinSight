package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.MyActivityStatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MyActivityStatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BoardPersistencePort boardPersistencePort;
    private final CommentPersistencePort commentPersistencePort;
    private final BoardReactionPersistencePort boardReactionPersistencePort;
    private final CommentReactionPersistencePort commentReactionPersistencePort;

    public MyActivityStatsService(
            BoardPersistencePort boardPersistencePort,
            CommentPersistencePort commentPersistencePort,
            BoardReactionPersistencePort boardReactionPersistencePort,
            CommentReactionPersistencePort commentReactionPersistencePort) {
        this.boardPersistencePort = boardPersistencePort;
        this.commentPersistencePort = commentPersistencePort;
        this.boardReactionPersistencePort = boardReactionPersistencePort;
        this.commentReactionPersistencePort = commentReactionPersistencePort;
    }

    public MyActivityStatsResponse getStats(
            String userEmail,
            String periodType,
            String fromDate,
            String toDate) {
        String period = periodType == null || periodType.isBlank() ? "ALL" : periodType.trim().toUpperCase();
        LocalDateTime from;
        LocalDateTime to;
        String rangeFrom;
        String rangeTo;

        if ("ALL".equals(period)) {
            from = null;
            to = null;
            rangeFrom = null;
            rangeTo = null;
        } else {
            LocalDate[] range = resolveRange(period, fromDate, toDate);
            from = range[0].atStartOfDay();
            to = range[1].plusDays(1).atStartOfDay();
            rangeFrom = range[0].format(DAY);
            rangeTo = range[1].format(DAY);
        }

        long boardCount;
        long commentCount;
        long boardReactionCount;
        long commentReactionCount;

        if (from == null) {
            boardCount = boardPersistencePort.countByAuthorEmail(userEmail);
            commentCount = commentPersistencePort.countByAuthorEmail(userEmail);
            boardReactionCount = boardReactionPersistencePort.countByUserEmail(userEmail);
            commentReactionCount = commentReactionPersistencePort.countByUserEmail(userEmail);
        } else {
            boardCount = boardPersistencePort.countByAuthorEmailBetween(userEmail, from, to);
            commentCount = commentPersistencePort.countByAuthorEmailBetween(userEmail, from, to);
            boardReactionCount = boardReactionPersistencePort.countByUserEmailBetween(userEmail, from, to);
            commentReactionCount = commentReactionPersistencePort.countByUserEmailBetween(userEmail, from, to);
        }

        log.info("내 활동 통계 - user={}, period={}, boards={}, comments={}, boardReactions={}, commentReactions={}",
                userEmail, period, boardCount, commentCount, boardReactionCount, commentReactionCount);

        return new MyActivityStatsResponse(
                boardCount,
                commentCount,
                boardReactionCount,
                commentReactionCount,
                rangeFrom,
                rangeTo,
                period);
    }

    private static LocalDate[] resolveRange(String period, String fromDate, String toDate) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "DAILY" -> {
                LocalDate day = parseDay(fromDate, today);
                yield new LocalDate[]{day, day};
            }
            case "WEEKLY" -> {
                LocalDate day = parseDay(fromDate, today);
                LocalDate start = day.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                yield new LocalDate[]{start, end};
            }
            case "MONTHLY" -> {
                LocalDate day = parseMonth(fromDate, today);
                LocalDate start = day.withDayOfMonth(1);
                LocalDate end = day.with(TemporalAdjusters.lastDayOfMonth());
                yield new LocalDate[]{start, end};
            }
            case "RANGE" -> {
                LocalDate from = parseDay(fromDate, today.minusDays(6));
                LocalDate to = parseDay(toDate, today);
                if (to.isBefore(from)) {
                    LocalDate tmp = from;
                    from = to;
                    to = tmp;
                }
                yield new LocalDate[]{from, to};
            }
            default -> new LocalDate[]{today, today};
        };
    }

    private static LocalDate parseDay(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim(), DAY);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static LocalDate parseMonth(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback.withDayOfMonth(1);
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.length() == 7) {
                return LocalDate.parse(trimmed + "-01", DAY);
            }
            return LocalDate.parse(trimmed, DAY).withDayOfMonth(1);
        } catch (Exception e) {
            return fallback.withDayOfMonth(1);
        }
    }
}
