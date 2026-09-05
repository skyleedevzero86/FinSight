package com.sleekydz86.finsight.core.comment.service;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentBatchModerationUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentBatchModerationService implements CommentBatchModerationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommentBatchModerationService.class);

    private final CommentPersistencePort commentPersistencePort;

    public CommentBatchModerationService(CommentPersistencePort commentPersistencePort) {
        this.commentPersistencePort = commentPersistencePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findHideCandidates(int reportThreshold) {
        return commentPersistencePort.findModerationCandidates(clampThreshold(reportThreshold));
    }

    @Override
    @Transactional
    public int hideOverReportedActiveComments(int reportThreshold, String actorEmail) {
        int threshold = clampThreshold(reportThreshold);
        List<Comment> targets = commentPersistencePort.findModerationCandidates(threshold);
        int hidden = 0;
        for (Comment comment : targets) {
            if (!comment.isActive()) {
                continue;
            }
            commentPersistencePort.save(comment.updateStatus(CommentStatus.HIDDEN));
            hidden++;
        }
        log.info("과다 신고 댓글 숨김 실행 - actor={}, threshold={}, hidden={}", actorEmail, threshold, hidden);
        return hidden;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findHiddenComments() {
        return commentPersistencePort.findByStatus(CommentStatus.HIDDEN);
    }

    @Override
    @Transactional
    public Comment restoreHiddenComment(Long commentId) {
        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));
        if (comment.getStatus() != CommentStatus.HIDDEN
                && comment.getStatus() != CommentStatus.BLOCKED
                && comment.getStatus() != CommentStatus.REPORTED) {
            throw new ValidationException(
                    "복구할 수 없는 상태입니다: " + comment.getStatus(),
                    List.of("status"));
        }
        Comment saved = commentPersistencePort.save(comment.updateStatus(CommentStatus.ACTIVE));
        log.info("숨김/차단 댓글 복구 - commentId={}", commentId);
        return saved;
    }

    @Override
    @Transactional
    public Comment blockComment(Long commentId) {
        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));
        Comment saved = commentPersistencePort.save(comment.updateStatus(CommentStatus.BLOCKED));
        log.info("댓글 영구 차단 - commentId={}", commentId);
        return saved;
    }

    private static int clampThreshold(int reportThreshold) {
        if (reportThreshold < 1) {
            return 1;
        }
        return Math.min(reportThreshold, 1000);
    }
}
