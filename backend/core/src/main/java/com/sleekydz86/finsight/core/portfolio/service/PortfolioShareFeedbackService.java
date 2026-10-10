package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReactionJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReactionJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetail;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PortfolioShareFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioShareFeedbackService.class);

    private final PortfolioShareJpaRepository shareRepository;
    private final PortfolioShareReactionJpaRepository reactionRepository;
    private final PortfolioShareReportJpaRepository reportRepository;

    public PortfolioShareFeedbackService(
            PortfolioShareJpaRepository shareRepository,
            PortfolioShareReactionJpaRepository reactionRepository,
            PortfolioShareReportJpaRepository reportRepository) {
        this.shareRepository = shareRepository;
        this.reactionRepository = reactionRepository;
        this.reportRepository = reportRepository;
    }

    public boolean countsForGoals(PortfolioShareJpaEntity share) {
        String status = share.getModerationStatus();
        boolean open = status == null || status.isBlank() || "OPEN".equals(status) || "WARN".equals(status);
        return open && reports(share.getId()) == 0;
    }

    public PortfolioShareCard toCard(PortfolioShareJpaEntity share) {
        long shareId = share.getId() == null ? 0L : share.getId();
        return PortfolioShareCards.from(share, likes(shareId), dislikes(shareId), reports(shareId));
    }

    @Transactional(readOnly = true)
    public PortfolioShareDetail detail(Long shareId, Long userId) {
        return snapshot(requirePublic(shareId), userId);
    }

    @Transactional
    public PortfolioShareDetail react(Long userId, Long shareId, String type) {
        requireUser(userId);
        PortfolioShareJpaEntity share = requirePublic(shareId);
        rejectClosed(share);
        String requested = reactionType(type);
        applyReaction(shareId, userId, requested);
        log.info("포트폴리오 공유 반응 shareId={} userId={} type={}", shareId, userId, requested);
        return snapshot(share, userId);
    }

    @Transactional
    public PortfolioShareDetail report(Long userId, Long shareId, String reason, String note) {
        requireUser(userId);
        String selected = PortfolioShareReasons.require(reason);
        String written = writtenNote(note);
        PortfolioShareJpaEntity share = requirePublic(shareId);
        if ("REMOVED".equals(stored(share))) {
            throw new ValidationException("삭제된 게시물은 신고할 수 없습니다.", List.of());
        }
        if (reportRepository.findByShareIdAndUserId(shareId, userId).isEmpty()) {
            saveReport(shareId, userId, selected, written);
            log.info("포트폴리오 공유 신고 shareId={} userId={} reason={}", shareId, userId, selected);
        }
        return snapshot(share, userId);
    }

    private void applyReaction(Long shareId, Long userId, String requested) {
        Optional<PortfolioShareReactionJpaEntity> existing = reactionRepository.findByShareIdAndUserId(shareId, userId);
        if (existing.isEmpty()) {
            saveReaction(shareId, userId, requested);
            return;
        }
        PortfolioShareReactionJpaEntity row = existing.get();
        if (requested.equals(row.getReactionType())) {
            reactionRepository.delete(row);
            return;
        }
        row.change(requested);
    }

    private void saveReaction(Long shareId, Long userId, String requested) {
        try {
            reactionRepository.save(new PortfolioShareReactionJpaEntity(shareId, userId, requested, LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            log.warn("포트폴리오 공유 반응 중복 shareId={} userId={}", shareId, userId);
        }
    }

    private void saveReport(Long shareId, Long userId, String reason, String note) {
        try {
            reportRepository.save(new PortfolioShareReportJpaEntity(shareId, userId, reason, note, LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            log.warn("포트폴리오 공유 신고 중복 shareId={} userId={}", shareId, userId);
        }
    }

    private String writtenNote(String note) {
        String text = note == null ? "" : note.trim();
        if (text.isBlank()) {
            throw new ValidationException("신고 사유를 입력해 주세요.", List.of());
        }
        if (text.length() > 500) {
            throw new ValidationException("신고 사유는 500자 이하로 입력해 주세요.", List.of());
        }
        return text;
    }

    private PortfolioShareDetail snapshot(PortfolioShareJpaEntity share, Long userId) {
        long shareId = share.getId() == null ? 0L : share.getId();
        String mine = userId == null ? null : reactionRepository.findByShareIdAndUserId(shareId, userId)
                .map(PortfolioShareReactionJpaEntity::getReactionType)
                .orElse(null);
        boolean reported = userId != null && reportRepository.findByShareIdAndUserId(shareId, userId).isPresent();
        return new PortfolioShareDetail(toCard(share), mine, reported, PortfolioShareViews.from(share));
    }

    private PortfolioShareJpaEntity requirePublic(Long shareId) {
        PortfolioShareJpaEntity share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ValidationException("공유 게시물을 찾을 수 없습니다. 번호: " + shareId, List.of()));
        if (!"PUBLIC".equals(share.getVisibility())) {
            throw new ValidationException("공유 게시물을 찾을 수 없습니다. 번호: " + shareId, List.of());
        }
        return share;
    }

    private void rejectClosed(PortfolioShareJpaEntity share) {
        String shown = PortfolioShareSignals.display(stored(share), dislikes(share.getId()), reports(share.getId()));
        if ("REMOVED".equals(shown)) {
            throw new ValidationException("삭제된 게시물은 반응할 수 없습니다.", List.of());
        }
        if ("BLIND".equals(shown)) {
            throw new ValidationException("블라인드 처리된 게시물은 반응할 수 없습니다.", List.of());
        }
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new ValidationException("로그인이 필요합니다.", List.of());
        }
    }

    private String reactionType(String type) {
        if (type == null || type.isBlank()) {
            throw new ValidationException("반응을 입력해 주세요.", List.of());
        }
        String value = type.trim().toUpperCase();
        if ("LIKE".equals(value) || "DISLIKE".equals(value)) {
            return value;
        }
        throw new ValidationException("반응은 LIKE 또는 DISLIKE 입니다. 입력값: " + type, List.of());
    }

    private String stored(PortfolioShareJpaEntity share) {
        String status = share.getModerationStatus();
        return status == null || status.isBlank() ? "OPEN" : status;
    }

    private long likes(Long shareId) {
        return shareId == null ? 0L : reactionRepository.countByShareIdAndReactionType(shareId, "LIKE");
    }

    private long dislikes(Long shareId) {
        return shareId == null ? 0L : reactionRepository.countByShareIdAndReactionType(shareId, "DISLIKE");
    }

    private long reports(Long shareId) {
        return shareId == null ? 0L : reportRepository.countByShareId(shareId);
    }
}
