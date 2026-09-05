package com.sleekydz86.finsight.core.history.service;

import com.sleekydz86.finsight.core.board.adapter.persistence.command.BoardJpaEntity;
import com.sleekydz86.finsight.core.board.adapter.persistence.command.BoardJpaRepository;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityItem;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityRequest;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityResponse;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityScore;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HistoryPopularityService {

    private static final Logger log = LoggerFactory.getLogger(HistoryPopularityService.class);
    private static final int MAX_ITEMS = 200;

    private final BoardJpaRepository boardJpaRepository;
    private final LiveVodFavoriteJpaRepository liveVodFavoriteJpaRepository;
    private final LiveVodCommentJpaRepository liveVodCommentJpaRepository;

    public HistoryPopularityService(
            BoardJpaRepository boardJpaRepository,
            LiveVodFavoriteJpaRepository liveVodFavoriteJpaRepository,
            LiveVodCommentJpaRepository liveVodCommentJpaRepository) {
        this.boardJpaRepository = boardJpaRepository;
        this.liveVodFavoriteJpaRepository = liveVodFavoriteJpaRepository;
        this.liveVodCommentJpaRepository = liveVodCommentJpaRepository;
    }

    @Transactional(readOnly = true)
    public PopularityResponse score(PopularityRequest request) {
        List<PopularityItem> items = request == null || request.items() == null
                ? List.of()
                : request.items().stream().limit(MAX_ITEMS).toList();
        if (items.isEmpty()) {
            return new PopularityResponse(List.of());
        }

        Set<String> videoIds = new HashSet<>();
        Set<Long> boardIds = new HashSet<>();
        for (PopularityItem item : items) {
            if (item == null || item.kind() == null || item.id() == null || item.id().isBlank()) {
                continue;
            }
            String kind = item.kind().trim().toUpperCase(Locale.ROOT);
            if ("LIVE_VOD".equals(kind)) {
                videoIds.add(item.id().trim());
            } else if ("BOARD".equals(kind)) {
                try {
                    boardIds.add(Long.parseLong(item.id().trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Map<String, Long> liveScores = loadLiveVodScores(videoIds);
        Map<Long, Long> boardScores = loadBoardScores(boardIds);

        List<PopularityScore> scores = new ArrayList<>(items.size());
        for (PopularityItem item : items) {
            if (item == null || item.key() == null || item.key().isBlank()) {
                continue;
            }
            String kind = item.kind() == null ? "" : item.kind().trim().toUpperCase(Locale.ROOT);
            long score = 0L;
            if ("LIVE_VOD".equals(kind) && item.id() != null) {
                score = liveScores.getOrDefault(item.id().trim(), 0L);
            } else if ("BOARD".equals(kind) && item.id() != null) {
                try {
                    score = boardScores.getOrDefault(Long.parseLong(item.id().trim()), 0L);
                } catch (NumberFormatException ignored) {
                    score = 0L;
                }
            }
            scores.add(new PopularityScore(item.key(), score));
        }
        log.info("히스토리 인기 점수 조회 - 요청={}건, LIVE/VOD={}건, 게시글={}건",
                items.size(), videoIds.size(), boardIds.size());
        return new PopularityResponse(scores);
    }

    private Map<String, Long> loadLiveVodScores(Set<String> videoIds) {
        Map<String, Long> scores = new HashMap<>();
        if (videoIds.isEmpty()) {
            return scores;
        }
        for (String id : videoIds) {
            scores.put(id, 0L);
        }
        for (Object[] row : liveVodFavoriteJpaRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            scores.put(id, scores.getOrDefault(id, 0L) + count);
        }
        for (Object[] row : liveVodCommentJpaRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            scores.put(id, scores.getOrDefault(id, 0L) + count);
        }
        return scores;
    }

    private Map<Long, Long> loadBoardScores(Set<Long> boardIds) {
        Map<Long, Long> scores = new HashMap<>();
        if (boardIds.isEmpty()) {
            return scores;
        }
        for (BoardJpaEntity board : boardJpaRepository.findAllById(boardIds)) {
            long score = (long) board.getViewCount() + board.getLikeCount();
            scores.put(board.getId(), score);
        }
        return scores;
    }
}
