package com.sleekydz86.finsight.core.comment.service;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentCommandUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentReportRequest;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReportPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.CommentReport;
import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class CommentCommandService implements CommentCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommentCommandService.class);

    private final CommentPersistencePort commentPersistencePort;
    private final CommentReactionPersistencePort commentReactionPersistencePort;
    private final CommentReportPersistencePort commentReportPersistencePort;

    public CommentCommandService(CommentPersistencePort commentPersistencePort,
                                 CommentReactionPersistencePort commentReactionPersistencePort,
                                 CommentReportPersistencePort commentReportPersistencePort) {
        this.commentPersistencePort = commentPersistencePort;
        this.commentReactionPersistencePort = commentReactionPersistencePort;
        this.commentReportPersistencePort = commentReportPersistencePort;
    }

    @Override
    public Comment createComment(String userEmail, CommentCreateRequest request) {
        log.info("Creating comment for user: {}, targetId: {}", userEmail, request.getTargetId());

        Comment comment = Comment.builder()
                .content(request.getContent())
                .authorEmail(userEmail)
                .commentType(request.getCommentType())
                .targetId(request.getTargetId())
                .parentId(request.getParentId())
                .status(CommentStatus.ACTIVE)
                .likeCount(0)
                .dislikeCount(0)
                .reportCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentPersistencePort.save(comment);
        log.info("Comment created successfully with ID: {}", savedComment.getId());

        return savedComment;
    }

    @Override
    public Comment updateComment(String userEmail, Long commentId, CommentUpdateRequest request) {
        log.info("Updating comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        if (!comment.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("댓글 수정 권한이 없습니다");
        }

        Comment updatedComment = comment.updateContent(request.getContent());
        Comment savedComment = commentPersistencePort.save(updatedComment);

        log.info("Comment updated successfully: {}", commentId);
        return savedComment;
    }

    @Override
    public void deleteComment(String userEmail, Long commentId) {
        log.info("Deleting comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        if (!comment.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("댓글 삭제 권한이 없습니다");
        }

        Comment deletedComment = comment.updateStatus(CommentStatus.DELETED);
        commentPersistencePort.save(deletedComment);

        log.info("Comment deleted successfully: {}", commentId);
    }

    @Override
    public Comment likeComment(String userEmail, Long commentId) {
        log.info("Liking comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        Optional<CommentReaction> existingReaction = commentReactionPersistencePort
                .findByCommentIdAndUserEmail(commentId, userEmail);

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            if (reaction.isLike()) {
                commentReactionPersistencePort.deleteByCommentIdAndUserEmail(commentId, userEmail);
                Comment updatedComment = comment.incrementLike().incrementLike();
                return commentPersistencePort.save(updatedComment);
            } else {
                commentReactionPersistencePort.deleteByCommentIdAndUserEmail(commentId, userEmail);
                CommentReaction newReaction = CommentReaction.builder()
                        .commentId(commentId)
                        .userEmail(userEmail)
                        .reactionType(ReactionType.LIKE)
                        .createdAt(LocalDateTime.now())
                        .build();
                commentReactionPersistencePort.save(newReaction);

                Comment updatedComment = comment.incrementLike().incrementDislike();
                return commentPersistencePort.save(updatedComment);
            }
        } else {
            CommentReaction reaction = CommentReaction.builder()
                    .commentId(commentId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.LIKE)
                    .createdAt(LocalDateTime.now())
                    .build();
            commentReactionPersistencePort.save(reaction);

            Comment updatedComment = comment.incrementLike();
            return commentPersistencePort.save(updatedComment);
        }
    }

    @Override
    public Comment dislikeComment(String userEmail, Long commentId) {
        log.info("Disliking comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        Optional<CommentReaction> existingReaction = commentReactionPersistencePort
                .findByCommentIdAndUserEmail(commentId, userEmail);

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            if (reaction.isDislike()) {
                commentReactionPersistencePort.deleteByCommentIdAndUserEmail(commentId, userEmail);
                Comment updatedComment = comment.incrementDislike().incrementDislike();
                return commentPersistencePort.save(updatedComment);
            } else {
                commentReactionPersistencePort.deleteByCommentIdAndUserEmail(commentId, userEmail);
                CommentReaction newReaction = CommentReaction.builder()
                        .commentId(commentId)
                        .userEmail(userEmail)
                        .reactionType(ReactionType.DISLIKE)
                        .createdAt(LocalDateTime.now())
                        .build();
                commentReactionPersistencePort.save(newReaction);

                Comment updatedComment = comment.incrementDislike().incrementLike(); // +1 싫어요, -1 좋아요
                return commentPersistencePort.save(updatedComment);
            }
        } else {
            CommentReaction reaction = CommentReaction.builder()
                    .commentId(commentId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.DISLIKE)
                    .createdAt(LocalDateTime.now())
                    .build();
            commentReactionPersistencePort.save(reaction);

            Comment updatedComment = comment.incrementDislike();
            return commentPersistencePort.save(updatedComment);
        }
    }

    @Override
    public void reportComment(String userEmail, Long commentId, CommentReportRequest request) {
        log.info("Reporting comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        Optional<CommentReport> existingReport = commentReportPersistencePort
                .findByCommentIdAndReporterEmail(commentId, userEmail);

        if (existingReport.isPresent()) {
            throw new IllegalArgumentException("이미 신고한 댓글입니다");
        }

        CommentReport report = CommentReport.builder()
                .commentId(commentId)
                .reporterEmail(userEmail)
                .reason(request.getReason())
                .description(request.getDescription())
                .reportedAt(LocalDateTime.now())
                .isProcessed(false)
                .build();

        commentReportPersistencePort.save(report);

        Comment updatedComment = comment.incrementReport();
        commentPersistencePort.save(updatedComment);

        log.info("Comment reported successfully: {}", commentId);
    }

    @Override
    public void blockComment(Long commentId) {
        log.info("Blocking comment: {}", commentId);

        Comment comment = commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));

        Comment blockedComment = comment.updateStatus(CommentStatus.BLOCKED);
        commentPersistencePort.save(blockedComment);

        log.info("Comment blocked successfully: {}", commentId);
    }
}