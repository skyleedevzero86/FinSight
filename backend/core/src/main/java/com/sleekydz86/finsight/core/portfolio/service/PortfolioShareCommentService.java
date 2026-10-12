package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareCommentJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareCommentJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareCommentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PortfolioShareCommentService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioShareCommentService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final PortfolioShareJpaRepository shareRepository;
    private final PortfolioShareCommentJpaRepository commentRepository;

    public PortfolioShareCommentService(
            PortfolioShareJpaRepository shareRepository,
            PortfolioShareCommentJpaRepository commentRepository) {
        this.shareRepository = shareRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioShareCommentItem> list(Long shareId) {
        requirePublic(shareId);
        return commentRepository.findTop30ByShareIdOrderByIdDesc(shareId).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public List<PortfolioShareCommentItem> add(Long userId, String nickname, Long shareId, String content) {
        if (userId == null) {
            throw new ValidationException("로그인이 필요합니다.", List.of());
        }
        PortfolioShareJpaEntity share = requirePublic(shareId);
        if ("REMOVED".equals(share.getModerationStatus())) {
            throw new ValidationException("삭제된 게시물에는 댓글을 남길 수 없습니다.", List.of());
        }
        String text = content == null ? "" : content.trim();
        if (text.isBlank()) {
            throw new ValidationException("댓글을 입력해 주세요.", List.of());
        }
        if (text.length() > 500) {
            throw new ValidationException("댓글은 500자 이하로 입력해 주세요.", List.of());
        }
        String name = nickname == null || nickname.isBlank() ? "회원" : nickname.trim();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        commentRepository.save(new PortfolioShareCommentJpaEntity(shareId, userId, name, text, LocalDateTime.now()));
        log.info("포트폴리오 공유 댓글 등록 shareId={} userId={}", shareId, userId);
        return list(shareId);
    }

    private PortfolioShareJpaEntity requirePublic(Long shareId) {
        PortfolioShareJpaEntity share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ValidationException("공유 게시물을 찾을 수 없습니다. 번호: " + shareId, List.of()));
        if (!"PUBLIC".equals(share.getVisibility())) {
            throw new ValidationException("공유 게시물을 찾을 수 없습니다. 번호: " + shareId, List.of());
        }
        return share;
    }

    private PortfolioShareCommentItem toItem(PortfolioShareCommentJpaEntity comment) {
        String created = comment.getCreatedAt() == null ? "" : STAMP.format(comment.getCreatedAt());
        long id = comment.getId() == null ? 0L : comment.getId();
        return new PortfolioShareCommentItem(id, PortfolioShareText.maskName(comment.getAuthorName()), comment.getContent(), created);
    }
}
